# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

MozzID — an offline-first Flutter (Dart) mobile app that identifies mosquito species from their wingbeat audio, on-device. Single codebase for Android + iOS. The wingbeat ML model is **deferred**: classification runs behind an interface that currently returns a mock.

## Commands

```bash
flutter pub get                         # deps; also generates l10n from lib/l10n/*.arb
flutter run                             # run on device (mic + GPS are real — prefer a physical device)
flutter test                            # all unit tests
flutter test test/log_filters_test.dart # a single test file
flutter test --name "week range"        # a single test by name
flutter analyze                         # static analysis (rules in analysis_options.yaml)
flutter build apk --release             # Android; also appbundle / ios
```

Platform folders (`android/`, `ios/`) are **not committed**. Before first run:
`flutter create . --platforms=android,ios --project-name mozzid`, then add the
mic/location/notification permissions from `README.md` to the generated
manifests. Localizations are generated on build (`generate: true`); `lib/l10n/gen/`
is gitignored, so `AppLocalizations` won't resolve in the analyzer until a build
has run.

## Architecture

Clean three-layer separation; dependencies point inward. `domain/` is pure Dart
(no Flutter widgets, no SQLite, no plugins) and both other layers depend on it.

- **`domain/`** — models, abstract interfaces, and pure logic. `stats/` (`computeStats`, `applyFilters`) is the unit-tested core and takes an injected `now` for determinism.
- **`data/`** — concrete implementations of the domain interfaces (sqflite, `record`, `geolocator`, notifications, TTS, CSV).
- **`presentation/`** — Riverpod providers + screens + widgets.

### Two seams that everything routes through

1. **ML — `SpeciesClassifier`** (`domain/classifier/species_classifier.dart`). The *only* boundary between the app and the model. The whole record→analyze→result→save flow depends on this interface, never a concrete model. `MockSpeciesClassifier` is live today; `TfliteSpeciesClassifier` is a skeleton. Swapping is one line in `Bootstrap.create()` — no UI/domain changes.
2. **Backend — `SyncService`** (`data/sync/sync_service.dart`). Bound to `NoopSyncService` by default so the app has **zero** backend dependency and works fully offline. Firebase is the intended drop-in, again via `Bootstrap.create()`.

When adding a device capability or backend, add the abstract interface in
`domain/`, implement it in `data/`, and wire it in `Bootstrap` — do not import
plugins from `presentation/` or `domain/`.

### Dependency injection

All singletons are assembled once in `Bootstrap.create()` (`presentation/providers/bootstrap.dart`) and injected through a **single** `bootstrapProvider` override in `main.dart`. Every other infrastructure provider (`classifierProvider`, `detectionRepositoryProvider`, …) derives from it. To swap an implementation, change `Bootstrap.create()`; to fake one in a test, override `bootstrapProvider`. Riverpod is used **without codegen** (plain `Notifier` / `Provider`).

### State + persistence

- `RecordController` (`presentation/providers/record_controller.dart`) is the capture state machine: `idle → listening (hold ~4s, Stopwatch+Timer ticker) → analyzing → result`. It owns the recorder/classifier/location/repo interactions and the save path (real timestamp + best-effort GPS).
- `LocalDetectionRepository` is the offline source of truth: an in-memory cache over sqflite plus a broadcast stream (`watch()`) so History updates immediately on save.
- Settings (language, theme, accent, toggles, onboarding flag) persist to a key/value table in the same SQLite DB, exposed live via `settingsProvider`.
- `AppDatabase.seedDemoData` seeds demo detections on first install; set `false` to start empty.

### Theming as design tokens

The full palette is a `MozzColors` `ThemeExtension` derived from `(Brightness, AppAccent)` (`core/theme/`). Read colors via `context.c` (extension in `app_theme.dart`); read fonts via `MozzType` (`typography.dart`, google_fonts); read strings via `context.l` (`core/l10n_ext.dart`). Changing theme or accent in Settings rebuilds `MaterialApp` and recolours the app live — so new UI should pull every color/font/string from those accessors, never hardcode.

### Conventions

- Severity is **colourblind-safe by contract**: every level pairs a glyph shape (▲/●/■) with a text label; never signal risk by colour alone.
- The History map (`StylizedMap`) is intentionally a fully-offline painted map, not real tiles — swapping in `flutter_map` is noted in `README.md`.
- `analysis_options.yaml` enables `strict-casts`/`strict-raw-types` and `require_trailing_commas`; match the existing formatting.

## Reference

`reference/prototype.html` is the original interactive design prototype — the source of truth for screens, copy, colors, and animations.
