import 'package:flutter/material.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../../domain/models/severity.dart';

/// Localized human label for a [Severity] level.
String severityLabel(BuildContext context, Severity s) {
  switch (s) {
    case Severity.high:
      return context.l.highRisk;
    case Severity.moderate:
      return context.l.moderateRisk;
    case Severity.low:
      return context.l.lowRisk;
  }
}

/// The disease-risk banner. Colourblind-safe: a glyph shape (▲/●/■) plus a text
/// label carry the meaning; colour only reinforces it.
class SeverityBanner extends StatelessWidget {
  const SeverityBanner({
    super.key,
    required this.severity,
    required this.diseases,
  });

  final Severity severity;
  final String diseases;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Semantics(
      label: '${severityLabel(context, severity)}. ${context.l.carries} $diseases',
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        decoration: BoxDecoration(
          color: severity.bg,
          borderRadius: BorderRadius.circular(18),
          border: Border.all(color: severity.border),
        ),
        child: Row(
          children: [
            Container(
              width: 40,
              height: 40,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: severity.iconBg,
                borderRadius: BorderRadius.circular(11),
              ),
              child: Text(
                severity.glyph,
                style: MozzType.mono(
                  size: 19,
                  weight: FontWeight.w700,
                  color: severity.color,
                ),
              ),
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    severityLabel(context, severity).toUpperCase(),
                    style: MozzType.sans(
                      size: 11,
                      weight: FontWeight.w700,
                      color: severity.color,
                      letterSpacing: 1,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '${context.l.carries} $diseases',
                    style: MozzType.sans(
                      size: 15,
                      weight: FontWeight.w600,
                      color: c.text,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
