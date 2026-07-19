import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/models/classification_result.dart';
import '../../domain/models/detection.dart';
import '../../domain/services/location_service.dart';
import 'bootstrap.dart';
import 'settings_provider.dart';

enum RecordPhase { idle, listening, analyzing, result }

enum RecordError { none, micDenied, failed }

@immutable
class RecordState {
  const RecordState({
    this.phase = RecordPhase.idle,
    this.progress = 0,
    this.result,
    this.error = RecordError.none,
  });

  final RecordPhase phase;
  final double progress; // 0..1 during listening
  final ClassificationResult? result;
  final RecordError error;

  RecordState copyWith({
    RecordPhase? phase,
    double? progress,
    ClassificationResult? result,
    RecordError? error,
  }) =>
      RecordState(
        phase: phase ?? this.phase,
        progress: progress ?? this.progress,
        result: result ?? this.result,
        error: error ?? this.error,
      );
}

/// Drives the capture flow: idle → listening (hold ~4s) → analyzing → result.
/// Talks only to the injected services and the [SpeciesClassifier] interface.
class RecordController extends Notifier<RecordState> {
  static const captureDuration = Duration(milliseconds: 4000);

  Timer? _ticker;
  Stopwatch? _watch;
  bool _saving = false;

  @override
  RecordState build() {
    ref.onDispose(_stopTicker);
    return const RecordState();
  }

  Future<void> startHold() async {
    if (state.phase != RecordPhase.idle) return;
    final recorder = ref.read(audioRecorderProvider);

    if (!await recorder.hasPermission()) {
      final granted = await recorder.requestPermission();
      if (!granted) {
        state = state.copyWith(error: RecordError.micDenied);
        return;
      }
    }

    try {
      await recorder.start();
    } catch (_) {
      state = state.copyWith(error: RecordError.failed);
      return;
    }

    state = const RecordState(phase: RecordPhase.listening, progress: 0);
    _watch = Stopwatch()..start();
    _ticker = Timer.periodic(const Duration(milliseconds: 16), (_) {
      final elapsed = _watch!.elapsedMilliseconds;
      final p = (elapsed / captureDuration.inMilliseconds).clamp(0.0, 1.0);
      state = state.copyWith(progress: p);
      if (p >= 1.0) _finishListening();
    });
  }

  Future<void> endHold() async {
    if (state.phase != RecordPhase.listening) return;
    if (state.progress >= 1.0) return; // already completing
    _stopTicker();
    await ref.read(audioRecorderProvider).cancel();
    state = const RecordState(phase: RecordPhase.idle);
  }

  Future<void> _finishListening() async {
    _stopTicker();
    state = state.copyWith(phase: RecordPhase.analyzing);
    final recorder = ref.read(audioRecorderProvider);
    final classifier = ref.read(classifierProvider);
    try {
      final sample = await recorder.stop();
      final result = await classifier.classify(sample);
      state = RecordState(phase: RecordPhase.result, result: result);
    } catch (_) {
      state = const RecordState(phase: RecordPhase.idle, error: RecordError.failed);
    }
  }

  void retry() {
    _stopTicker();
    state = const RecordState();
  }

  void clearError() => state = state.copyWith(error: RecordError.none);

  /// Persist the current result with a real timestamp + best-effort GPS fix.
  /// Returns the saved detection, or null if there was nothing to save.
  Future<Detection?> save() async {
    final result = state.result;
    if (result == null || _saving) return null;
    _saving = true;
    try {
      final location = ref.read(locationServiceProvider);
      final GeoFix fix = await location.currentFix();
      final detection = Detection(
        speciesId: result.primary.id,
        confidence: result.confidence,
        wingbeatHz: result.wingbeatHz,
        timestamp: DateTime.now(),
        latitude: fix.latitude,
        longitude: fix.longitude,
        locationLabel: fix.label,
      );
      final saved = await ref.read(detectionRepositoryProvider).add(detection);

      // Fire a local high-activity alert for high-risk vectors.
      final settings = ref.read(settingsProvider);
      if (result.primary.severity.name == 'high') {
        unawaited(ref.read(syncServiceProvider).pushDetection(saved));
      }
      if (result.primary.severity.name == 'high' && settings.backgroundListening) {
        final notif = ref.read(notificationServiceProvider);
        unawaited(notif.showActivityAlert(
          'High dengue-vector activity',
          '${result.primary.scientificName} logged near you.',
        ));
      }

      state = const RecordState();
      return saved;
    } finally {
      _saving = false;
    }
  }

  void _stopTicker() {
    _ticker?.cancel();
    _ticker = null;
    _watch?.stop();
    _watch = null;
  }
}

final recordControllerProvider =
    NotifierProvider<RecordController, RecordState>(RecordController.new);
