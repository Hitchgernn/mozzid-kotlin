import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../providers/record_controller.dart';
import '../widgets/confidence_ring.dart';
import '../widgets/spectrogram.dart';
import 'result_view.dart';

class RecordScreen extends ConsumerWidget {
  const RecordScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(recordControllerProvider);

    // Surface mic/record errors, then clear.
    ref.listen(recordControllerProvider.select((s) => s.error), (_, error) {
      if (error == RecordError.none) return;
      final msg = error == RecordError.micDenied
          ? context.l.micDenied
          : context.l.recordingFailed;
      ScaffoldMessenger.of(context)
        ..hideCurrentSnackBar()
        ..showSnackBar(SnackBar(content: Text(msg)));
      ref.read(recordControllerProvider.notifier).clearError();
    });

    if (state.phase == RecordPhase.result) {
      return const ResultView();
    }
    return _RecordBody(state: state);
  }
}

class _RecordBody extends ConsumerWidget {
  const _RecordBody({required this.state});
  final RecordState state;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.c;
    final l = context.l;
    final idle = state.phase == RecordPhase.idle;
    final listening = state.phase == RecordPhase.listening;

    return SafeArea(
      child: Padding(
        padding: const EdgeInsets.fromLTRB(24, 16, 24, 8),
        child: Column(
          children: [
            Align(
              alignment: Alignment.center,
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
                decoration: BoxDecoration(
                  color: c.accentMix(9),
                  borderRadius: BorderRadius.circular(999),
                  border: Border.all(color: c.accentMix(22)),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      width: 7,
                      height: 7,
                      decoration: BoxDecoration(color: c.accent, shape: BoxShape.circle),
                    ),
                    const SizedBox(width: 7),
                    Text(l.offline,
                        style: MozzType.sans(size: 12, weight: FontWeight.w600, color: c.accentSoftText)),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 28),
            Text(l.heardBuzz,
                textAlign: TextAlign.center,
                style: MozzType.serif(size: 27, color: c.text, height: 1.22)),
            const SizedBox(height: 10),
            Text(l.tapHold,
                textAlign: TextAlign.center,
                style: MozzType.sans(size: 14.5, color: c.text3, height: 1.5)),
            Expanded(
              child: Center(child: _recordArea(context, ref)),
            ),
            Text(
              idle
                  ? l.holdSteady
                  : listening
                      ? l.listeningHold
                      : l.onDevice,
              textAlign: TextAlign.center,
              style: idle
                  ? MozzType.mono(size: 12.5, color: c.text4)
                  : MozzType.sans(size: 13.5, weight: FontWeight.w500, color: c.accentSoftText),
            ),
          ],
        ),
      ),
    );
  }

  Widget _recordArea(BuildContext context, WidgetRef ref) {
    final controller = ref.read(recordControllerProvider.notifier);
    switch (state.phase) {
      case RecordPhase.idle:
        return _IdleButton(onStart: controller.startHold);
      case RecordPhase.listening:
        return _ListeningButton(progress: state.progress, onEnd: controller.endHold);
      case RecordPhase.analyzing:
        return _AnalyzingView();
      case RecordPhase.result:
        return const SizedBox.shrink();
    }
  }
}

class _IdleButton extends StatelessWidget {
  const _IdleButton({required this.onStart});
  final VoidCallback onStart;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Listener(
      onPointerDown: (_) => onStart(),
      child: Container(
        width: 264,
        height: 264,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          shape: BoxShape.circle,
          border: Border.all(color: c.accentMix(14)),
        ),
        child: Container(
          width: 184,
          height: 184,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            gradient: RadialGradient(
              center: const Alignment(0, -0.24),
              colors: [c.accentHi, c.accent2],
            ),
            boxShadow: [
              BoxShadow(color: c.accent.withValues(alpha: 0.5), blurRadius: 60, spreadRadius: -18, offset: const Offset(0, 20)),
            ],
          ),
          child: Icon(Icons.mic_rounded, size: 52, color: c.accentInk),
        ),
      ),
    );
  }
}

class _ListeningButton extends StatelessWidget {
  const _ListeningButton({required this.progress, required this.onEnd});
  final double progress;
  final VoidCallback onEnd;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Listener(
      onPointerUp: (_) => onEnd(),
      onPointerCancel: (_) => onEnd(),
      child: SizedBox(
        width: 264,
        height: 264,
        child: Stack(
          alignment: Alignment.center,
          children: [
            ConfidenceRing(percent: progress * 100, size: 264, stroke: 5, glow: true),
            Container(
              width: 184,
              height: 184,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: RadialGradient(
                  center: const Alignment(0, -0.24),
                  colors: [c.accentHi, c.accent2],
                ),
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(Icons.graphic_eq_rounded, size: 44, color: c.accentInk),
                  const SizedBox(height: 10),
                  Text('${(progress * 100).round()}%',
                      style: MozzType.mono(size: 13, weight: FontWeight.w600, color: c.accentInk)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AnalyzingView extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        const Spectrogram(),
        const SizedBox(height: 26),
        Text(l.matching, style: MozzType.mono(size: 14, color: c.accentSoftText)),
        const SizedBox(height: 8),
        Text(l.onDevice, style: MozzType.sans(size: 12.5, color: c.text4)),
      ],
    );
  }
}
