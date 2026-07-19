import '../models/classification_result.dart';

/// One captured wingbeat sample handed to the classifier.
///
/// Today this is just the recorded audio file path plus its duration; when the
/// real model lands it will also carry the decoded PCM / mel-spectrogram. Keep
/// this the single input type so the ML swap does not ripple outward.
class AudioSample {
  const AudioSample({
    required this.filePath,
    required this.duration,
    this.sampleRate = 44100,
  });

  final String filePath;
  final Duration duration;
  final int sampleRate;
}

/// ┌──────────────────────────────────────────────────────────────────────┐
/// │  ML SEAM — SpeciesClassifier                                          │
/// │                                                                        │
/// │  The ONLY boundary between the app and the wingbeat model. Everything  │
/// │  upstream (record → analyze → result → save) talks to this interface,  │
/// │  never to a concrete model.                                            │
/// │                                                                        │
/// │  • Now:   MockSpeciesClassifier returns a plausible fake result.       │
/// │  • Later: TfliteSpeciesClassifier decodes audio → mel-spectrogram →    │
/// │           CNN (TFLite) → ClassificationResult. Drop it in and swap the │
/// │           provider override in main.dart; no UI changes required.      │
/// └──────────────────────────────────────────────────────────────────────┘
abstract interface class SpeciesClassifier {
  /// Load model weights / labels. Cheap and idempotent for the mock.
  Future<void> load();

  /// Classify a single wingbeat sample. Runs fully on-device.
  Future<ClassificationResult> classify(AudioSample sample);

  /// Release native resources (interpreter, buffers).
  Future<void> dispose();
}
