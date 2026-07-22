# Alur Aplikasi MozzID (dengan Firebase)

Dokumen ini menjelaskan cara kerja MozzID dari startup sampai data tersimpan
dan tersinkron, termasuk lapisan Firebase yang dipasang di belakang seam
`SyncService`.

**Prinsip yang tidak boleh dilanggar:** aplikasi harus tetap berfungsi penuh
tanpa internet dan tanpa Firebase. Firebase bersifat **aditif**, bukan syarat.

---

## 1. Arsitektur — Tiga Lapis

```
presentation/   Compose UI + ViewModel + theming
      ↓ bergantung ke
domain/         model, interface, logika murni (Kotlin polos, nol Android)
      ↑ diimplementasikan oleh
data/           Room, AudioRecord, GPS, katalog spesies, Firebase
```

Arah dependensi selalu menunjuk ke dalam. `domain/` tidak tahu apa pun soal
Android, Room, maupun Firebase — hanya interface dan data class. Karena itu
implementasi apa pun bisa ditukar tanpa menyentuh UI atau logika bisnis.

---

## 2. Dua Seam (Titik Colok)

Seluruh aplikasi hanya mengenal dua interface ini, tidak pernah kelas konkretnya.

| Seam | Interface | Implementasi default | Implementasi produksi |
|---|---|---|---|
| ML | `SpeciesClassifier` | `MockSpeciesClassifier` | `TfliteSpeciesClassifier` |
| Backend | `SyncService` | `NoopSyncService` | `FirebaseSyncService` |

```kotlin
interface SyncService {
    val isEnabled: Boolean
    suspend fun pushDetection(detection: Detection)
    suspend fun pullAggregates()
}
```

Menukar implementasi = mengubah satu baris di `Bootstrap.create()`. Tidak ada
perubahan di `domain/` maupun `presentation/`.

---

## 3. Startup

1. `MozzApplication` start, menyiapkan `CompletableDeferred<Bootstrap>`.
2. `Bootstrap.create(context)` merakit semua singleton di satu tempat:
   - buka database Room (`mozzid.db`)
   - seed 6 data demo Jakarta jika tabel masih kosong
   - bangun `SpeciesCatalog`, `MockSpeciesClassifier` (+`load()`),
     `RoomDetectionRepository`, recorder, location service
   - **pilih implementasi sync** (lihat bagian 6)
3. `MainActivity` menunggu Bootstrap siap lewat `produceState`, lalu merender
   `HomeScreen(boot)`.

---

## 4. Alur Utama — Rekam dan Identifikasi Nyamuk

State machine di `RecordViewModel`: `IDLE → LISTENING → ANALYZING → RESULT`

```
User tekan-tahan tombol
  ↓
startHold() — cek izin mikrofon, recorder.start()
  ↓
LISTENING — tick tiap 16ms, progress naik sampai 4000ms
  ↓
finishListening() — recorder.stop() mengembalikan (filePath, durasi)
  ↓
ANALYZING — classifier.classify(AudioSample(...))
             mock: delay 1700ms, pilih spesies acak,
             confidence 78–94%, runner-up dari sisa persentase
  ↓
RESULT — kartu hasil tampil (glyph severity + nama + Hz + confidence)
  ↓
GPS best-effort — kalau gagal, tetap lanjut dengan koordinat null
  ↓
detections.add(Detection(...))  →  MASUK ROOM (sumber kebenaran)
  ↓
sync.pushDetection(saved)       →  Firebase (best-effort, boleh gagal)
```

Kalau user melepas tombol sebelum 4 detik, `endHold()` membatalkan dan
mengembalikan state ke `IDLE`.

**Catatan penting:** penyimpanan ke Room terjadi **sebelum** dan **terpisah
dari** sync. Kegagalan Firebase tidak pernah membatalkan penyimpanan lokal.

---

## 5. Kenapa History Langsung Ter-update

`RoomDetectionRepository.watch()` mengembalikan `Flow<List<Detection>>`. Room
otomatis meng-emit ulang setiap kali tabel `detections` berubah. UI memakai
`collectAsState()`, jadi begitu detection baru masuk, daftar re-compose sendiri.
Tidak ada refresh manual.

---

## 6. Lapisan Firebase

### 6.1 Pemilihan implementasi saat boot

```kotlin
// Bootstrap.create()
val sync: SyncService =
    if (FirebaseSyncService.isAvailable(context)) FirebaseSyncService(context)
    else NoopSyncService
```

`isAvailable()` memeriksa apakah `google-services.json` benar-benar terpasang
dan Firebase berhasil diinisialisasi. Kalau tidak, aplikasi jatuh balik ke
`NoopSyncService` dan berjalan 100% offline seperti biasa.

### 6.2 Layanan Firebase yang dipakai

