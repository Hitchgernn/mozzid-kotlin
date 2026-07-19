import 'dart:io';
import 'dart:ui' as ui;

import 'package:flutter/material.dart';
import 'package:flutter/rendering.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../../domain/models/classification_result.dart';
import '../../domain/models/species.dart';
import '../providers/bootstrap.dart';
import '../providers/record_controller.dart';
import '../providers/settings_provider.dart';
import '../widgets/confidence_ring.dart';
import '../widgets/severity_banner.dart';
import '../widgets/species_sheet.dart';

class ResultView extends ConsumerStatefulWidget {
  const ResultView({super.key});

  @override
  ConsumerState<ResultView> createState() => _ResultViewState();
}

class _ResultViewState extends ConsumerState<ResultView> {
  final _cardKey = GlobalKey();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _maybeSpeak());
  }

  void _maybeSpeak() {
    final settings = ref.read(settingsProvider);
    final result = ref.read(recordControllerProvider).result;
    if (!settings.voiceOutput || result == null || !mounted) return;
    final l = context.l;
    ref.read(ttsServiceProvider).speak(
          '${result.primary.scientificName}. ${l.carries} ${result.primary.diseases}. '
          '${result.confidence} ${l.confidence}.',
          localeCode: settings.languageCode,
        );
  }

  Future<void> _save() async {
    await ref.read(recordControllerProvider.notifier).save();
    if (!mounted) return;
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(SnackBar(content: Text(context.l.savedToLog)));
  }

  Future<void> _share() async {
    try {
      final boundary = _cardKey.currentContext?.findRenderObject()
          as RenderRepaintBoundary?;
      if (boundary == null) return;
      final image = await boundary.toImage(pixelRatio: 3);
      final bytes = await image.toByteData(format: ui.ImageByteFormat.png);
      if (bytes == null) return;
      final dir = await getTemporaryDirectory();
      final file = await File('${dir.path}/mozzid_result.png')
          .writeAsBytes(bytes.buffer.asUint8List());
      await Share.shareXFiles([XFile(file.path)]);
    } catch (_) {/* sharing is best-effort */}
  }

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    final result = ref.watch(recordControllerProvider).result;
    if (result == null) return const SizedBox.shrink();
    final voice = ref.watch(settingsProvider).voiceOutput;

    return SafeArea(
      child: SingleChildScrollView(
        padding: const EdgeInsets.fromLTRB(22, 8, 22, 24),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                TextButton.icon(
                  onPressed: () =>
                      ref.read(recordControllerProvider.notifier).retry(),
                  icon: Icon(Icons.chevron_left_rounded, color: c.text2, size: 20),
                  label: Text(l.recordAgain,
                      style: MozzType.sans(size: 13, color: c.text2)),
                  style: TextButton.styleFrom(
                    backgroundColor: c.fill,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                      side: BorderSide(color: c.line2),
                    ),
                  ),
                ),
                if (voice)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
                    decoration: BoxDecoration(
                      color: c.accentMix(10),
                      borderRadius: BorderRadius.circular(999),
                      border: Border.all(color: c.accentMix(22)),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(Icons.volume_up_rounded, size: 15, color: c.accent),
                        const SizedBox(width: 7),
                        Text(l.speaking,
                            style: MozzType.sans(size: 11.5, weight: FontWeight.w600, color: c.accentSoftText)),
                      ],
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 16),
            RepaintBoundary(
              key: _cardKey,
              child: Container(
                color: c.bg,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    SeverityBanner(
                      severity: result.primary.severity,
                      diseases: result.primary.diseases,
                    ),
                    const SizedBox(height: 20),
                    _SpeciesHeader(result: result, onTap: () => _openSpecies(result.primary)),
                    const SizedBox(height: 20),
                    _Metrics(result: result),
                    const SizedBox(height: 12),
                    _TimeCrossCheck(species: result.primary),
                    const SizedBox(height: 16),
                    Text(result.primary.note,
                        style: MozzType.sans(size: 13.5, color: c.text2, height: 1.55)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),
            Text(l.couldAlso.toUpperCase(),
                style: MozzType.sans(size: 11, weight: FontWeight.w600, color: c.text4, letterSpacing: 1)),
            const SizedBox(height: 8),
            _RunnerUp(result: result, onTap: () => _openSpecies(result.runner)),
            const SizedBox(height: 22),
            Row(
              children: [
                SizedBox(
                  width: 56,
                  height: 54,
                  child: OutlinedButton(
                    onPressed: _share,
                    style: OutlinedButton.styleFrom(
                      backgroundColor: c.fill,
                      side: BorderSide(color: c.line2),
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                      padding: EdgeInsets.zero,
                    ),
                    child: Icon(Icons.ios_share_rounded, color: c.text2, size: 20),
                  ),
                ),
                const SizedBox(width: 11),
                Expanded(
                  child: SizedBox(
                    height: 54,
                    child: FilledButton.icon(
                      onPressed: _save,
                      style: FilledButton.styleFrom(
                        backgroundColor: c.accent,
                        foregroundColor: c.accentInk,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(15)),
                      ),
                      icon: Icon(Icons.save_alt_rounded, size: 18, color: c.accentInk),
                      label: Text(l.saveLog,
                          style: MozzType.sans(size: 15.5, weight: FontWeight.w700, color: c.accentInk)),
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  void _openSpecies(Species s) =>
      showSpeciesSheet(context, s);
}

class _SpeciesHeader extends StatelessWidget {
  const _SpeciesHeader({required this.result, required this.onTap});
  final ClassificationResult result;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    return Row(
      children: [
        Container(
          width: 88,
          height: 88,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: c.surface2,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: c.line),
          ),
          child: Text('photo', style: MozzType.mono(size: 9, color: c.faint)),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(l.identifiedAs.toUpperCase(),
                  style: MozzType.sans(size: 11, weight: FontWeight.w600, color: c.text4, letterSpacing: 1)),
              const SizedBox(height: 3),
              GestureDetector(
                onTap: onTap,
                child: Text(result.primary.scientificName,
                    style: MozzType.serif(size: 26, color: c.text, style: FontStyle.italic, height: 1.1)),
              ),
              const SizedBox(height: 4),
              Text(result.primary.commonName,
                  style: MozzType.sans(size: 13, color: c.text3)),
            ],
          ),
        ),
      ],
    );
  }
}

