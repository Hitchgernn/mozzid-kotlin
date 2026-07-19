# MozzID

An offline-first **mosquito-species identifier**. Hold the record button near a
buzzing mosquito, MozzID captures ~4 seconds of its wingbeat, classifies the
species **on-device**, and tells you which disease it carries — with a
confidence score, the measured wingbeat frequency, and a personal detection log
+ map. Built for tropical regions, used at night, one-handed, often offline.

Flutter (Dart), single codebase for Android + iOS. Dark-mode-first, four brand
accents, full EN/ID localization.

> The wingbeat **ML model is deferred**. Classification runs behind a clean
> `SpeciesClassifier` interface that currently returns a realistic mock, so a
> TFLite CNN can drop in later without touching the UI. See
> [The ML seam](#the-ml-seam-deferred).

---

## Quick start

```bash
# 1. Flutter 3.27+ (Dart 3.6+). Check:
flutter --version

# 2. Generate the platform folders this repo doesn't commit (android/ ios/).
#    Safe: it only scaffolds what's missing, never overwrites lib/.
flutter create . --platforms=android,ios --project-name mozzid

# 3. Add the permissions listed below to the generated manifests.

# 4. Install deps (localizations are generated automatically from lib/l10n).
flutter pub get

# 5. Run on a device (a real device is best — the mic + GPS are real).
flutter run
```

### Build release

```bash
flutter build apk --release          # Android
flutter build appbundle --release    # Play Store
flutter build ios --release          # iOS (needs Xcode signing)
```

### Test

```bash
flutter test
```

Unit tests cover the pure log / filter / stats logic and the detection model
round-trip: `test/detection_stats_test.dart`, `test/log_filters_test.dart`,
`test/detection_model_test.dart`.

---

## Required permissions

Add to `android/app/src/main/AndroidManifest.xml` (inside `<manifest>`):

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
```

Add to `ios/Runner/Info.plist`:

```xml
<key>NSMicrophoneUsageDescription</key>
<string>MozzID listens to a mosquito's wingbeat to identify the species.</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>MozzID tags each detection with its location to build your map.</string>
```

---

## Architecture

Clean, three-layer separation. Dependencies point inward: `presentation` →
`domain` ← `data`. The domain layer knows nothing about Flutter widgets,
SQLite, or any plugin.

```
lib/
├── core/                     Cross-cutting: theming + design tokens, l10n access
│   └── theme/                MozzColors (ThemeExtension), AppAccent, typography
├── domain/                   Pure Dart — models, interfaces, business logic
│   ├── models/               Species, Detection, ClassificationResult, Severity
│   ├── classifier/           SpeciesClassifier interface  ← THE ML SEAM
│   ├── repositories/         DetectionRepository, SpeciesRepository (abstract)
│   ├── services/             AudioRecorderService, LocationService (abstract)
│   └── stats/                computeStats + applyFilters  ← unit-tested logic
├── data/                     Implementations of the domain interfaces
│   ├── classifier/           MockSpeciesClassifier (now) + Tflite… (skeleton)
│   ├── local/                sqflite database, DAO, settings store
│   ├── detection/            LocalDetectionRepository (offline source of truth)
│   ├── species/              SpeciesCatalog (static on-device knowledge)
│   ├── audio/  location/     record + geolocator adapters
│   ├── notifications/  voice/  export/   local notifications, TTS, CSV
│   └── sync/                 SyncService seam (Noop default; Firebase later)
├── presentation/
│   ├── providers/            Riverpod: bootstrap, settings, detections, record
│   ├── screens/              onboarding, home shell, record, result, history, settings
│   └── widgets/              mascot, rings, spectrogram, map, species/morning sheets
├── l10n/                     app_en.arb / app_id.arb  (generated → l10n/gen)
├── app.dart                  MaterialApp: live theme + locale, onboarding gate
└── main.dart                 Assembles Bootstrap, injects one provider override
```

**State management:** Riverpod (no codegen). Infrastructure is assembled once in
`Bootstrap.create()` and injected through a single `bootstrapProvider` override
in `main.dart`; everything else derives from it.

**Storage:** `sqflite` (raw SQL, no build step). `detections` is the offline
source of truth; settings live in a key/value table in the same DB. A handful of
demo detections are seeded on first install — toggle
`AppDatabase.seedDemoData = false` to start empty.

**Theming as design tokens:** the whole palette is a `MozzColors`
`ThemeExtension` derived from `(Brightness, AppAccent)`. Switching theme or
accent in Settings rebuilds `MaterialApp` and recolours the app live.

---

## The ML seam (deferred)

The **only** boundary between the app and the wingbeat model:

```
lib/domain/classifier/species_classifier.dart   ← the interface
lib/data/classifier/mock_species_classifier.dart ← STUB in use today
lib/data/classifier/tflite_species_classifier.dart ← drop-in skeleton
```

The entire flow — record → analyze → result → save — depends on the
`SpeciesClassifier` interface, never on a concrete model. The mock ignores the
audio and returns a plausible primary + runner-up + frequency after a short
"inference" delay.

**To land the real model:**

1. Add `tflite_flutter` (+ helpers) to `pubspec.yaml`.
2. Bundle `assets/model/wingbeat_cnn.tflite` and `labels.txt`.
3. Implement `TfliteSpeciesClassifier`: decode WAV → resample → mel-spectrogram
   → CNN → softmax → top-2 → map labels to `Species` via `SpeciesRepository`.
4. In `Bootstrap.create()`, swap `MockSpeciesClassifier` →
   `TfliteSpeciesClassifier`. **No UI or domain changes.**

Deliberately out of scope for now: the model itself, its training dataset, and
any server-side aggregation.

---

## Backend (optional, Firebase)

The app is **fully functional offline** — a backend is additive. The only seam
is `SyncService` (`lib/data/sync/sync_service.dart`), bound to `NoopSyncService`
by default so nothing depends on Firebase.

**To enable cross-device sync + aggregate maps + model updates:**

1. Add `firebase_core` + `cloud_firestore`; run `flutterfire configure`.
2. Implement `FirebaseSyncService` against the `SyncService` interface (push
   local detections, pull aggregates).
3. Override `syncServiceProvider` in `main.dart`. No UI/domain changes.

---

## Notes

- **Real map tiles:** History uses a fully-offline stylised map. For real tiles,
  swap `StylizedMap`'s painted background for a `flutter_map` `FlutterMap` +
  `TileLayer` (Mapbox/OSM) and a `MarkerLayer` from the same detections. Kept
  offline by default to honour the offline-first requirement.
- **Offline fonts:** fonts come from `google_fonts` (fetched + cached on first
  launch). For guaranteed offline typography, bundle the Spectral / IBM Plex
  Sans / IBM Plex Mono TTFs under `assets/fonts` and point `MozzType` at them.
- **Accessibility:** result screens carry semantic labels, severity is
  colourblind-safe (glyph + label, never colour alone), and voice output (TTS)
  reads the result aloud in the active language.

---

## Design reference

The original interactive design prototype lives at
[`reference/prototype.html`](reference/prototype.html).
