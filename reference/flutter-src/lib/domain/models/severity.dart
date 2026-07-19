import 'package:flutter/material.dart';

/// Disease-risk severity. Colourblind-safe by design: every level pairs a
/// distinct **glyph shape** and a text label with its colour, so the meaning
/// never rides on hue alone.
enum Severity {
  high('▲', Color(0xFFFF8A7A)),
  moderate('●', Color(0xFFFFCF6B)),
  low('■', Color(0xFF7FD0FF));

  const Severity(this.glyph, this.color);

  /// Shape marker shown alongside the label (triangle / circle / square).
  final String glyph;

  /// Accent colour for the banner. Never the sole signal — always with glyph.
  final Color color;

  Color get bg => color.withValues(alpha: 0.10);
  Color get border => color.withValues(alpha: 0.28);
  Color get iconBg => color.withValues(alpha: 0.16);

  static Severity fromName(String name) =>
      Severity.values.firstWhere((s) => s.name == name, orElse: () => low);
}
