# MozzID — Architecture

Offline-first Flutter app that identifies mosquito species from their wingbeat,
on-device. This document explains the big-picture structure: the layering, the
two deliberate seams (ML + backend), how a detection flows from mic to log, and
where state and persistence live.

---

## 1. Layered architecture

Clean architecture with a strict **inward** dependency rule. `domain/` is pure
Dart and depends on nothing else; `data/` and `presentation/` both depend on
`domain/`, never on each other's concretions.

```mermaid
flowchart TB
    subgraph PRES["presentation/  (Flutter + Riverpod)"]
        screens["screens/<br/>onboarding · record · result<br/>history · settings"]
        widgets["widgets/<br/>mascot · rings · spectrogram<br/>map · sheets"]
        providers["providers/<br/>settings · detections · record"]
    end

    subgraph DOM["domain/  (pure Dart — no Flutter, no plugins)"]
        models["models/<br/>Species · Detection<br/>ClassificationResult · Severity"]
        interfaces["interfaces<br/>SpeciesClassifier · DetectionRepository<br/>AudioRecorderService · LocationService"]
        logic["stats/<br/>computeStats · applyFilters"]
    end

    subgraph DATA["data/  (implementations)"]
        classifier["classifier/<br/>Mock · TFLite skeleton"]
        local["local/<br/>sqflite · DAO · settings"]
        repos["repositories<br/>LocalDetectionRepository · SpeciesCatalog"]
        adapters["adapters/<br/>record · geolocator · notifications<br/>TTS · CSV"]
        sync["sync/<br/>SyncService (Noop → Firebase)"]
    end

    boot["Bootstrap + bootstrapProvider<br/>(main.dart wires it all once)"]

    PRES --> DOM
    DATA --> DOM
    boot -. injects .-> DATA
    boot -. overrides .-> providers
    providers --> interfaces
    classifier -. implements .-> interfaces
    repos -. implements .-> interfaces
    adapters -. implements .-> interfaces

    classDef pure fill:#0d1b2a,stroke:#2dd4bf,color:#e6f4f1;
    class DOM pure;
```

**The rule in practice:** to add a capability (a sensor, an export format, a
backend), declare an interface in `domain/`, implement it in `data/`, and wire
it in `Bootstrap`. Widgets and domain logic never import a plugin directly.

---

## 2. Dependency injection

Everything is assembled once in `Bootstrap.create()` and injected through a
**single** `bootstrapProvider` override in `main.dart`. Every other
infrastructure provider derives from it, so swapping an implementation (Mock →
TFLite classifier, Noop → Firebase sync) is a one-line change in one file.

```mermaid
flowchart LR
    main["main.dart"] -->|"await"| create["Bootstrap.create()"]
    create -->|"overrideWithValue"| bp["bootstrapProvider"]
    bp --> cls["classifierProvider"]
    bp --> rep["detectionRepositoryProvider"]
    bp --> spc["speciesRepositoryProvider"]
    bp --> aud["audioRecorderProvider"]
    bp --> loc["locationServiceProvider"]
    bp --> ntf["notificationServiceProvider"]
    bp --> tts["ttsServiceProvider"]
    bp --> csv["csvExporterProvider"]
    bp --> syn["syncServiceProvider"]

    cls --> rc["RecordController"]
    rep --> dp["detectionsProvider"]
    rep --> rc
    loc --> rc
```

Riverpod is used **without codegen** — plain `Notifier` / `Provider`. In tests,
override `bootstrapProvider` with fakes to drive the whole graph.

---

## 3. The two seams

### ML seam — `SpeciesClassifier`

The single boundary between the app and the wingbeat model. The whole
record→analyze→result→save flow depends on this interface, never a concrete
model.

| File | Role |
|------|------|
| `domain/classifier/species_classifier.dart` | the interface + `AudioSample` input |
| `data/classifier/mock_species_classifier.dart` | **live today** — plausible fake result |
| `data/classifier/tflite_species_classifier.dart` | skeleton with drop-in steps |

Landing the real model: implement `TfliteSpeciesClassifier` (decode WAV →
mel-spectrogram → CNN → softmax → top-2 → map to `Species`), then swap one line
in `Bootstrap.create()`. No UI or domain changes.

### Backend seam — `SyncService`

The app is fully functional offline; sync is additive. Bound to
`NoopSyncService` by default so **nothing** depends on Firebase. Implement
`FirebaseSyncService` against the interface and override the provider to enable
cross-device sync, aggregate maps, and model updates.

---

## 4. Capture data flow

