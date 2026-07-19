import '../../domain/classifier/species_classifier.dart';
import '../../domain/models/classification_result.dart';

/// ┌──────────────────────────────────────────────────────────────────────┐
/// │  ML DROP-IN (deferred) — TfliteSpeciesClassifier                      │
/// │                                                                        │
/// │  Skeleton for the real on-device model. Left unimplemented on purpose  │
/// │  — the wingbeat CNN + its training set are out of scope for now. When  │
/// │  ready:                                                                │
/// │                                                                        │
/// │   1. Add `tflite_flutter` (+ `tflite_flutter_helper`) to pubspec.      │
/// │   2. Bundle assets/model/wingbeat_cnn.tflite and labels.txt.           │
/// │   3. In load(): Interpreter.fromAsset(...).                            │
/// │   4. In classify(): decode WAV → resample → mel-spectrogram → run the  │
/// │      interpreter → softmax → top-2 → map label ids to Species via the  │
/// │      SpeciesRepository → ClassificationResult.                         │
/// │   5. Swap the provider override in main.dart from Mock… to this class.  │
/// │                                                                        │
/// │  Nothing upstream changes: the record/analyze/result/save flow only    │
/// │  knows the SpeciesClassifier interface.                                │
/// └──────────────────────────────────────────────────────────────────────┘
class TfliteSpeciesClassifier implements SpeciesClassifier {
  @override
  Future<void> load() async {
    throw UnimplementedError(
      'TFLite wingbeat model not wired yet — using MockSpeciesClassifier. '
      'See class docs for the drop-in steps.',
    );
  }

  @override
  Future<ClassificationResult> classify(AudioSample sample) {
    throw UnimplementedError('TFLite model not wired yet.');
  }

  @override
  Future<void> dispose() async {}
}
