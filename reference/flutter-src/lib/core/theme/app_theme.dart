import 'package:flutter/material.dart';

import 'accent.dart';
import 'mozz_colors.dart';
import 'typography.dart';

/// Builds a [ThemeData] for a given [brightness] + [AppAccent]. The full design
/// token set rides along as a [MozzColors] extension. Changing either input
/// rebuilds the theme and recolours the app live.
class AppTheme {
  const AppTheme._();

  static ThemeData build(Brightness brightness, AppAccent accent) {
    final colors = MozzColors.of(brightness, accent);
    final scheme = ColorScheme.fromSeed(
      seedColor: accent.color,
      brightness: brightness,
    ).copyWith(
      surface: colors.bg,
      primary: colors.accent,
      onPrimary: colors.accentInk,
    );

    return ThemeData(
      useMaterial3: true,
      brightness: brightness,
      colorScheme: scheme,
      scaffoldBackgroundColor: colors.bg,
      canvasColor: colors.bg,
      textTheme: MozzType.textTheme(colors.text),
      splashFactory: NoSplash.splashFactory,
      extensions: <ThemeExtension<dynamic>>[colors],
    );
  }
}

extension MozzColorsX on BuildContext {
  MozzColors get c => Theme.of(this).extension<MozzColors>()!;
}
