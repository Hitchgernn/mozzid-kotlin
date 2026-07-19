# CLAUDE.md

## What this is

MozzID (Kotlin/Android) — an offline-first Android app that identifies mosquito
species from wingbeat audio, on-device. **Kotlin only, Android only** (a
competition constraint; the earlier cross-platform Flutter build is not allowed).
The wingbeat ML model is **deferred** — classification runs behind an interface
returning a mock. Firebase is **optional sync only**; the app must work fully
offline with zero backend.

## Port from, don't reinvent

This project is a native port of the working Flutter app. **Read
`PORTING_SPEC.md` first** — it is the distilled, stack-neutral brief (architecture,
the two seams, exact design tokens, models, species data, logic). Drop into
`reference/` for line-level detail:

- `reference/prototype.html` — original interactive design; source of truth for
  screens, copy, colours, motion.
- `reference/flutter-src/lib/` — Dart implementation to port 1:1 (domain models,
  seams, stats/filter logic in `domain/`; impl patterns in `data/`; screens +
  custom painters in `presentation/`).
- `reference/flutter-src/lib/l10n/app_{en,id}.arb` — all UI strings, EN + ID.
- `reference/fonts/` — 12 bundled TTFs (Spectral / IBM Plex Sans / IBM Plex Mono)
  → `res/font/`.
- `reference/docs/` — ARCHITECTURE.md, README.md, BACKLOG.md.

## Target stack

Jetpack Compose · ViewModel + StateFlow · Room · AudioRecord/MediaRecorder ·
FusedLocationProviderClient · NotificationManager · TextToSpeech ·
TFLite (later) · Firebase (optional, behind the SyncService seam).

## Non-negotiables (from the design contract)

- **Offline-first:** zero required backend. `NoopSyncService` is the default sync
  binding; Firebase only swaps in behind the `SyncService` interface.
- **Two seams:** `SpeciesClassifier` (ML) and `SyncService` (backend) are the only
  boundaries to the model/server. Swap implementations in one `Bootstrap`/DI place.
- **Clean layers:** `domain/` is pure Kotlin (no Android framework); `data/`
  implements its interfaces; `presentation/` is Compose + ViewModels.
- **Colourblind-safe severity:** every risk level pairs a glyph (▲/●/■) + text
  label + colour. Never signal risk by colour alone.
- **Design tokens:** colours/fonts/strings come from the theme/token accessors,
  never hardcoded — accent + brightness swap recolours the whole app live.