The core interaction — press-and-hold to identify a mosquito and save it —
routed entirely through interfaces and the offline database.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant RS as RecordScreen
    participant RC as RecordController
    participant Rec as AudioRecorderService
    participant Cls as SpeciesClassifier
    participant Loc as LocationService
    participant Repo as DetectionRepository
    participant DB as sqflite

    User->>RS: press & hold
    RS->>RC: startHold()
    RC->>Rec: hasPermission / start()
    Note over RC: phase = listening<br/>Stopwatch ticker → progress 0..1
    User->>RS: release (or 4s elapse)
    alt released early
        RC->>Rec: cancel()
        RC-->>RS: phase = idle
    else 4s complete
        RC->>Rec: stop() → AudioSample
        Note over RC: phase = analyzing
        RC->>Cls: classify(sample)
        Cls-->>RC: ClassificationResult (primary, runner, conf, Hz)
        RC-->>RS: phase = result
        RS-->>User: species · severity · confidence · runner-up
        User->>RS: Save to log
        RS->>RC: save()
        RC->>Loc: currentFix() (best-effort GPS)
        RC->>Repo: add(Detection)
        Repo->>DB: insert
        Repo-->>RS: stream emits → History updates
    end
```

Key properties:
- **Progress** is a `Stopwatch` + 16 ms ticker; releasing before 4 s cancels the recording and returns to idle.
- **GPS is best-effort** — a denied/failed fix still saves the detection (offline-first).
- **History is reactive** — `LocalDetectionRepository` emits on a broadcast stream, so the list/map/stats refresh the instant a detection is saved.

---

## 5. State & persistence

- **`RecordController`** (`Notifier<RecordState>`) — the capture state machine: `idle → listening → analyzing → result`. Owns recorder/classifier/location/repo interactions and the save path.
- **`settingsProvider`** — language, theme, accent, voice, background, onboarding. Live: theme/accent/locale changes rebuild `MaterialApp`. Persisted to a key/value table.
- **`detectionsProvider`** (stream) → **`filteredDetectionsProvider`** (species + date filter) and **`statsProvider`** (`computeStats` over the full log).
- **Storage** — one `sqflite` database: `detections` (the offline source of truth, with real timestamps + GPS) and `settings` (key/value). `AppDatabase.seedDemoData` seeds demo rows on first install.

```mermaid
flowchart LR
    subgraph db["sqflite (mozzid.db)"]
        det[("detections<br/>species · conf · Hz<br/>timestamp · lat/lng")]
        set[("settings<br/>key / value")]
    end
    dao["DetectionDao"] --> det
    store["SettingsStore"] --> set
    ldr["LocalDetectionRepository<br/>(cache + broadcast stream)"] --> dao
    ldr -->|watch| dsp["detectionsProvider"]
    dsp --> fil["filteredDetectionsProvider"]
    dsp --> sta["statsProvider"]
    store --> setp["settingsProvider"]
```

---

## 6. Theming as design tokens

The full palette is a `MozzColors` `ThemeExtension` derived from
`(Brightness, AppAccent)`. Widgets read colour via `context.c`, fonts via
`MozzType`, strings via `context.l`. Switching theme or accent in Settings
rebuilds the theme and recolours the app live — so UI must always pull from
those accessors, never hardcode a colour, font, or string.

Severity is **colourblind-safe by contract**: every level pairs a glyph shape
(▲ / ● / ■) with a text label; colour only reinforces, never carries meaning
alone.

---

## 7. Module map

```
lib/
├── core/theme/          MozzColors, AppAccent, typography, dimens
├── core/l10n_ext.dart   context.l accessor
├── domain/
│   ├── models/          Species, Detection, ClassificationResult, Severity
│   ├── classifier/      SpeciesClassifier interface  ← ML SEAM
│   ├── repositories/    DetectionRepository, SpeciesRepository
│   ├── services/        AudioRecorderService, LocationService
│   └── stats/           computeStats, applyFilters  ← unit-tested
├── data/
│   ├── classifier/      MockSpeciesClassifier, TfliteSpeciesClassifier
│   ├── local/           AppDatabase, DetectionDao, SettingsStore
│   ├── detection/       LocalDetectionRepository
│   ├── species/         SpeciesCatalog
│   ├── audio/ location/ notifications/ voice/ export/   adapters
│   └── sync/            SyncService  ← BACKEND SEAM
├── presentation/
│   ├── providers/       bootstrap, settings, detections, record_controller
│   ├── screens/         onboarding, home_shell, record, result, history, settings
│   └── widgets/         mascot, rings, spectrogram, map, sheets
├── l10n/                app_en.arb, app_id.arb
├── app.dart             MaterialApp: live theme + locale, onboarding gate
└── main.dart            Bootstrap + single provider override
```
