# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

MozzID — an offline-first Android app that identifies mosquito species from
wingbeat audio, on-device. **Kotlin only, Android only** (a competition
constraint; the earlier cross-platform Flutter build is not allowed). The
wingbeat ML model is **deferred** — classification runs behind an interface
returning a mock. Firebase is **optional sync only**; the app must work fully
offline with zero backend.

This is a native port of a working Flutter app. **Read `PORTING_SPEC.md`
first** — it is the distilled, stack-neutral brief (architecture, the two seams,
exact design tokens, models, species data, DB schema, record state machine,
stats/filter logic) and the source of truth for the port. Don't re-derive it.

`reference/` holds the Flutter implementation and is **read-only** — never
modify it:
- `reference/prototype.html` — original interactive design; source of truth for
  screens, copy, colours, motion
- `reference/flutter-src/lib/` — Dart implementation to port from
- `reference/flutter-src/lib/l10n/app_{en,id}.arb` — all UI strings, EN + ID
- `reference/docs/` — ARCHITECTURE.md, README.md, BACKLOG.md from the Flutter build

## Commands

**Every Gradle command needs JDK 17 first.** The `java` on PATH is JDK 25, which
AGP rejects. This applies to the command line only — Android Studio uses its own
bundled runtime.

```bash
export JAVA_HOME=/home/hitchgernn/development/jdk17

./gradlew :app:testDebugUnitTest     # unit tests (8 currently)
./gradlew :app:assembleDebug         # debug APK
./gradlew :app:lintDebug             # Android lint (~2.5 min)
./gradlew :app:compileDebugKotlin    # fastest compile check
```

Single test class or method:

```bash
./gradlew :app:testDebugUnitTest --tests "com.mozzid.StatsTest"
./gradlew :app:testDebugUnitTest --tests "com.mozzid.LogFiltersTest.week range drops older than 7 days"
```

Test results are XML at `app/build/test-results/testDebugUnitTest/`; lint
findings at `app/build/reports/lint-results-debug.xml` (parse the XML rather than
reading the HTML).

### Running on a device

