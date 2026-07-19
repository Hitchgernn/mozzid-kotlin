import '../models/detection.dart';

/// Per-species share of the log, used for the breakdown bar + legend.
class SpeciesShare {
  const SpeciesShare({
    required this.speciesId,
    required this.count,
    required this.percent,
  });

  final String speciesId;
  final int count;

  /// Whole-number percent of the total, 0–100.
  final int percent;
}

/// Aggregate stats over a detection log. Pure value object; computed by
/// [computeStats].
class DetectionStats {
  const DetectionStats({
    required this.total,
    required this.breakdown,
    required this.peakWindow,
  });

  final int total;

  /// Species shares, highest count first.
  final List<SpeciesShare> breakdown;

  /// Most-active 2-hour window label, e.g. `10PM–12AM`, or null when empty.
  final String? peakWindow;

  static const DetectionStats empty =
      DetectionStats(total: 0, breakdown: [], peakWindow: null);
}

/// Compute [DetectionStats] from a log. Deterministic and side-effect free so
/// it can be unit-tested directly.
DetectionStats computeStats(List<Detection> log) {
  if (log.isEmpty) return DetectionStats.empty;

  final counts = <String, int>{};
  final buckets = List<int>.filled(12, 0); // 12 × 2-hour windows

  for (final d in log) {
    counts.update(d.speciesId, (v) => v + 1, ifAbsent: () => 1);
    buckets[d.timestamp.hour ~/ 2] += 1;
  }

  final total = log.length;
  final breakdown = counts.entries
      .map((e) => SpeciesShare(
            speciesId: e.key,
            count: e.value,
            percent: ((e.value / total) * 100).round(),
          ))
      .toList()
    ..sort((a, b) => b.count.compareTo(a.count));

  var peakBucket = 0;
  for (var i = 1; i < buckets.length; i++) {
    if (buckets[i] > buckets[peakBucket]) peakBucket = i;
  }

  return DetectionStats(
    total: total,
    breakdown: breakdown,
    peakWindow: formatWindow(peakBucket * 2),
  );
}

/// Format a 2-hour window starting at [startHour] (0–22, even) as `10PM–12AM`.
String formatWindow(int startHour) {
  final end = (startHour + 2) % 24;
  return '${_hour12(startHour)}–${_hour12(end)}';
}

String _hour12(int h) {
  final period = h < 12 ? 'AM' : 'PM';
  final base = h % 12 == 0 ? 12 : h % 12;
  return '$base$period';
}
