import '../models/detection.dart';

/// Persistence boundary for detections. Implemented over SQLite; the domain and
/// presentation layers never see the database.
abstract interface class DetectionRepository {
  Future<List<Detection>> all();
  Future<Detection> add(Detection detection);
  Future<void> remove(int id);
  Future<void> clear();

  /// Live stream so the history screen updates when a detection is saved.
  Stream<List<Detection>> watch();
}
