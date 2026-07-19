import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

/// The three design typefaces. Serif display = Spectral, UI = IBM Plex Sans,
/// numerics/labels = IBM Plex Mono. Centralised so a bundled-font swap is a
/// one-file change (see README → offline fonts).
class MozzType {
  const MozzType._();

  static TextStyle serif({
    double? size,
    FontWeight weight = FontWeight.w600,
    Color? color,
    FontStyle style = FontStyle.normal,
    double? height,
  }) =>
      GoogleFonts.spectral(
        fontSize: size,
        fontWeight: weight,
        fontStyle: style,
        color: color,
        height: height,
      );

  static TextStyle sans({
    double? size,
    FontWeight weight = FontWeight.w400,
    Color? color,
    double? height,
    double? letterSpacing,
  }) =>
      GoogleFonts.ibmPlexSans(
        fontSize: size,
        fontWeight: weight,
        color: color,
        height: height,
        letterSpacing: letterSpacing,
      );

  static TextStyle mono({
    double? size,
    FontWeight weight = FontWeight.w500,
    Color? color,
    double? letterSpacing,
  }) =>
      GoogleFonts.ibmPlexMono(
        fontSize: size,
        fontWeight: weight,
        color: color,
        letterSpacing: letterSpacing,
      );

  static TextTheme textTheme(Color base) => GoogleFonts.ibmPlexSansTextTheme(
        ThemeData(brightness: Brightness.dark).textTheme,
      ).apply(bodyColor: base, displayColor: base);
}
