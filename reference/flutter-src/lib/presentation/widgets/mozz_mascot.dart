import 'package:flutter/material.dart';

import '../../core/theme/app_theme.dart';

/// The MozzID mosquito mascot, with gently flapping wings. Rendered from the
/// same geometry as the design's SVG, scaled to [size]. Accent-tinted.
class MozzMascot extends StatefulWidget {
  const MozzMascot({super.key, this.size = 112, this.float = true});

  final double size;
  final bool float;

  @override
  State<MozzMascot> createState() => _MozzMascotState();
}

class _MozzMascotState extends State<MozzMascot>
    with TickerProviderStateMixin {
  late final AnimationController _wing = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 500),
  )..repeat(reverse: true);
  late final AnimationController _bob = AnimationController(
    vsync: this,
    duration: const Duration(seconds: 4),
  )..repeat(reverse: true);

  @override
  void dispose() {
    _wing.dispose();
    _bob.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    // RepaintBoundary isolates the ~60fps wing repaint from the rest of the UI.
    final Widget painter = RepaintBoundary(
      child: AnimatedBuilder(
        animation: _wing,
        builder: (_, __) => CustomPaint(
          size: Size.square(widget.size),
          painter: _MascotPainter(
            wing: _wing.value,
            accent: c.accent,
            accent2: c.accent2,
            accentHi: c.accentHi,
            ink: c.accentInk,
          ),
        ),
      ),
    );
    if (!widget.float) return painter;
    return AnimatedBuilder(
      animation: _bob,
      builder: (_, child) => Transform.translate(
        offset: Offset(0, -9 * (_bob.value - 0.5) * 2),
        child: Transform.rotate(angle: (_bob.value - 0.5) * 0.06, child: child),
      ),
      child: painter,
    );
  }
}

class _MascotPainter extends CustomPainter {
  _MascotPainter({
    required this.wing,
    required this.accent,
    required this.accent2,
    required this.accentHi,
    required this.ink,
  });

  final double wing; // 0..1 flap
  final Color accent;
  final Color accent2;
  final Color accentHi;
  final Color ink;

  @override
  void paint(Canvas canvas, Size size) {
    final s = size.width / 120.0;
    Offset pt(double x, double y) => Offset(x * s, y * s);
    // Body shades derived from the accent so the whole mascot recolours:
    // deep accent for limbs, mid for abdomen, base for thorax, bright for head.
    final limb = accent2;
    final abdomen = Color.lerp(accent2, accent, 0.5)!;
    final legPaint = Paint()
      ..color = limb
      ..strokeWidth = 2.5 * s
      ..strokeCap = StrokeCap.round;
    final antPaint = Paint()
      ..color = accentHi
      ..strokeWidth = 2 * s
      ..strokeCap = StrokeCap.round;

    // Wings (flap by scaling Y a touch around their anchor).
    final flap = 1.0 - 0.3 * wing;
    void wingAt(double cx, double cy, double rot) {
      canvas.save();
      canvas.translate(cx * s, cy * s);
      canvas.rotate(rot);
      canvas.scale(1, flap);
      final rect = Rect.fromCenter(center: Offset.zero, width: 54 * s, height: 22 * s);
      canvas.drawOval(rect, Paint()..color = accent.withValues(alpha: 0.22));
      canvas.drawOval(
        rect,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = 1 * s
          ..color = accent.withValues(alpha: 0.4),
      );
      canvas.restore();
    }

    wingAt(46, 44, -0.5);
    wingAt(74, 44, 0.5);

    // Legs.
    canvas.drawLine(pt(60, 70), pt(52, 104), legPaint);
    canvas.drawLine(pt(60, 72), pt(68, 104), legPaint);
    canvas.drawLine(pt(56, 68), pt(40, 96), legPaint);
    canvas.drawLine(pt(64, 68), pt(80, 96), legPaint);

    // Abdomen + stripes.
    canvas.drawOval(
      Rect.fromCenter(center: pt(60, 82), width: 30 * s, height: 48 * s),
      Paint()..color = abdomen,
    );
    final stripe = Paint()
      ..color = ink.withValues(alpha: 0.25)
      ..strokeWidth = 2 * s;
    canvas.drawLine(pt(47, 80), pt(73, 80), stripe);
    canvas.drawLine(pt(48, 90), pt(72, 90), stripe);

    // Thorax + head.
    canvas.drawCircle(pt(60, 58), 13 * s, Paint()..color = accent);
    canvas.drawCircle(pt(60, 42), 15 * s, Paint()..color = accentHi);

    // Eyes.
    final eye = Paint()..color = ink;
    canvas.drawCircle(pt(54, 41), 5.5 * s, eye);
    canvas.drawCircle(pt(66, 41), 5.5 * s, eye);
    final glint = Paint()..color = Colors.white;
    canvas.drawCircle(pt(55.6, 39.4), 1.9 * s, glint);
    canvas.drawCircle(pt(67.6, 39.4), 1.9 * s, glint);

    // Proboscis + antennae.
    canvas.drawLine(pt(60, 52), pt(60, 66), legPaint);
    canvas.drawLine(pt(53, 30), pt(46, 16), antPaint);
    canvas.drawLine(pt(67, 30), pt(74, 16), antPaint);
    canvas.drawCircle(pt(46, 15), 2.6 * s, Paint()..color = accentHi);
    canvas.drawCircle(pt(74, 15), 2.6 * s, Paint()..color = accentHi);
  }

  @override
  bool shouldRepaint(_MascotPainter old) =>
      old.wing != wing || old.accent != accent || old.accent2 != accent2;
}
