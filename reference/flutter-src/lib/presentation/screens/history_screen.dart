import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../../domain/models/detection.dart';
import '../../domain/models/species.dart';
import '../../domain/repositories/species_repository.dart';
import '../../domain/stats/detection_stats.dart';
import '../../domain/stats/log_filters.dart';
import '../providers/bootstrap.dart';
import '../providers/detection_providers.dart';
import '../widgets/mozz_mascot.dart';
import '../widgets/species_sheet.dart';
import '../widgets/stylized_map.dart';

class HistoryScreen extends ConsumerWidget {
  const HistoryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.c;
    final l = context.l;
    final all = ref.watch(detectionsProvider).valueOrNull ?? const [];
    final filtered = ref.watch(filteredDetectionsProvider);
    final stats = ref.watch(statsProvider);
    final filter = ref.watch(logFilterProvider);
    final species = ref.watch(speciesRepositoryProvider);

    return SafeArea(
      child: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.fromLTRB(22, 8, 22, 4),
            sliver: SliverToBoxAdapter(
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(l.history, style: MozzType.serif(size: 26, color: c.text)),
                  Text('${stats.total} ${l.logged}',
                      style: MozzType.mono(size: 12.5, color: c.text4)),
                ],
              ),
            ),
          ),
          if (all.isEmpty)
            SliverFillRemaining(hasScrollBody: false, child: _EmptyState())
          else ...[
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(22, 12, 22, 0),
                child: StylizedMap(
                  detections: filtered,
                  species: species,
                  animateKey: '${filter.speciesId ?? 'all'}-${filter.range.name}',
                ),
              ),
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(22, 14, 22, 0),
                child: _StatsCard(stats: stats, species: species),
              ),
            ),
            SliverToBoxAdapter(
              child: _Filters(filter: filter, ref: ref, species: species),
            ),
            SliverPadding(
              padding: const EdgeInsets.fromLTRB(22, 14, 22, 24),
              sliver: SliverList.separated(
                itemCount: filtered.length,
                separatorBuilder: (_, __) => const SizedBox(height: 9),
                itemBuilder: (context, i) =>
                    _LogRow(detection: filtered[i], species: species),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _EmptyState extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    return Padding(
      padding: const EdgeInsets.fromLTRB(40, 40, 40, 40),
      child: Column(
        mainAxisAlignment: MainAxisAlignment.center,
        children: [
          const MozzMascot(size: 84),
          const SizedBox(height: 18),
          Text(l.emptyTitle, style: MozzType.serif(size: 20, color: c.text2)),
          const SizedBox(height: 8),
          Text(l.emptyBody,
              textAlign: TextAlign.center,
              style: MozzType.sans(size: 13.5, color: c.text3, height: 1.5)),
        ],
      ),
    );
  }
}

class _StatsCard extends StatelessWidget {
  const _StatsCard({required this.stats, required this.species});
  final DetectionStats stats;
  final SpeciesRepository species;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: c.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('${stats.total}',
                      style: MozzType.mono(size: 28, weight: FontWeight.w600, color: c.text)),
                  const SizedBox(height: 4),
                  Text(l.totalDetections, style: MozzType.sans(size: 11.5, color: c.text4)),
                ],
              ),
              if (stats.peakWindow != null)
                Column(
                  crossAxisAlignment: CrossAxisAlignment.end,
                  children: [
                    Text(stats.peakWindow!,
                        style: MozzType.sans(size: 13, weight: FontWeight.w600, color: c.accent)),
                    const SizedBox(height: 3),
                    Text(l.mostActive, style: MozzType.sans(size: 11, color: c.text4)),
                  ],
                ),
            ],
          ),
          const SizedBox(height: 14),
          ClipRRect(
            borderRadius: BorderRadius.circular(6),
            child: SizedBox(
              height: 9,
              child: Row(
                children: [
                  for (final b in stats.breakdown)
                    Expanded(
                      flex: b.percent.clamp(1, 100),
                      child: Container(color: species.byId(b.speciesId)?.dotColor ?? c.accent),
                    ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 10),
          Wrap(
            spacing: 12,
            runSpacing: 6,
            children: [
              for (final b in stats.breakdown)
                Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 9,
                      height: 9,
                      decoration: BoxDecoration(
                        color: species.byId(b.speciesId)?.dotColor ?? c.accent,
                        borderRadius: BorderRadius.circular(3),
                      ),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      '${species.byId(b.speciesId)?.scientificName.split(' ').first ?? b.speciesId} ',
                      style: MozzType.sans(size: 12, color: c.text2),
                    ),
                    Text('${b.percent}%', style: MozzType.sans(size: 12, color: c.text4)),
                  ],
                ),
            ],
          ),
        ],
      ),
    );
  }
}

