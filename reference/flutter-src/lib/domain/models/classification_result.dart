import 'species.dart';

/// Output of a [SpeciesClassifier] run over one wingbeat sample: the best match
/// plus the runner-up and the measured frequency. Pure data — no persistence.
class ClassificationResult {
  const ClassificationResult({
    required this.primary,
    required this.runner,
    required this.confidence,
    required this.runnerConfidence,
    required this.wingbeatHz,
  });

  final Species primary;
  final Species runner;

  /// Primary confidence, 0–100.
  final int confidence;

  /// Runner-up confidence, 0–100.
  final int runnerConfidence;

  /// Measured wingbeat fundamental in Hz (e.g. 612).
  final int wingbeatHz;
}
