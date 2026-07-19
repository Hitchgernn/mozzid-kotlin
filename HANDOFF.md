# Session handoff prompt

Paste everything below into a new Claude Code session started inside
`~/projects/mozzid-kotlin`.

---

I'm building **MozzID** for a competition: an offline-first Android app that
identifies mosquito species from wingbeat audio, on-device. **Competition rules
require Kotlin/Android native — no Flutter, no cross-platform.**

This repo is a native port of a working Flutter app that lives at
`~/projects/mozzid` (kept as reference, not to be modified).

## Read these first, in this order

1. `PORTING_SPEC.md` — the distilled, stack-neutral brief: architecture, the two
   seams, exact design tokens (all colour hex values), domain models, species
   data, DB schema, record state machine, stats/filter logic. **This is the
   source of truth for the port.** Don't re-derive any of it.
2. `CLAUDE.md` — project conventions and non-negotiables.
3. `reference/` — line-level detail when the spec isn't enough:
   - `reference/prototype.html` — the original interactive design; source of truth
     for screens, copy, colours, motion.
   - `reference/flutter-src/lib/` — the full Dart implementation to port from.
   - `reference/flutter-src/lib/l10n/app_{en,id}.arb` — all UI strings, EN + ID.
   - `reference/fonts/` — the bundled TTFs (already copied into `app/src/main/res/font/`).
   - `reference/docs/` — ARCHITECTURE.md, README.md, BACKLOG.md from the Flutter build.

## What already exists (scaffolded and building)

Gradle/Compose project, package `com.mozzid`, in clean three-layer architecture:

- **domain/** (pure Kotlin, no Android framework)
  - `model/` — `Detection`, `Species` + `ActiveWindow`, `Severity` (colourblind-safe
    glyphs ▲/●/■), `ClassificationResult`
  - `classifier/SpeciesClassifier.kt` — **ML seam** + `AudioSample`
  - `sync/SyncService.kt` — **backend seam** (Firebase goes here, optional)
  - `repository/` — `DetectionRepository`, `SpeciesRepository`,
    `AudioRecorderService`, `LocationService`, `GeoFix`
  - `stats/` — `computeStats` / `formatWindow`, `applyFilters` / `LogFilter` /
    `DateRange` (pure, `now` injected for determinism — unit-tested)
- **data/** — `SpeciesCatalog` (3 species, full copy), `MockSpeciesClassifier`
  (1700ms delay, 78–94 confidence, per-species Hz ranges), `NoopSyncService`,
  Room (`DetectionEntity`, `DetectionDao`, `SettingEntity`/`SettingsDao`,
  `MozzDatabase`, `RoomDetectionRepository`, `DemoSeeder` with the 6 Jakarta demo rows)
- **presentation/** — `theme/` (`AppAccent` 4 accents, `MozzColors` full token set
  via `LocalMozzColors`, `MozzType` bundled fonts, `Dimens`, `MozzTheme`),
  `record/` (`RecordState`, `RecordViewModel` — the idle→listening(4s)→analyzing→
  result state machine), `HomeScreen.kt` (skeleton screen driving the real flow)
- `Bootstrap.kt` — single composition root; swapping any implementation is one
  line here. `MozzApplication`, `MainActivity`.
- Tests: `app/src/test/kotlin/com/mozzid/{StatsTest,LogFiltersTest}.kt`

## Verified state

- `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL, 8/8 tests pass**
  (Kotlin + KSP/Room + Compose all compile clean).
- **The APK has NOT been built yet** — `:app:assembleDebug` has never run.
- Git repo exists on branch `main` with **zero commits**; everything is untracked.
- The app has never been run on a device or emulator.

## Toolchain gotchas (important)

- **Always `export JAVA_HOME=/home/hitchgernn/development/jdk17` before Gradle.**
  The `java` on PATH is JDK 25, which AGP rejects.
- `local.properties` already points at `sdk.dir=/home/hitchgernn/Android/Sdk`
  (it's gitignored). SDK platforms 34/35/36 and build-tools 34/35/36 are installed.
- Gradle wrapper 8.11.1 is committed-ready; AGP 8.7.3, Kotlin 2.0.21, compileSdk 36,
  minSdk 26. Room via KSP.
- The user's physical phone (OnePlus, `adb` id `1439b923`) drops off USB often —
  `adb kill-server && adb start-server` usually fixes it. KVM is available if an
  emulator is preferable.

## Non-negotiables

- **Offline-first:** zero required backend. `NoopSyncService` stays the default.
  Firebase only ever drops in behind the `SyncService` interface, and a missing
  `google-services.json` must fall back to Noop.
- **Two seams only:** `SpeciesClassifier` (ML) and `SyncService` (backend). The
  record→analyze→result→save flow must never touch a concrete model or backend.
- **Clean layers:** `domain/` stays pure Kotlin. No Android imports in domain.
- **Colourblind-safe severity:** glyph + text label + colour, always. Never colour alone.
- **Design tokens:** every colour/font/dimension comes from the theme accessors
  (`MozzTheme.colors`, `MozzType`, `Dimens`) — never hardcoded. Accent/brightness
  swap must recolour the whole app live.
- Commits must end with the trailer:
  `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

## What to do next (suggested order)

1. Run `:app:assembleDebug` and confirm the APK packages; then run it on a device
   or emulator to see the skeleton flow work end-to-end.
2. Make the first git commit (the scaffold is currently uncommitted).
3. Replace the stubs in `data/location/StubServices.kt` with real implementations:
   `AudioRecord`/`MediaRecorder` capture and `FusedLocationProviderClient`, plus
   the runtime permission flow for RECORD_AUDIO / location.
4. Port the remaining screens from `reference/flutter-src/lib/presentation/`:
   onboarding, full record screen (mascot + spectrogram + confidence ring),
   result view, history (log + stylized offline map with staggered pin drop-in +
   stats card + filters), species sheet, morning summary, settings.
   The custom painters become Compose `Canvas` — geometry and animation timings
   are documented in `PORTING_SPEC.md` §11 and in the Dart sources.
5. Move UI strings into `res/values/strings.xml` + `res/values-in/strings.xml`
   from the `.arb` files, and wire the in-app language switch.
6. Then, and only then, the optional `FirebaseSyncService` (Firestore + anonymous
   Auth) behind the existing seam.

Start by reading `PORTING_SPEC.md` and `CLAUDE.md`, then confirm the build still
passes before changing anything.
