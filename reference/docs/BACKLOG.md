# MozzID — Backlog & Performance Notes

Status of what's built, what's not, and where performance/caching actually
matters. Grounded in the current code, not aspirational.

---

## Done

- All screens: onboarding, record → listening → analyzing → result, history (map + stats + filters), species card, morning summary, settings.
- Real mic capture (`record`), real GPS (`geolocator`), offline SQLite persistence (`sqflite`) as the source of truth.
- i18n EN/ID (ARB), live theme + accent token system, colourblind-safe severity.
- CSV export, local notification (fire on high-severity save + preview), TTS voice output, share result as image.
- `SpeciesClassifier` seam with working mock; `SyncService` seam (Noop); TFLite skeleton.
- Unit tests on stats / filters / detection model. `flutter analyze` clean.

---

## Not done yet

### ML (deferred by design)
- [ ] Real wingbeat model — `TfliteSpeciesClassifier` throws `UnimplementedError`. Decode WAV → mel-spectrogram → CNN → top-2. (Model + dataset out of scope.)
- [ ] The recorded audio is captured but not analyzed — the mock ignores it. Wire real feature extraction when the model lands.

### Mobile features (real gaps, not just stubs)
- [ ] **Background / passive listening** — Settings has the toggle and the morning summary reads the real log, but **nothing actually listens overnight**. Needs a foreground service + `workmanager`/periodic wake + battery-aware capture. Today it's UI only.
- [ ] **Scheduled notifications** — only an on-save high-activity alert + a preview button exist. The dawn/dusk peak-hour reminders need `zonedSchedule` + timezone init (`flutter_native_timezone` / `timezone`).
- [ ] **Location labels** — real saved detections have `locationLabel == null` (show "—"); only seeded demo rows are labelled. Add reverse geocoding (`geocoding`) or a manual label field.
- [ ] **Detection detail screen** — tapping a log row opens the species sheet, not a per-detection view (its own map point, exact time, confidence, delete). 
- [ ] **Delete / clear log UI** — repository has `remove`/`clear`, but no UI (swipe-to-delete, clear-all in settings).
- [ ] **Camera secondary ID path** — brief stretch goal, not started.
- [ ] **Firebase sync** — seam only (`NoopSyncService`).
- [ ] **Real map tiles** — stylised offline map only; `flutter_map` + `TileLayer` drop-in documented but not wired.

### Polish / hardening
- [ ] **Permissions** — onboarding requests mic/location but doesn't gate on the result or deep-link to settings if permanently denied; mid-flow denial only shows a snackbar.
- [ ] **Accessibility** — severity has semantics; still need a full screen-reader pass on record/result, large-text layout testing, focus order.
- [ ] **App icon + splash + `applicationId`** — still the default Flutter icon and `com.example.mozzid`.
- [ ] **Release signing** — debug keys only.
- [ ] **Widget / integration tests** — only pure-logic unit tests exist (the default `widget_test.dart` was removed). Add a record-flow widget test with fakes (override `bootstrapProvider`) and a golden or two.

---

## Performance & caching

**Is it laggy now? No.** With demo-scale data (single-digit detections) the
stats/filter passes are O(n) and trivial, the DB repo already caches rows in
memory, and Riverpod memoises provider results until their inputs change. No
data-caching layer is needed at this size. The items below are ordered by
actual payoff, and most are about **rendering**, not caching.

### Do these (cheap, real wins)
1. **Bundle the fonts** — the one genuine caching gap. `google_fonts` fetches
   Spectral / IBM Plex over the network on first launch and falls back to the
   system font until then, which causes a first-run text reflow (and breaks the
   offline-first promise on a fresh install). Ship the TTFs under `assets/fonts`
   and point `MozzType` at them. Removes the fetch, the reflow, and the network
   dependency.
2. **`RepaintBoundary` around the always-animating widgets** — the mascot
   (`CustomPaint`, ~60 fps wing flap), the analyzing spectrogram (32 bars
   rebuilt each tick), and the map pins each run a controller. Wrapping these
   subtrees in `RepaintBoundary` stops them from marking ancestors dirty and
   keeps the animation cost isolated. Near-zero effort, measurable on lower-end
   phones.

### Do these as the log grows (scaling, not needed at demo scale)
3. **Cap / cluster map pins** — every visible pin is its own
   `StatefulWidget` + `AnimationController`. Fine for a handful; hundreds of
   located detections = hundreds of controllers. Render only the most-recent N,
   or cluster by proximity, and only animate the newly-added ones.
4. **Paginate the DB** — `LocalDetectionRepository` loads the *entire* log into
   memory on first read. There's already a `timestamp` index; switch to
   `LIMIT`/offset (or keyset) queries and lazy-load history once the log is
   large.
5. **Stagger-delay via `Interval`, not `Future.delayed`** — pin entrance uses a
   per-pin `Future.delayed` before `forward()`. With many pins, prefer a single
   shared controller with `Interval` curves to avoid N timers.

### Explicitly NOT worth doing now
- A memo cache over `computeStats` / `applyFilters` — Riverpod already caches
  these; recompute only happens when the log or filter changes. Premature.
- An image cache — species images are placeholders; add `cached_network_image`
  or asset bundling only when real images land.

---

## Suggested next order

1. Bundle fonts + add `RepaintBoundary` (perf, quick).
2. Background listening service (the biggest missing product surface).
3. Detection detail + delete/clear UI (rounds out the data story).
4. Scheduled peak-hour notifications + location labels.
5. Icon/splash/signing for a real installable build.
6. Then the ML model and Firebase, per the seams already in place.
