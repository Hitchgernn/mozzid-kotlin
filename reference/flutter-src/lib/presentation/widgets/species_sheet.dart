import 'package:flutter/material.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../../domain/models/species.dart';
import 'severity_banner.dart';

/// Opens the species reference card as a bottom sheet.
Future<void> showSpeciesSheet(BuildContext context, Species species) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (_) => SpeciesSheet(species: species),
  );
}

class SpeciesSheet extends StatelessWidget {
  const SpeciesSheet({super.key, required this.species});
  final Species species;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.88,
      ),
      decoration: BoxDecoration(
        color: c.surface3,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
        border: Border.all(color: c.line2),
      ),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(22, 12, 22, 30),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Center(
              child: Container(
                width: 44,
                height: 5,
                decoration: BoxDecoration(
                  color: c.line2,
                  borderRadius: BorderRadius.circular(3),
                ),
              ),
            ),
            const SizedBox(height: 16),
            Container(
              height: 150,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: c.surface2,
                borderRadius: BorderRadius.circular(20),
                border: Border.all(color: c.line),
              ),
              child: Text('${species.scientificName} — photo',
                  style: MozzType.mono(size: 11, color: c.faint)),
            ),
            const SizedBox(height: 16),
            Text(species.scientificName,
                style: MozzType.serif(size: 27, color: c.text, style: FontStyle.italic)),
            const SizedBox(height: 2),
            Text(species.commonName, style: MozzType.sans(size: 13.5, color: c.text3)),
            const SizedBox(height: 18),
            Row(
              children: [
                Expanded(
                  child: _InfoTile(
                    label: l.wingbeat,
                    value: species.wingbeatRange,
                    valueColor: c.accent,
                    mono: true,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: _InfoTile(label: l.activeHours, value: species.activeLabel),
                ),
              ],
            ),
            const SizedBox(height: 10),
            SeverityBanner(severity: species.severity, diseases: species.diseases),
            const SizedBox(height: 20),
            Text(l.prevention.toUpperCase(),
                style: MozzType.sans(size: 11, weight: FontWeight.w600, color: c.text4, letterSpacing: 1)),
            const SizedBox(height: 9),
            for (final tip in species.tips)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Container(
                      margin: const EdgeInsets.only(top: 7, right: 11),
                      width: 6,
                      height: 6,
                      decoration: BoxDecoration(color: c.accent, shape: BoxShape.circle),
                    ),
                    Expanded(
                      child: Text(tip, style: MozzType.sans(size: 13.5, color: c.text2, height: 1.5)),
                    ),
                  ],
                ),
              ),
            const SizedBox(height: 14),
            SizedBox(
              height: 50,
              child: OutlinedButton(
                onPressed: () => Navigator.of(context).pop(),
                style: OutlinedButton.styleFrom(
                  backgroundColor: c.fill,
                  side: BorderSide(color: c.line2),
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
                ),
                child: Text(l.close,
                    style: MozzType.sans(size: 15, weight: FontWeight.w600, color: c.text2)),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _InfoTile extends StatelessWidget {
  const _InfoTile({
    required this.label,
    required this.value,
    this.valueColor,
    this.mono = false,
  });

  final String label;
  final String value;
  final Color? valueColor;
  final bool mono;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: c.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label.toUpperCase(),
              style: MozzType.sans(size: 10.5, weight: FontWeight.w600, color: c.text4, letterSpacing: 0.5)),
          const SizedBox(height: 5),
          Text(
            value,
            style: mono
                ? MozzType.mono(size: 17, color: valueColor ?? c.text)
                : MozzType.sans(size: 14, weight: FontWeight.w600, color: valueColor ?? c.text),
          ),
        ],
      ),
    );
  }
}
