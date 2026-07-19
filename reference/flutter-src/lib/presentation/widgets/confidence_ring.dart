import 'dart:math';

import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// A circular progress ring (0–100%) used for capture progress and the result
/// confidence score. The design's signature frequency-ring motif.
class ConfidenceRing extends StatelessWidget {
  const ConfidenceRing({
    super.key,
    required this.percent,
    this.size = 58,
    this.stroke = 5,
    this.glow = false,
    this.child,
  });

  final double percent; // 0..100
  final double size;
  final double stroke;
  final bool glow;
  final Widget? child;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return SizedBox(
      width: size,
      height: size,
      child: CustomPaint(
        painter: _RingPainter(
          percent: percent.clamp(0, 100) / 100,
          track: c.line,
          accent: c.accent,
          stroke: stroke,
          glow: glow,
        ),
        child: child == null ? null : Center(child: child),
      ),
    );
  }
}

class _RingPainter extends CustomPainter {
  _RingPainter({
    required this.percent,
    required this.track,
    required this.accent,
    required this.stroke,
    required this.glow,
  });

  final double percent;
  final Color track;
  final Color accent;
  final double stroke;
  final bool glow;

  @override
  void paint(Canvas canvas, Size size) {
    final center = size.center(Offset.zero);
    final radius = (size.width - stroke) / 2;
    final rect = Rect.fromCircle(center: center, radius: radius);

    canvas.drawCircle(
      center,
      radius,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = stroke
        ..color = track,
    );

    final arc = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke
      ..strokeCap = StrokeCap.round
      ..color = accent;
    if (glow) arc.maskFilter = const MaskFilter.blur(BlurStyle.normal, 3);
    canvas.drawArc(rect, -pi / 2, 2 * pi * percent, false, arc);
  }

  @override
  bool shouldRepaint(_RingPainter old) =>
      old.percent != percent || old.accent != accent;
}
