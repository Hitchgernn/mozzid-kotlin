import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../domain/models/detection.dart';
import '../../domain/stats/detection_stats.dart';
import '../../domain/stats/log_filters.dart';
import 'bootstrap.dart';

/// Live detection log from the repository.
final detectionsProvider = StreamProvider<List<Detection>>(
  (ref) => ref.watch(detectionRepositoryProvider).watch(),
);

/// Current history filter (species + date range).
class LogFilterNotifier extends Notifier<LogFilter> {
  @override
  LogFilter build() => const LogFilter();

  void setSpecies(String? id) => state = state.copyWith(speciesId: id);
  void setRange(DateRange range) => state = state.copyWith(range: range);
}

final logFilterProvider =
    NotifierProvider<LogFilterNotifier, LogFilter>(LogFilterNotifier.new);

/// Detections after the active filter is applied (list view + map pins).
final filteredDetectionsProvider = Provider<List<Detection>>((ref) {
  final all = ref.watch(detectionsProvider).valueOrNull ?? const [];
  final filter = ref.watch(logFilterProvider);
  return applyFilters(all, filter, now: DateTime.now());
});

/// Aggregate stats over the *full* log (not the filtered view), per design.
final statsProvider = Provider<DetectionStats>((ref) {
  final all = ref.watch(detectionsProvider).valueOrNull ?? const [];
  return computeStats(all);
});