```bash
export PATH=$PATH:$HOME/Android/Sdk/platform-tools
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Two environment traps here:

- **Signature clash.** Android Studio signs with a debug key inside its flatpak
  sandbox; the CLI uses `~/.android/debug.keystore`. Installing over the other's
  build fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Run
  `adb uninstall com.mozzid` first — this wipes the DB, which re-seeds on launch.
- **AVDs are invisible to the CLI.** Studio is a flatpak, so its AVDs live in
  `~/.var/app/com.google.AndroidStudio/config/.android/avd`, not `~/.android/avd`.
  `emulator -list-avds` finds nothing unless `ANDROID_AVD_HOME` points there. A
  running emulator is still reachable via `adb` normally.

Debugging a crash: `adb logcat -d -b crash` — the exception line is *above* the
stack frames, so `grep -B3` the topmost app frame rather than `grep -A`.

## Architecture

Three layers, dependencies pointing inward. `presentation` and `data` both depend
on `domain`, and never on each other.

**`domain/` is pure Kotlin and must stay that way** — no Android, Compose, or
Play Services imports. This is enforceable, so verify after touching it:

```bash
grep -rn "^import android\|^import androidx\|^import com.google" app/src/main/kotlin/com/mozzid/domain/
```

A consequence worth knowing: `Severity` and `Species` carry colours as ARGB
`Long`, not Compose `Color`. `presentation/theme/SeverityColors.kt` paints them
and derives the severity bg/border/iconBg tints.

`kotlinx.coroutines` (including `Flow`) *is* allowed in domain — it is pure
Kotlin, not Android.

### The two seams

These are the only boundaries to the model and any backend. The whole
record → analyze → result → save flow depends on the interfaces, never a concrete
implementation.

- **`SpeciesClassifier`** (`domain/classifier/`) — ML seam.
  `MockSpeciesClassifier` is live: waits 1700 ms, returns 78–94% confidence with
  per-species Hz ranges. `TfliteSpeciesClassifier` drops in later.
- **`SyncService`** (`domain/sync/`) — backend seam. `NoopSyncService` is the
  **default binding and stays that way**. Firebase must fall back to Noop when
  `google-services.json` is missing.

Capture writes **16-bit PCM WAV at 44.1 kHz** (not compressed) specifically so the
real model can read clips without a decode step.

### Wiring

`Bootstrap` is the single composition root — swapping any implementation is a
one-line change there. It is currently assembled in `MainActivity` (opening the
DB and seeding are suspending calls), *not* in `MozzApplication`.

> `MozzApplication` holds a `CompletableDeferred<Bootstrap>` that nothing ever
> completes. Awaiting it hangs forever. Complete it or delete it before adding a
> second entry point.

Adding a device capability or backend: interface in `domain/`, implementation in
`data/`, wired in `Bootstrap`. Never import an Android API from `domain/` or
`presentation/`.

### Two non-obvious presentation patterns

**`PermissionBridge`** (`data/permission/`) exists because services are built in
`Bootstrap` before any Activity exists, and `domain/` cannot reference Android
types. `MainActivity` binds an `ActivityResultLauncher` while composed and
unbinds on dispose; services call a suspending `request()`. With nothing bound it
resolves `false` rather than hanging. Requests are serialised through a `Mutex`.

**`ProvideAppLanguage`** (`presentation/AppLocale.kt`) overrides the locale live
without recreating the Activity. It must wrap the Activity as its `ContextWrapper`
base and override only `getResources()`. Using a bare
`createConfigurationContext()` returns a **detached** context, which severs the
chain Compose walks to find Activity-scoped owners — every
`rememberLauncherForActivityResult` then dies with *"No ActivityResultRegistryOwner
was provided"*. This crashed the app on every launch once; do not reintroduce it.

### State and persistence

Room over `mozzid.db`. `detections` is the offline source of truth; `settings` is
a **sparse** key/value table — a key is written only when changed and anything
absent falls back to the `AppSettings` default, so new settings need no
migration. Both are read as `Flow`, so history and settings update reactively.
`DemoSeeder` seeds 6 Jakarta rows on first install.

`RecordViewModel` is the state machine: `idle → listening (4s, 16ms ticker) →
analyzing → result`. Releasing early cancels and discards. GPS is best-effort —
a denied or slow fix still saves the detection with null coordinates.

The pure, unit-tested logic lives in `domain/stats/`. It takes `now` as a
parameter instead of reading the clock, which is what makes date-range filtering
deterministic — keep it that way.

### Localisation

90 strings plus one `<plurals>` in `res/values/strings.xml` (EN) and
`res/values-in/strings.xml` (ID — `in` is Android's legacy qualifier), with
identical key sets. The bulk came from the `.arb` files; regenerate rather than
hand-editing one side. Verify parity with:

```bash
diff <(grep -oE '(string|plurals) name="[a-z_0-9]+"' app/src/main/res/values/strings.xml | sort) \
     <(grep -oE '(string|plurals) name="[a-z_0-9]+"' app/src/main/res/values-in/strings.xml | sort)
```

Bundle language splitting is disabled in `app/build.gradle.kts` — with splits on,
Play could omit the language a user later picks and an offline app cannot fetch
it back.

Species names are **not** localised: `SpeciesCatalog` hardcodes English, and the
`.arb` files never had them either.

## Non-negotiables

- **Offline-first:** zero required backend. `NoopSyncService` stays the default.
- **Two seams only:** the capture flow must never touch a concrete model or backend.
- **Clean layers:** `domain/` stays pure Kotlin (verify with the grep above).
- **Colourblind-safe severity:** glyph (▲/●/■) + text label + colour, always.
  Never signal risk by colour alone. Severity colours stay fixed across accents.
- **Design tokens:** colours from `MozzTheme.colors`, fonts from `MozzType`, sizes
  from `Dimens`, text from `stringResource` — never hardcoded. Accent and
  brightness swaps must recolour the whole app live.
- Commits end with the trailer:
  `Co-Authored-By: Claude Opus 4.8 <noreply@anthropic.com>`

## Current state

Working and verified on an emulator: both seams, real mic capture, real GPS,
the full capture flow, Room persistence, the token system, both locales with a
live in-app switch, **all designed screens**, 14 passing tests.

Screens live under `presentation/` by feature (`record/`, `history/`, `settings/`,
`species/`, `onboarding/`, `morning/`), with `MozzApp.kt` as the tab shell and
overlay host, and shared widgets in `components/`. Custom vector work is Compose
`Canvas`: the mascot, the icon set (ported from the design's SVG paths), the
confidence ring, spectrogram, and offline map. Motion tokens are in
`theme/Motion.kt`, lifted from the design's CSS keyframes so timings cannot drift.

Not built: the real model, Firebase sync, background/passive listening (the toggle
and morning summary exist but no background service runs), voice output, and CSV
export — the export action only confirms with a toast.

Known bug: recorded WAVs overrun the 4s window (~5.4s captured) because the writer
loop drains after `stop()`. Harmless with the mock, but fix before TFLite work.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
