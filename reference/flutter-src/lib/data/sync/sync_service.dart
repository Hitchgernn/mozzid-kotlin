import '../../domain/models/detection.dart';

/// ┌──────────────────────────────────────────────────────────────────────┐
/// │  BACKEND SEAM — SyncService (optional, Firebase)                      │
/// │                                                                        │
/// │  The app is fully functional offline; sync is additive. This interface │
/// │  is the only place the app talks to a backend. The default binding is  │
/// │  NoopSyncService (does nothing), so nothing depends on Firebase.       │
/// │                                                                        │
/// │  To enable cross-device sync + aggregate maps + model updates:         │
/// │   1. Add firebase_core + cloud_firestore, run flutterfire configure.   │
/// │   2. Implement FirebaseSyncService against this interface (push local  │
/// │      detections, pull aggregates).                                     │
/// │   3. Override the provider in main.dart. No UI/domain changes.         │
/// └──────────────────────────────────────────────────────────────────────┘
abstract interface class SyncService {
  bool get isEnabled;

  /// Push newly-saved detections upstream (best-effort). Never blocks the UI.
  Future<void> pushDetection(Detection detection);

  /// Pull remote aggregates for the map. Returns empty when disabled.
  Future<void> pullAggregates();
}

/// Default no-op binding — keeps the app 100% offline unless Firebase is wired.
class NoopSyncService implements SyncService {
  const NoopSyncService();

  @override
  bool get isEnabled => false;

  @override
  Future<void> pushDetection(Detection detection) async {}

  @override
  Future<void> pullAggregates() async {}
}
