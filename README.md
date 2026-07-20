# MozzID

An offline-first **mosquito species identifier** for Android. Hold the record
button near a buzzing mosquito, MozzID captures about four seconds of its
wingbeat, identifies the species **on-device**, and tells you which diseases that
species carries — with a confidence score, the measured wingbeat frequency, and a
personal detection log and map.

Built for tropical regions: used at night, one-handed, and usually with no
signal. Kotlin and Jetpack Compose, Android only. Dark-first, four accent
colours, full English and Indonesian localisation.

> The wingbeat **model is deferred**. Identification runs behind a
> `SpeciesClassifier` interface that returns a realistic mock today, so a TFLite
> model can drop in later without touching a single screen. See
> [The two seams](#the-two-seams).

For the layer topology, seam contracts, DB schema, and capture pipeline, see
[`ARCHITECTURE.md`](ARCHITECTURE.md). For exact design tokens, screen geometry,
and porting detail, see [`PORTING_SPEC.md`](PORTING_SPEC.md).

---

## Status

This is a native Kotlin port of a working Flutter app, which is kept in
`reference/` for line-level detail and is not modified.

**Working, and verified on an emulator:** three-layer architecture with a
verified-pure domain layer, both seams, real microphone capture, real GPS, the
full record-analyze-result-save flow, Room persistence with demo seed data, all
designed screens (onboarding, record, result, history with the offline map,
species sheet, settings, morning summary), the complete design token system with
live accent and brightness switching, every string in both locales with a live
in-app language switch, and 14 passing unit tests.

**Not built yet:** the real classification model, optional Firebase sync,
background/passive listening (the toggle and morning summary exist, but no
background service runs), voice output, and CSV export — the export action
currently only confirms with a toast.

**Known issue:** recorded WAVs overrun the 4s capture window (~5.4s of PCM)
because the writer loop drains after `stop()`. Harmless against the mock, which
ignores audio content, but must be fixed before TFLite work.

---

## Quick start

### Requirements

- **JDK 17.** This matters — see the note below.
- Android SDK with platform 36 and build-tools 36
- Android Studio (optional; the command line is enough to build)

### The JDK gotcha

The Android Gradle Plugin rejects newer JDKs. If `java -version` on your PATH
reports anything above 17, every Gradle command must be prefixed:

```bash
export JAVA_HOME=/path/to/jdk17
```

This applies to the command line only. Android Studio uses its own bundled
runtime and is unaffected.

### Build and run

```bash
export JAVA_HOME=/path/to/jdk17

./gradlew :app:testDebugUnitTest    # unit tests
./gradlew :app:assembleDebug        # debug APK
./gradlew :app:lintDebug            # Android lint
./gradlew :app:assembleRelease      # release APK
```

The debug APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

To install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` must point at your SDK and is deliberately not committed:

```properties
sdk.dir=/path/to/Android/Sdk
```

---

## Permissions

Already declared in `AndroidManifest.xml`; all are requested at runtime.

| Permission | Why | Required? |
|---|---|---|
| `RECORD_AUDIO` | Capture the wingbeat | Yes — no recording without it |
| `ACCESS_COARSE_LOCATION` | Place detections on the map | No |
| `ACCESS_FINE_LOCATION` | More precise placement | No |
| `POST_NOTIFICATIONS` | Overnight activity alerts | No |

Only the microphone is genuinely required. A refused or slow location still
saves the detection with no coordinates — losing a location is acceptable,
losing the detection is not.

---

## Project layout

Three layers, dependencies pointing inward: `presentation` and `data` both
depend on `domain`, and never on each other.

```
app/src/main/kotlin/com/mozzid/
├── Bootstrap.kt              Single composition root — swap implementations here
├── MainActivity.kt           Applies persisted locale, theme and accent
├── domain/                   Pure Kotlin. No Android imports, enforced.
│   ├── model/                Detection, Species, Severity, AppSettings, ...
│   ├── classifier/           SpeciesClassifier interface        <- ML SEAM
│   ├── sync/                 SyncService interface              <- BACKEND SEAM
│   ├── repository/           Detection, Species, Settings, recorder, location
│   └── stats/                computeStats, applyFilters         <- unit-tested
├── data/                     Implementations of the domain interfaces
│   ├── classifier/           MockSpeciesClassifier (live today)
│   ├── local/                Room database, DAOs, repositories, demo seeder
│   ├── audio/                MicAudioRecorder (AudioRecord, PCM WAV)
│   ├── location/             FusedLocationService
│   ├── permission/           PermissionBridge
│   ├── species/              SpeciesCatalog (three species, static)
│   └── sync/                 NoopSyncService (the default binding)
└── presentation/             Compose + ViewModels
    ├── theme/                MozzColors, AppAccent, MozzType, Dimens, Motion
    ├── components/           Mascot, icon set, cards, chips, toggles
    ├── record/               RecordViewModel state machine + record/result screens
    ├── history/              Log, offline map, stats, filters
    ├── settings/             Appearance, language, toggles, data actions
    ├── species/              Species reference sheet
    ├── onboarding/           Three-page intro + permission primers
    ├── morning/              Overnight summary
    ├── AppLocale.kt          Live locale override
    └── MozzApp.kt            Tab shell, overlays, toast
```

**Dependency injection** is a single `Bootstrap` object, currently assembled in
`MainActivity` because opening the database and seeding demo data are suspending
calls. Swapping any implementation is a one-line change in `Bootstrap`.

> `MozzApplication` holds a `CompletableDeferred<Bootstrap>` that nothing ever
> completes — a leftover from an earlier wiring. Awaiting it would hang forever.
> Either complete it in `onCreate` and have `MainActivity` await it, or delete it.

**Storage** is Room over `mozzid.db`: `detections` is the offline source of
truth, `settings` is a sparse key/value table. Reads are `Flow`, so history and
settings update reactively. Demo rows are seeded on first install by
`DemoSeeder`.

**Theming** derives the full palette from brightness and accent, provided through
a `CompositionLocal`. Every screen reads colours from `MozzTheme.colors`, fonts
from `MozzType`, sizes from `Dimens` — never hardcoded, so an accent or
brightness change recolours the app live.

---

## The two seams

These are the only two boundaries between the app and the outside world.
Everything else is self-contained.

### ML seam: `SpeciesClassifier`

```
domain/classifier/SpeciesClassifier.kt      the interface + AudioSample
data/classifier/MockSpeciesClassifier.kt    live today
```

The entire record-analyze-result-save flow depends on this interface and never on
a concrete model. The mock ignores the audio, waits 1700 ms to imitate inference,
and returns a plausible primary and runner-up with 78-94 percent confidence and a
per-species wingbeat frequency.

Capture already writes **16-bit PCM WAV at 44.1 kHz** rather than a compressed
format, specifically so the real model can read clips as-is without a decode step.

To land the real model:

1. Add `org.tensorflow:tensorflow-lite`.
2. Bundle the model and labels in `assets/`.
3. Implement `TfliteSpeciesClassifier`: decode WAV, build a mel-spectrogram, run
   the model, take the top two, map labels to `Species` via `SpeciesRepository`.
4. Change one line in `Bootstrap`. No UI or domain changes.

### Backend seam: `SyncService`

```
domain/sync/SyncService.kt      the interface
data/sync/NoopSyncService.kt    the default binding — does nothing
```

The app is fully functional offline; sync is purely additive. `NoopSyncService`
is the default so nothing depends on a backend. To add Firebase, implement
`FirebaseSyncService` against the interface and swap it in `Bootstrap`. Wire it so
a missing `google-services.json` falls back to `NoopSyncService` — the app must
still build and run entirely offline.

---

## Testing

```bash
export JAVA_HOME=/path/to/jdk17
./gradlew :app:testDebugUnitTest
```

14 tests cover the pure statistics, filtering, and active-window logic in
`domain/`. That logic takes `now` (or the hour) as a parameter rather than
reading the clock, which is what makes date-range filtering and the active-hours
cross-check deterministically testable.

---

## Conventions

- **Offline-first is structural, not aspirational.** `NoopSyncService` stays the
  default binding. The app must work fully with zero backend.
- **`domain/` stays pure Kotlin.** No Android, Compose, or Play Services imports.
  Verify with:
  ```bash
  grep -rn "^import android\|^import androidx\|^import com.google" app/src/main/kotlin/com/mozzid/domain/
  ```
- **Risk is never signalled by colour alone.** Every severity level pairs a glyph
  (triangle, circle, square) with a text label. Around one in twelve men has some
  form of colour blindness, and risk is the most important thing on the screen.
- **No hardcoded colours, fonts, sizes, or strings.** Read them from
  `MozzTheme.colors`, `MozzType`, `Dimens`, and `stringResource`.
- **New capabilities get an interface in `domain/`, an implementation in `data/`,
  and wiring in `Bootstrap`.** Never import an Android API from `domain/` or
  `presentation/`.
- Commits end with the trailer:
  `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

---

## Localisation

All 90 strings plus one `<plurals>` live in `res/values/strings.xml` (English)
and `res/values-in/strings.xml` (Indonesian — `in` is Android's legacy qualifier
for `id`), with identical key sets on both sides. The bulk were generated from
the reference `.arb` files so the two locales cannot drift apart.

The in-app language switch overrides the system locale and applies immediately
without restarting, by overriding the Context and Configuration the composition
reads. Bundle language splitting is disabled in `app/build.gradle.kts`: with
splits enabled, Play could omit the language a user later selects, and an
offline-first app has no way to fetch it back.

---

## Toolchain

| Component | Version |
|---|---|
| Gradle | 8.13 |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.12.01 |
| Room (via KSP) | 2.6.1 |
| compileSdk / targetSdk | 36 |
| minSdk | 26 (Android 8.0) |

---

## Reference material

`reference/` holds the original Flutter implementation and design, kept for
porting detail and never modified:

- `reference/prototype.html` — the original interactive design, and the source of
  truth for screens, copy, colours, and motion
- `reference/flutter-src/lib/` — the Dart implementation being ported
- `reference/fonts/` — the bundled Spectral and IBM Plex typefaces
- `reference/docs/` — architecture, README, and backlog from the Flutter build
