import 'package:flutter/material.dart';

/// The four brand accents from the design. The accent swaps live and recolours
/// the whole app via [MozzColors]; nothing else in the palette changes.
enum AppAccent {
  teal(0xFF2DD4BF, 0xFF12A695, 0xFF34E6D1, 0xFF04211D),
  lime(0xFF84CC16, 0xFF4D7C0F, 0xFFBEF264, 0xFF152400),
  amber(0xFFF5A623, 0xFFB45309, 0xFFFBBF24, 0xFF2A1600),
  indigo(0xFF8B93F8, 0xFF4F46E5, 0xFFA5B4FC, 0xFF0A0F2A);

  const AppAccent(this._base, this._deep, this._bright, this._ink);

  final int _base;
  final int _deep;
  final int _bright;
  final int _ink;

  /// Primary accent colour (`--accent`).
  Color get color => Color(_base);

  /// Darker accent used for gradient ends (`--accent2`).
  Color get deep => Color(_deep);

  /// Brighter highlight (`--accent-hi`).
  Color get bright => Color(_bright);

  /// Ink colour that reads on top of the accent (`--accent-ink`).
  Color get ink => Color(_ink);

  static AppAccent fromName(String? name) =>
      AppAccent.values.firstWhere((a) => a.name == name, orElse: () => teal);
}
