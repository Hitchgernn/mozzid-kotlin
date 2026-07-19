import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../../domain/models/detection.dart';
import '../../domain/repositories/species_repository.dart';
import '../providers/bootstrap.dart';
import '../providers/detection_providers.dart';
import 'mozz_mascot.dart';

Future<void> showMorningSummary(BuildContext context) {
  return showModalBottomSheet<void>(
    context: context,
    isScrollControlled: true,
    backgroundColor: Colors.transparent,
    builder: (_) => const MorningSummarySheet(),
  );
}

/// Overnight passive-listening recap: totals per species + a night timeline
/// (10PM–6AM), computed from the real log.
class MorningSummarySheet extends ConsumerWidget {
  const MorningSummarySheet({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.c;
    final l = context.l;
    final species = ref.watch(speciesRepositoryProvider);
    final all = ref.watch(detectionsProvider).valueOrNull ?? const [];
    final overnight = _overnight(all);
    final counts = <String, int>{};
    for (final d in overnight) {
      counts.update(d.speciesId, (v) => v + 1, ifAbsent: () => 1);
    }

    return Container(
      constraints: BoxConstraints(
        maxHeight: MediaQuery.of(context).size.height * 0.9,
      ),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [c.surface3, c.bg],
        ),
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 30),
        child: Column(
          children: [
            const MozzMascot(size: 96),
            const SizedBox(height: 14),
            Text(l.goodMorning,
                style: MozzType.mono(size: 12, color: c.accent, letterSpacing: 1)),
            const SizedBox(height: 8),
            Text(
              l.detectedOvernight(overnight.length),
              textAlign: TextAlign.center,
              style: MozzType.serif(size: 25, color: c.text, height: 1.3),
            ),
            const SizedBox(height: 24),
            Row(
              children: [
                for (final e in counts.entries)
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 5),
                      child: _CountTile(
                        count: e.value,
                        label: species.byId(e.key)?.scientificName.split(' ').first ?? e.key,
                        color: species.byId(e.key)?.dotColor ?? c.accent,
                      ),
                    ),
                  ),
                if (counts.isEmpty)
                  Expanded(
                    child: _CountTile(count: 0, label: '—', color: c.text4),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            _Timeline(detections: overnight, species: species),
            const SizedBox(height: 20),
            SizedBox(
              width: double.infinity,
              height: 52,
              child: FilledButton(
                onPressed: () => Navigator.of(context).pop(),
                style: FilledButton.styleFrom(
                  backgroundColor: c.accent,
                  foregroundColor: c.accentInk,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                ),
                child: Text(l.gotIt,
                    style: MozzType.sans(size: 15, weight: FontWeight.w700, color: c.accentInk)),
              ),
            ),
          ],
        ),
      ),
    );
  }

  /// Detections in the most recent night window (roughly 6PM → 9AM).
  List<Detection> _overnight(List<Detection> all) {
    final now = DateTime.now();
    final since = now.subtract(const Duration(hours: 24));
    return all.where((d) {
      if (d.timestamp.isBefore(since)) return false;
      final h = d.timestamp.hour;
      return h >= 18 || h < 9;
    }).toList();
  }
}

class _CountTile extends StatelessWidget {
  const _CountTile({required this.count, required this.label, required this.color});
  final int count;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Container(
      padding: const EdgeInsets.all(15),
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: c.line),
      ),
      child: Column(
        children: [
          Text('$count', style: MozzType.mono(size: 26, weight: FontWeight.w600, color: color)),
          const SizedBox(height: 4),
          Text(label, style: MozzType.serif(size: 11.5, color: c.text3, style: FontStyle.italic)),
        ],
      ),
    );
  }
}

class _Timeline extends StatelessWidget {
  const _Timeline({required this.detections, required this.species});
  final List<Detection> detections;
  final SpeciesRepository species;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    final repo = species;
    // 12 buckets across 10PM..6AM → 8h in 40-min steps for a fuller bar row.
    final buckets = List<Color?>.filled(12, null);
    final heights = List<double>.filled(12, 0.06);
    for (final d in detections) {
      final h = d.timestamp.hour;
      final slot = ((h - 22 + 24) % 24);
      if (slot < 12) {
        final idx = slot;
        heights[idx] = 0.4 + 0.5 * (heights[idx]);
        buckets[idx] = repo.byId(d.speciesId)?.dotColor;
      }
    }
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: c.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(l.overnightTimeline.toUpperCase(),
              style: MozzType.sans(size: 11, weight: FontWeight.w600, color: c.text4, letterSpacing: 1)),
          const SizedBox(height: 14),
          SizedBox(
            height: 60,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                for (var i = 0; i < 12; i++)
                  Expanded(
                    child: Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 2.5),
                      child: FractionallySizedBox(
                        heightFactor: buckets[i] == null ? 0.06 : heights[i].clamp(0.1, 1.0),
                        child: DecoratedBox(
                          decoration: BoxDecoration(
                            color: buckets[i] ?? c.text4.withValues(alpha: 0.15),
                            borderRadius: const BorderRadius.vertical(top: Radius.circular(3)),
                          ),
                        ),
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text('10PM', style: MozzType.mono(size: 10, color: c.text4)),
              Text('2AM', style: MozzType.mono(size: 10, color: c.text4)),
              Text('6AM', style: MozzType.mono(size: 10, color: c.text4)),
            ],
          ),
        ],
      ),
    );
  }
}
