import 'dart:math';

import '../../domain/classifier/species_classifier.dart';
import '../../domain/models/classification_result.dart';
import '../../domain/repositories/species_repository.dart';

/// ┌──────────────────────────────────────────────────────────────────────┐
/// │  STUB — MockSpeciesClassifier                                         │
/// │                                                                        │
/// │  Stands in for the real wingbeat model until the TFLite CNN lands.     │
/// │  It ignores the audio content and returns a plausible random result   │
/// │  (primary + runner-up + a wingbeat frequency near the species range),  │
/// │  after a short delay that mimics on-device inference.                  │
/// │                                                                        │
/// │  Replace with TfliteSpeciesClassifier — see                            │
/// │  data/classifier/tflite_species_classifier.dart. No caller changes.    │
/// └──────────────────────────────────────────────────────────────────────┘
class MockSpeciesClassifier implements SpeciesClassifier {
  MockSpeciesClassifier(this._species, {Random? random})
      : _random = random ?? Random();

  final SpeciesRepository _species;
  final Random _random;

  @override
  Future<void> load() async {
    // Nothing to load for the mock. The real model would read weights + labels.
  }

  @override
  Future<ClassificationResult> classify(AudioSample sample) async {
    // Simulate inference latency (~1.7s, matching the design's analyze state).
    await Future<void>.delayed(const Duration(milliseconds: 1700));

    final ids = _species.classifiableIds;
    final primaryId = ids[_random.nextInt(ids.length)];
    var runnerId = ids[_random.nextInt(ids.length)];
    while (runnerId == primaryId) {
      runnerId = ids[_random.nextInt(ids.length)];
    }

    final primary = _species.byId(primaryId)!;
    final runner = _species.byId(runnerId)!;

    final confidence = 78 + _random.nextInt(17); // 78–94
    final runnerConfidence =
        max(3, 100 - confidence - _random.nextInt(6));

    return ClassificationResult(
      primary: primary,
      runner: runner,
      confidence: confidence,
      runnerConfidence: runnerConfidence,
      wingbeatHz: _freqFor(primaryId),
    );
  }

  /// A believable measured frequency inside each species' known range.
  int _freqFor(String id) {
    switch (id) {
      case 'aedes':
        return 450 + _random.nextInt(251); // 450–700
      case 'culex':
        return 300 + _random.nextInt(151); // 300–450
      case 'anopheles':
        return 400 + _random.nextInt(201); // 400–600
      default:
        return 400 + _random.nextInt(300);
    }
  }

  @override
  Future<void> dispose() async {}
}
