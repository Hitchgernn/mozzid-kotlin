import 'package:flutter/material.dart';

import 'severity.dart';

/// When a species is typically biting. Used for the result screen's
/// active-time cross-check ("detected at an unusual hour").
enum ActiveWindow {
  day, // daytime + dawn/dusk biter (e.g. Aedes)
  night, // strictly night
  duskToDawn; // dusk through dawn (e.g. Anopheles)

  bool get isDayBiter => this == ActiveWindow.day;
}

/// A mosquito species reference record. Static, on-device knowledge — no
/// network. `id` is the stable key stored on every [Detection].
@immutable
class Species {
  const Species({
    required this.id,
    required this.scientificName,
    required this.commonName,
    required this.diseases,
    required this.wingbeatHz,
    required this.wingbeatRange,
    required this.severity,
    required this.activeWindow,
    required this.activeLabel,
    required this.note,
    required this.tips,
    required this.dotColor,
  });

  final String id;
  final String scientificName;
  final String commonName;
  final String diseases;

  /// Representative wingbeat, e.g. `~600 Hz`.
  final String wingbeatHz;

  /// Full range, e.g. `450–700 Hz`.
  final String wingbeatRange;
  final Severity severity;
  final ActiveWindow activeWindow;
  final String activeLabel; // human label, e.g. "Day · dawn/dusk"
  final String note;
  final List<String> tips;

  /// Map-pin / list colour, matching the design's per-species dot.
  final Color dotColor;
}