class _Filters extends StatelessWidget {
  const _Filters({required this.filter, required this.ref, required this.species});
  final LogFilter filter;
  final WidgetRef ref;
  final SpeciesRepository species;

  @override
  Widget build(BuildContext context) {
    final l = context.l;
    final notifier = ref.read(logFilterProvider.notifier);
    final chips = <(String?, String)>[
      (null, l.allSpecies),
      for (final s in species.all())
        (s.id, s.scientificName.split(' ').first),
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(top: 16),
          child: SizedBox(
            height: 36,
            child: ListView(
              scrollDirection: Axis.horizontal,
              padding: const EdgeInsets.symmetric(horizontal: 22),
              children: [
                for (final chip in chips)
                  Padding(
                    padding: const EdgeInsets.only(right: 8),
                    child: _Chip(
                      label: chip.$2,
                      selected: filter.speciesId == chip.$1 ||
                          (filter.isAllSpecies && chip.$1 == null),
                      onTap: () => notifier.setSpecies(chip.$1),
                    ),
                  ),
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(22, 9, 22, 0),
          child: Row(
            children: [
              _Chip(
                label: l.allTime,
                mono: true,
                selected: filter.range == DateRange.all,
                onTap: () => notifier.setRange(DateRange.all),
              ),
              const SizedBox(width: 8),
              _Chip(
                label: l.thisWeek,
                mono: true,
                selected: filter.range == DateRange.week,
                onTap: () => notifier.setRange(DateRange.week),
              ),
            ],
          ),
        ),
      ],
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({
    required this.label,
    required this.selected,
    required this.onTap,
    this.mono = false,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;
  final bool mono;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 8),
        decoration: BoxDecoration(
          color: selected ? c.accentMix(14) : c.fill,
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: selected ? c.accentMix(30) : c.line),
        ),
        child: Text(
          label,
          style: mono
              ? MozzType.mono(size: 12.5, weight: FontWeight.w500, color: selected ? c.accentSoftText : c.text3)
              : MozzType.sans(size: 13, weight: FontWeight.w500, color: selected ? c.accentSoftText : c.text3),
        ),
      ),
    );
  }
}

class _LogRow extends StatelessWidget {
  const _LogRow({required this.detection, required this.species});
  final Detection detection;
  final SpeciesRepository species;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final Species? s = species.byId(detection.speciesId);
    final time = DateFormat.jm().format(detection.timestamp);
    final date = DateFormat.MMMd().format(detection.timestamp);
    return GestureDetector(
      onTap: s == null ? null : () => showSpeciesSheet(context, s),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
        decoration: BoxDecoration(
          color: c.surface2,
          borderRadius: BorderRadius.circular(15),
          border: Border.all(color: c.line),
        ),
        child: Row(
          children: [
            Container(
              width: 11,
              height: 11,
              decoration: BoxDecoration(
                color: s?.dotColor ?? c.accent,
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 13),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(s?.scientificName ?? detection.speciesId,
                      style: MozzType.serif(size: 15.5, color: c.text, style: FontStyle.italic)),
                  const SizedBox(height: 2),
                  Row(
                    children: [
                      Text(time, style: MozzType.sans(size: 11.5, color: c.text4)),
                      Text('  •  ', style: MozzType.sans(size: 11.5, color: c.text4)),
                      Text(detection.locationLabel ?? '—',
                          style: MozzType.sans(size: 11.5, color: c.text4)),
                    ],
                  ),
                ],
              ),
            ),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text('${detection.confidence}%', style: MozzType.mono(size: 13, color: c.text2)),
                const SizedBox(height: 2),
                Text(date, style: MozzType.sans(size: 10.5, color: c.text4)),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
