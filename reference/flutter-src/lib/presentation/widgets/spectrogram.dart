import 'dart:math';

import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// The analyzing-state spectrogram: animated frequency bars with a sweeping
/// scan line, signalling on-device signal processing.
class Spectrogram extends StatefulWidget {
  const Spectrogram({super.key, this.bars = 32});

  final int bars;

  @override
  State<Spectrogram> createState() => _SpectrogramState();
}

class _SpectrogramState extends State<Spectrogram>
    with SingleTickerProviderStateMixin {
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 900),
  )..repeat(reverse: true);

  late final List<double> _heights = List.generate(
    widget.bars,
    (i) => 0.15 + 0.6 * sin(i * 0.6).abs() + Random().nextDouble() * 0.2,
  );

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return RepaintBoundary(
      child: Container(
      width: 280,
      height: 120,
      padding: const EdgeInsets.all(10),
      clipBehavior: Clip.hardEdge,
      decoration: BoxDecoration(
        color: c.surface3,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: c.accentMix(14)),
      ),
      child: AnimatedBuilder(
        animation: _c,
        builder: (_, __) => Row(
          crossAxisAlignment: CrossAxisAlignment.end,
          children: [
            for (var i = 0; i < widget.bars; i++)
              Expanded(
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 1),
                  child: FractionallySizedBox(
                    heightFactor: (_heights[i] *
                            (0.55 + 0.45 * (((_c.value + i / widget.bars) % 1))))
                        .clamp(0.1, 1.0),
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        gradient: LinearGradient(
                          begin: Alignment.topCenter,
                          end: Alignment.bottomCenter,
                          colors: [c.accent, c.accent2],
                        ),
                        borderRadius: BorderRadius.circular(1),
                      ),
                    ),
                  ),
                ),
              ),
          ],
        ),
      ),
      ),
    );
  }
}
