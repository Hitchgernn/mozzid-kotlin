import '../models/detection.dart';

/// Date-range filter for the history log.
enum DateRange { all, week }

/// The two history filters combined. `speciesId == null` (or `'all'`) means all
/// species.
class LogFilter {
  const LogFilter({this.speciesId, this.range = DateRange.all});

  final String? speciesId;
  final DateRange range;

  bool get isAllSpecies => speciesId == null || speciesId == 'all';

  LogFilter copyWith({String? speciesId, DateRange? range}) => LogFilter(
        speciesId: speciesId ?? this.speciesId,
        range: range ?? this.range,
      );
}

/// Apply [filter] to [detections]. Pure and deterministic — [now] is injected
/// so the "this week" window is testable. Order is preserved.
List<Detection> applyFilters(
  List<Detection> detections,
  LogFilter filter, {
  required DateTime now,
}) {
  final cutoff = now.subtract(const Duration(days: 7));
  return detections.where((d) {
    final speciesOk = filter.isAllSpecies || d.speciesId == filter.speciesId;
    final rangeOk =
        filter.range == DateRange.all || d.timestamp.isAfter(cutoff);
    return speciesOk && rangeOk;
  }).toList();
}
