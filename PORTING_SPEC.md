# MozzID — Kotlin/Android Porting Spec

This is the **distilled, stack-neutral knowledge** for porting MozzID from the
Flutter/Dart implementation (in `reference/flutter-src/`) to native Kotlin
Android. It captures everything that survives the language change: architecture,
the two seams, exact design tokens, domain models, data, and logic. Read this
first; drop into the reference source only for line-level detail.

> **Source of truth for pixels/copy/motion:** `reference/prototype.html` (the
> original interactive design). The Flutter code is the source of truth for
> architecture, models, and logic.

---

## 0. What MozzID is

Offline-first Android app that identifies mosquito species from wingbeat audio,
on-device. The ML model is **deferred** — classification runs behind an
interface returning a mock today. Firebase is **optional sync only**; the app
must work fully offline with zero backend.

**Competition constraint:** Kotlin only, Android only (no Flutter, no
cross-platform). Was cross-platform; now single platform.

---

## 1. Stack mapping (Flutter → Kotlin/Android)

| Flutter/Dart | Kotlin/Android target |
|---|---|
| Flutter widgets | **Jetpack Compose** (recommended) |
| Riverpod providers + Notifier | **ViewModel + StateFlow**; DI via Hilt (or manual `Bootstrap` object) |
| `bootstrapProvider` single override | A `Bootstrap`/`AppContainer` object built in `Application.onCreate` |
| sqflite (raw SQL) | **Room** (entities + DAO), or raw `SQLiteOpenHelper` to mirror SQL 1:1 |
| in-memory cache + broadcast `watch()` | Room `Flow<List<…>>` query (reactive by default) |
| `record` plugin | `AudioRecord` / `MediaRecorder` |
| geolocator | `FusedLocationProviderClient` (Play Services Location) |
| flutter_local_notifications | `NotificationManager` + channel |
| flutter_tts | `android.speech.tts.TextToSpeech` |
| share_plus | `Intent.ACTION_SEND` / `FileProvider` |
| csv package | hand-rolled CSV writer (trivial) |
| google_fonts + bundled TTFs | `res/font/` + Compose `FontFamily` (TTFs already in `reference/fonts/`) |
| `.arb` (app_en / app_id) | `res/values/strings.xml` + `res/values-in/strings.xml` (Indonesian = `in`, legacy code for `id`) |
| TFLite seam (skeleton) | `org.tensorflow:tensorflow-lite` — easier native |
| Firebase seam (Noop default) | Firebase Android SDK (Firestore + Auth) behind same interface |
| CustomPaint (mascot/map/spectrogram) | Compose `Canvas` / `DrawScope` |
| ThemeExtension `MozzColors` | Compose `MaterialTheme` + custom `CompositionLocal<MozzColors>` |

---

## 2. Architecture — 3 layers, dependencies point inward

```
presentation  →  domain  ←  data
(Compose,          (pure Kotlin:      (Room, AudioRecord,
 ViewModel)         models, interfaces, Fused location, TTS,
                    stats logic)        notifications, CSV)
```

