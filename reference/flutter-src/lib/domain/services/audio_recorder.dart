import '../classifier/species_classifier.dart';

/// Abstraction over the microphone so the record flow can be driven with a fake
/// in tests. The concrete implementation wraps the `record` plugin.
abstract interface class AudioRecorderService {
  /// Whether the mic permission is currently granted.
  Future<bool> hasPermission();

  /// Request mic permission; returns the resulting grant state.
  Future<bool> requestPermission();

  /// Begin recording to a temp file. Throws if permission is missing.
  Future<void> start();

  /// Stop and return the captured sample (path + duration).
  Future<AudioSample> stop();

  /// Abort an in-progress recording, discarding the file.
  Future<void> cancel();

  Future<void> dispose();
}