class _Metrics extends StatelessWidget {
  const _Metrics({required this.result});
  final ClassificationResult result;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    // IntrinsicHeight bounds the row's height so the two cards can stretch to
    // equal height inside the scroll view (plain stretch would force infinite
    // height here and break layout + hit-testing).
    return IntrinsicHeight(
      child: Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: c.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: c.line),
            ),
            child: Row(
              children: [
                ConfidenceRing(percent: result.confidence.toDouble(), size: 58),
                const SizedBox(width: 14),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('${result.confidence}%',
                        style: MozzType.mono(size: 22, weight: FontWeight.w600, color: c.text)),
                    Text(l.confidence, style: MozzType.sans(size: 11, color: c.text4)),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: c.surface,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: c.line),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text('~${result.wingbeatHz} Hz',
                    style: MozzType.mono(size: 22, weight: FontWeight.w600, color: c.text)),
                Text(l.wingbeat, style: MozzType.sans(size: 11, color: c.text4)),
              ],
            ),
          ),
        ),
      ],
      ),
    );
  }
}

class _TimeCrossCheck extends StatelessWidget {
  const _TimeCrossCheck({required this.species});
  final Species species;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    final hour = DateTime.now().hour;
    final isNight = hour >= 19 || hour < 6;
    final consistent = !(species.activeWindow.isDayBiter && isNight);
    final color = consistent ? c.accent : const Color(0xFFFFCF6B);
    final title = consistent ? l.consistentHours : l.unusualHour;
    final note = consistent
        ? '${species.activeLabel} — ${species.commonName}'
        : '${species.scientificName.split(' ').first}: ${species.activeLabel}';

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      decoration: BoxDecoration(
        color: consistent ? c.accentMix(7) : const Color(0x14FFCF6B),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: consistent ? c.accentMix(20) : const Color(0x3DFFCF6B)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(Icons.schedule_rounded, color: color, size: 20),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: MozzType.sans(size: 13, weight: FontWeight.w600, color: c.text)),
                const SizedBox(height: 2),
                Text(note, style: MozzType.sans(size: 12.5, color: c.text3, height: 1.45)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _RunnerUp extends StatelessWidget {
  const _RunnerUp({required this.result, required this.onTap});
  final ClassificationResult result;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: c.surface2,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: c.line),
        ),
        child: Row(
          children: [
            Expanded(
              child: Text(result.runner.scientificName,
                  style: MozzType.serif(size: 15, color: c.text2, style: FontStyle.italic)),
            ),
            Text('${result.runnerConfidence}%',
                style: MozzType.mono(size: 13, color: c.text3)),
            const SizedBox(width: 10),
            SizedBox(
              width: 60,
              height: 5,
              child: ClipRRect(
                borderRadius: BorderRadius.circular(3),
                child: LinearProgressIndicator(
                  value: result.runnerConfidence / 100,
                  backgroundColor: c.line2,
                  valueColor: AlwaysStoppedAnimation(c.text4),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