- **domain/** — pure Kotlin. No Android framework, no Room, no Compose. Models,
  the two seam interfaces, repository interfaces, and the unit-tested stats/
  filter logic.
- **data/** — concrete implementations of domain interfaces.
- **presentation/** — Compose screens + ViewModels (StateFlow).

**Two seams everything routes through — keep them as Kotlin interfaces:**

### Seam 1 — `SpeciesClassifier` (ML boundary)
```kotlin
data class AudioSample(
    val filePath: String,
    val durationMs: Long,
    val sampleRate: Int = 44100,
)

interface SpeciesClassifier {
    suspend fun load()                                  // idempotent
    suspend fun classify(sample: AudioSample): ClassificationResult
    suspend fun dispose()
}
```
- `MockSpeciesClassifier` live today (see §6). `TfliteSpeciesClassifier` later.
- The whole record→analyze→result→save flow depends only on this interface.

### Seam 2 — `SyncService` (backend boundary, optional)
```kotlin
interface SyncService {
    val isEnabled: Boolean
    suspend fun pushDetection(detection: Detection)     // best-effort, never blocks UI
    suspend fun pullAggregates()                        // no-op when disabled
}
object NoopSyncService : SyncService {                  // DEFAULT binding — 100% offline
    override val isEnabled = false
    override suspend fun pushDetection(detection: Detection) {}
    override suspend fun pullAggregates() {}
}
```
- Default `NoopSyncService`. `FirebaseSyncService` (Firestore push + anon Auth
  scoping + aggregate pull) is the intended drop-in. Swap in `Bootstrap` only.

**Rule:** device capabilities/backends get an interface in `domain/`, an impl in
`data/`, wired in `Bootstrap`. Never import Android plugins from domain/presentation.

---

## 3. Domain models (exact fields)

### Detection (persisted record)
| field | type | notes |
|---|---|---|
| id | Int? / Long? | null until inserted (autoincrement) |
| speciesId | String | FK-ish key: `aedes` / `culex` / `anopheles` |
| confidence | Int | 0–100 |
| wingbeatHz | Int | measured fundamental |
| timestamp | epoch millis (Long) | store as Long; `DateTime` in Dart |
| latitude | Double? | null when GPS unavailable/denied |
| longitude | Double? | null likewise |
| locationLabel | String? | e.g. "Bedroom"; falls back to coords |

### Species (static reference — see §5 for the 3 records)
`id, scientificName, commonName, diseases, wingbeatHz (str "~600 Hz"),
wingbeatRange (str "450–700 Hz"), severity, activeWindow, activeLabel, note,
tips: List<String>, dotColor: Int(ARGB)`

`ActiveWindow` enum: `day, night, duskToDawn` (`day.isDayBiter == true`).

### ClassificationResult (transient, not persisted)
`primary: Species, runner: Species, confidence: Int(0–100),
runnerConfidence: Int(0–100), wingbeatHz: Int`

### Severity — **colourblind-safe by contract**
Every level = glyph shape **+** text label **+** colour. Never colour alone.
| level | glyph | colour (ARGB) |
|---|---|---|
| high | ▲ | `0xFFFF8A7A` |
| moderate | ● | `0xFFFFCF6B` |
| low | ■ | `0xFF7FD0FF` |
Derived: `bg = color@10%`, `border = color@28%`, `iconBg = color@16%`.

---

## 4. Design tokens

### Accents (`AppAccent`) — 4 brands, each 4 colours: base, deep, bright, ink
| accent | base `--accent` | deep `--accent2` | bright `--accent-hi` | ink `--accent-ink` |
|---|---|---|---|---|
| teal (default) | `FF2DD4BF` | `FF12A695` | `FF34E6D1` | `FF04211D` |
| lime | `FF84CC16` | `FF4D7C0F` | `FFBEF264` | `FF152400` |
| amber | `FFF5A623` | `FFB45309` | `FFFBBF24` | `FF2A1600` |
| indigo | `FF8B93F8` | `FF4F46E5` | `FFA5B4FC` | `FF0A0F2A` |

Changing accent recolours the whole app live; nothing else in the palette moves.

### MozzColors — full token set, per (Brightness, Accent)
Non-accent tokens (dark / light):
| token | dark | light |
|---|---|---|
| bg | `FF080B11` | `FFE7EBF1` |
| surface | `FF0F141D` | `FFFFFFFF` |
| surface2 | `FF0D121A` | `FFEEF2F7` |
| surface3 | `FF0C1119` | `FFFFFFFF` |
| text | `FFEEF3F9` | `FF0F1720` |
| text2 | `FFC6D0DD` | `FF33414F` |
| text3 | `FF93A1B3` | `FF5A6675` |
| text4 | `FF6B7788` | `FF8592A1` |
| faint | `FF4A5567` | `FFAEB8C4` |
| line | `0FFFFFFF` | `1A101823` |
| line2 | `1FFFFFFF` | `2E101823` |
| fill | `0AFFFFFF` | `0D101823` |

Accent tokens: `accent=base, accent2=deep, accentHi=bright, accentInk=ink,
accentSoftText = dark ? bright : deep`.
Helper `accentMix(pct)` = accent at `pct%` alpha (mirrors CSS `color-mix … N%`).

In Compose: hold these in a `data class MozzColors`, provide via
`staticCompositionLocalOf`, recompute on brightness/accent change so the whole
tree recolours.

### Typography (`MozzType`) — 3 typefaces
- **Serif display** = Spectral (default weight 600)
- **UI** = IBM Plex Sans (default 400)
- **Numerics/labels** = IBM Plex Mono (default 500)

TTFs already bundled in `reference/fonts/` (12 files) → drop into `res/font/`,
build a `FontFamily` per typeface. Fully offline; no runtime font fetch.

### Dimens (radii/spacing, dp)
`screenRadius 42, cardRadius 18, tileRadius 16, chipRadius 999, sheetRadius 28,
pagePad 22, gap 12`.

---

## 5. Species catalog (the 3 static records — verbatim data)

**aedes** — *Aedes aegypti*, "Yellow fever mosquito", diseases "Dengue & Zika",
wingbeatHz "~600 Hz", range "450–700 Hz", severity **high**, activeWindow
**day**, activeLabel "Day · dawn/dusk", dotColor `FFFF8A7A`.
note: "A daytime biter thriving in urban water containers. The primary vector of
dengue across tropical cities."
tips: ["Empty standing water in pots, tyres and gutters weekly.", "Use screens
and repellent during daylight hours.", "Wear long sleeves at dawn and dusk."]

**culex** — *Culex quinquefasciatus*, "Southern house mosquito", diseases "West
Nile & filariasis", wingbeatHz "~350 Hz", range "300–450 Hz", severity
**moderate**, activeWindow **night**, activeLabel "Night", dotColor `FF7FD0FF`.
note: "A night-active house mosquito, drawn to polluted stagnant water. A vector
of lymphatic filariasis."
tips: ["Clear drains and polluted stagnant water.", "Sleep under a bed net at
night.", "Fit window and door screens."]

**anopheles** — *Anopheles sundaicus*, "Malaria mosquito", diseases "Malaria",
wingbeatHz "~500 Hz", range "400–600 Hz", severity **high**, activeWindow
**duskToDawn**, activeLabel "Night · dusk to dawn", dotColor `FFFFCF6B`.
note: "The malaria vector, biting from dusk to dawn. Rests at a distinctive
head-down angle."
tips: ["Sleep under an insecticide-treated net.", "Use indoor residual spraying
where advised.", "Cover skin after sunset."]

`classifiableIds = [aedes, culex, anopheles]` (catalogue order).

---

## 6. Mock classifier behaviour (port exactly)

- `classify()` waits **1700 ms** (mimics on-device inference / the analyze state).
- Picks random `primary` from `classifiableIds`; `runner` = different random id.
- `confidence = 78 + rand(0..16)` → 78–94.
- `runnerConfidence = max(3, 100 − confidence − rand(0..5))`.
- `wingbeatHz` inside species range: aedes 450–700, culex 300–450, anopheles
  400–600 (else 400–700).
- `load()` / `dispose()` are no-ops for the mock.

---

## 7. Record capture state machine

States: `idle → listening → analyzing → result` (+ `error`: none/micDenied/failed).
- `captureDuration = 4000 ms`. Hold gesture drives it.
- Ticker every **16 ms**: `progress = elapsed / 4000`, clamp 0..1; at 1.0 →
  finish listening → analyzing.
- Flow: check/request mic permission → `recorder.start()` → listening (progress
  ticker) → on complete stop recorder → `classifier.classify(sample)` (analyzing,
  spectrogram animation) → result → save.
- **Save path:** real timestamp + best-effort GPS (medium accuracy, ~8s timeout;
  null lat/lng if denied/timeout). Insert into repo → History updates live.

In Kotlin: a ViewModel exposing `StateFlow<RecordState>`; ticker via coroutine +
`delay(16)` loop or `withFrameNanos`.

---

## 8. Persistence

- SQLite DB `mozzid.db`, version 1. Two tables:
  ```sql
  CREATE TABLE detections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    species_id TEXT NOT NULL, confidence INTEGER NOT NULL,
    wingbeat_hz INTEGER NOT NULL, timestamp INTEGER NOT NULL,
    latitude REAL, longitude REAL, location_label TEXT);
  CREATE INDEX idx_detections_timestamp ON detections(timestamp DESC);
  CREATE TABLE settings (key TEXT PRIMARY KEY, value TEXT NOT NULL);
  ```
- Repository = offline source of truth. In Kotlin use Room with a
  `Flow<List<Detection>>` query so History recomposes on insert (replaces the
  Dart in-memory-cache + broadcast-stream pattern).
- Settings (language, theme brightness, accent, toggles, onboarding flag) live in
  the `settings` key/value table; expose as live state. (DataStore is an
  acceptable Kotlin alternative, but the SQL table keeps parity.)
- `seedDemoData = true`: seed 6 demo detections on first install (Jakarta coords,
  species/confidence/hz/timestamps in `reference/flutter-src/lib/data/local/app_database.dart`).

---

## 9. Stats + filter logic (pure — unit-test these)

### computeStats(log) → DetectionStats
- `total = log.size`.
- `breakdown`: per-species `SpeciesShare(speciesId, count, percent =
  round(count/total*100))`, sorted by count desc.
- `peakWindow`: 12 buckets of 2 hours (`hour / 2`); pick fullest bucket; format
  its start hour as `10PM–12AM` via 12-hour formatter. Null when log empty.

### applyFilters(detections, filter, now) → List<Detection>
- `LogFilter(speciesId: String?, range: DateRange{all, week})`.
- `isAllSpecies` = speciesId null or `"all"`.
- week cutoff = `now − 7 days`; keep if `timestamp.isAfter(cutoff)`.
- **Inject `now`** for determinism (this is why the logic is testable). Keep it a
  parameter in Kotlin too.

---

## 10. i18n

- Two locales: **EN** + **ID** (Indonesian). Full strings in
  `reference/flutter-src/lib/l10n/app_en.arb` and `app_id.arb`.
- Android: `res/values/strings.xml` (en default) + `res/values-in/strings.xml`
  (Indonesian folder qualifier is `in`, Android's legacy code for `id`).
- Language is user-selectable in Settings (overrides system locale) — persist the
  choice and apply per-app locale (`AppCompatDelegate.setApplicationLocales` or a
  config wrapper).

---

## 11. Screens (port list)

onboarding · home shell (nav) · record (idle/listening/analyzing/result states) ·
result view (confidence ring, metrics, severity banner, TTS speak, share) ·
history (log list + stylized map + stats card + filters) · species card/sheet ·
morning summary sheet · settings (language, theme, accent, toggles).

Custom-drawn pieces to reproduce on Compose `Canvas`:
- **MozzMascot** — mosquito, flapping wings + bob; body colours derived from
  accent (limb=accent2, abdomen=lerp(accent2,accent,0.5), thorax=accent,
  head=accentHi, stripes=ink@25%). Geometry in `mozz_mascot.dart`.
- **StylizedMap** — offline painted map (dotted grid + faux streets), pins placed
  by normalising GPS into frame; teardrop pins drop in staggered (80ms each,
  scale 0→1.15→1 over 500ms, tip down). Detail in `stylized_map.dart`.
- **Spectrogram** — animated frequency bars + accent gradient during analyzing.
- **ConfidenceRing** — arc gauge on result.

---

## 12. Reference map (where to look for line-level detail)

```
reference/
  prototype.html              ← design source of truth: screens, copy, colors, motion
  flutter-src/lib/
    domain/                   ← models, seams, stats/filter logic (port 1:1)
    data/                     ← impl patterns (DB schema, mock, recorder, location…)
    presentation/             ← screens + painters to reproduce on Compose
    l10n/app_{en,id}.arb      ← all UI strings, both locales
    core/theme/               ← tokens (already distilled in §4 above)
  docs/                       ← ARCHITECTURE.md, README.md, BACKLOG.md, CLAUDE.md (Flutter)
  fonts/                      ← 12 bundled TTFs → res/font/
```

## 13. Firebase (satisfies competition's Firebase requirement, stays optional)

Behind the `SyncService` seam only. Natural Firebase pieces:
- **Firestore** — mirror detections; has native offline persistence (fits offline-first).
- **Auth (anonymous)** — scope a user's detections across devices.
- **Cloud Functions + Firestore** — server-side species aggregation / heatmap.
- **FCM** — push high-activity alerts (complements local notifications).
- **Firebase ML / Storage + Remote Config** — deliver/update the TFLite model.

Guard wiring so a missing `google-services.json` falls back to `NoopSyncService`
— the app must still build and run fully offline.