| Layanan | Fungsi di MozzID |
|---|---|
| **Authentication (Anonymous)** | Identitas perangkat tanpa login. Dipakai sebagai pemilik dokumen di Firestore dan untuk aturan keamanan. |
| **Cloud Firestore** | Menyimpan detection milik user dan agregat sebaran spesies per wilayah. |
| **Cloud Messaging (FCM)** | Push peringatan wabah/lonjakan kasus untuk wilayah user. |
| **Remote Config** | Distribusi versi model ML, ambang confidence, dan flag fitur tanpa rilis ulang. |
| **Crashlytics + Analytics** | Pantau crash dan pemakaian fitur. |
| **Cloud Storage** *(opsional)* | Unggah potongan audio yang disetujui user untuk melatih ulang model. Default mati. |

### 6.3 Struktur data Firestore

```
users/{uid}/
  detections/{detectionId}
    speciesId: String
    confidence: Int
    wingbeatHz: Int
    timestamp: Timestamp
    latitude: Double?      // dibulatkan ~1 km sebelum dikirim
    longitude: Double?
    locationLabel: String?

aggregates/{geohash}/
    speciesCounts: Map<String, Int>
    lastUpdated: Timestamp
    riskLevel: String       // dihitung Cloud Function, bukan client
```

Koordinat **dibulatkan** sebelum meninggalkan perangkat. Lokasi presisi penuh
hanya ada di database lokal.

### 6.4 Alur sync

**Push (naik):**

```
Detection tersimpan di Room
  ↓
sync.pushDetection(detection)
  ↓
Firestore SDK menulis ke cache lokalnya, langsung balik sukses
  ↓
SDK mengirim ke server saat jaringan tersedia (antre otomatis kalau offline)
```

Persistensi offline Firestore diaktifkan, jadi antrean saat tidak ada sinyal
ditangani SDK — bukan kode kita.

**Pull (turun):**

```
pullAggregates()
  ↓
baca aggregates/{geohash} untuk wilayah user
  ↓
simpan ke tabel cache lokal
  ↓
UI membaca cache, bukan jaringan
```

UI **tidak pernah** membaca langsung dari Firestore. Selalu lewat Room. Jadi
layar tetap terisi meski offline.

### 6.5 Aturan keamanan Firestore

```
match /users/{uid}/detections/{doc} {
  allow read, write: if request.auth != null && request.auth.uid == uid;
}
match /aggregates/{geohash} {
  allow read: if request.auth != null;
  allow write: if false;   // hanya Cloud Function
}
```

Data user terisolasi per uid. Agregat hanya bisa dibaca client; penulisannya
dilakukan Cloud Function agar tidak bisa dimanipulasi dari perangkat.

### 6.6 Privasi

- Tidak ada data yang dikirim sebelum user menyalakan sync di Settings (opt-in).
- Koordinat dibulatkan ke grid ~1 km.
- Rekaman audio tidak pernah diunggah kecuali user menyetujuinya secara eksplisit.
- Menonaktifkan sync menghentikan pengiriman; data lokal tetap utuh.

---

## 7. Logika Murni (yang Diuji)

- `computeStats(log)` — total, breakdown per spesies (persen, urut terbesar),
  dan jam puncak. Bucket 2 jam, 12 slot. Output seperti `"10PM–12AM"`.
- `applyFilters(detections, filter, nowMillis)` — filter spesies + rentang waktu
  (`ALL` / `WEEK` 7 hari). `nowMillis` disuntik dari luar supaya test
  deterministik, bukan membaca jam sistem.

8 unit test lulus untuk kedua fungsi ini. Keduanya murni Kotlin, tidak butuh
emulator maupun Firebase.

---

## 8. Theming — Design Token

Warna tidak pernah di-hardcode. `MozzColors.of(dark, accent)` menghasilkan 17
warna dari kombinasi `(Brightness, AppAccent)`. Ada 4 accent
(TEAL/LIME/AMBER/INDIGO) × 2 mode. Disebar lewat `LocalMozzColors`
CompositionLocal, dibaca melalui `MozzTheme.colors`. Mengganti accent di
Settings langsung mengubah warna seluruh aplikasi.

---

## 9. Kontrak Aksesibilitas

Severity **tidak pernah** disampaikan lewat warna saja. Selalu bentuk glyph +
label teks:

- ▲ HIGH (merah)
- ● MODERATE (kuning)
- ■ LOW (biru)

Pengguna buta warna tetap bisa membedakan tingkat risiko.

---

## 10. Status Implementasi

**Sudah jalan:** database Room, kedua seam, dependency injection, theming,
state machine perekaman, stats/filter, layar skeleton.

**Masih stub:** `StubAudioRecorder` (mengembalikan `"" to 4000L`) dan
`StubLocationService` (mengembalikan null). Mock classifier mengabaikan isi
audio, jadi alur end-to-end tetap terbukti berjalan.

**Belum diport:** onboarding, layar history + peta + stats, kartu spesies,
settings, custom painter (mascot, peta, spektrogram, confidence ring), TTS,
notifikasi, ekspor CSV.

**Firebase:** `FirebaseSyncService` belum ditulis — `NoopSyncService` masih
aktif. Seam-nya sudah siap, jadi pemasangannya tidak menyentuh kode lain.
