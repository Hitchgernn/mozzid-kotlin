import 'package:flutter/material.dart';

import 'accent.dart';

/// The full design-token colour set, exposed as a [ThemeExtension] so widgets
/// read `Theme.of(context).extension<MozzColors>()!` and recolour live when the
/// brightness or accent changes. Mirrors the CSS custom properties in the
/// original design (`--bg`, `--surface`, `--text`, `--line`, `--accent`, …).
@immutable
class MozzColors extends ThemeExtension<MozzColors> {
  const MozzColors({
    required this.bg,
    required this.surface,
    required this.surface2,
    required this.surface3,
    required this.text,
    required this.text2,
    required this.text3,
    required this.text4,
    required this.faint,
    required this.line,
    required this.line2,
    required this.fill,
    required this.accent,
    required this.accent2,
    required this.accentHi,
    required this.accentInk,
    required this.accentSoftText,
  });

  final Color bg;
  final Color surface;
  final Color surface2;
  final Color surface3;
  final Color text;
  final Color text2;
  final Color text3;
  final Color text4;
  final Color faint;
  final Color line;
  final Color line2;
  final Color fill;
  final Color accent;
  final Color accent2;
  final Color accentHi;
  final Color accentInk;
  final Color accentSoftText;

  factory MozzColors.of(Brightness brightness, AppAccent accent) {
    final dark = brightness == Brightness.dark;
    return MozzColors(
      bg: dark ? const Color(0xFF080B11) : const Color(0xFFE7EBF1),
      surface: dark ? const Color(0xFF0F141D) : const Color(0xFFFFFFFF),
      surface2: dark ? const Color(0xFF0D121A) : const Color(0xFFEEF2F7),
      surface3: dark ? const Color(0xFF0C1119) : const Color(0xFFFFFFFF),
      text: dark ? const Color(0xFFEEF3F9) : const Color(0xFF0F1720),
      text2: dark ? const Color(0xFFC6D0DD) : const Color(0xFF33414F),
      text3: dark ? const Color(0xFF93A1B3) : const Color(0xFF5A6675),
      text4: dark ? const Color(0xFF6B7788) : const Color(0xFF8592A1),
      faint: dark ? const Color(0xFF4A5567) : const Color(0xFFAEB8C4),
      line: dark ? const Color(0x0FFFFFFF) : const Color(0x1A101823),
      line2: dark ? const Color(0x1FFFFFFF) : const Color(0x2E101823),
      fill: dark ? const Color(0x0AFFFFFF) : const Color(0x0D101823),
      accent: accent.color,
      accent2: accent.deep,
      accentHi: accent.bright,
      accentInk: accent.ink,
      accentSoftText: dark ? accent.bright : accent.deep,
    );
  }

  /// A translucent tint of the accent, matching the design's frequent
  /// `color-mix(in srgb, var(--accent) N%, transparent)`.
  Color accentMix(double percent) => accent.withValues(alpha: percent / 100);

  @override
  MozzColors copyWith({
    Color? bg,
    Color? surface,
    Color? surface2,
    Color? surface3,
    Color? text,
    Color? text2,
    Color? text3,
    Color? text4,
    Color? faint,
    Color? line,
    Color? line2,
    Color? fill,
    Color? accent,
    Color? accent2,
    Color? accentHi,
    Color? accentInk,
    Color? accentSoftText,
  }) {
    return MozzColors(
      bg: bg ?? this.bg,
      surface: surface ?? this.surface,
      surface2: surface2 ?? this.surface2,
      surface3: surface3 ?? this.surface3,
      text: text ?? this.text,
      text2: text2 ?? this.text2,
      text3: text3 ?? this.text3,
      text4: text4 ?? this.text4,
      faint: faint ?? this.faint,
      line: line ?? this.line,
      line2: line2 ?? this.line2,
      fill: fill ?? this.fill,
      accent: accent ?? this.accent,
      accent2: accent2 ?? this.accent2,
      accentHi: accentHi ?? this.accentHi,
      accentInk: accentInk ?? this.accentInk,
      accentSoftText: accentSoftText ?? this.accentSoftText,
    );
  }

  @override
  MozzColors lerp(covariant MozzColors? other, double t) {
    if (other == null) return this;
    return MozzColors(
      bg: Color.lerp(bg, other.bg, t)!,
      surface: Color.lerp(surface, other.surface, t)!,
      surface2: Color.lerp(surface2, other.surface2, t)!,
      surface3: Color.lerp(surface3, other.surface3, t)!,
      text: Color.lerp(text, other.text, t)!,
      text2: Color.lerp(text2, other.text2, t)!,
      text3: Color.lerp(text3, other.text3, t)!,
      text4: Color.lerp(text4, other.text4, t)!,
      faint: Color.lerp(faint, other.faint, t)!,
      line: Color.lerp(line, other.line, t)!,
      line2: Color.lerp(line2, other.line2, t)!,
      fill: Color.lerp(fill, other.fill, t)!,
      accent: Color.lerp(accent, other.accent, t)!,
      accent2: Color.lerp(accent2, other.accent2, t)!,
      accentHi: Color.lerp(accentHi, other.accentHi, t)!,
      accentInk: Color.lerp(accentInk, other.accentInk, t)!,
      accentSoftText: Color.lerp(accentSoftText, other.accentSoftText, t)!,
    );
  }
}
