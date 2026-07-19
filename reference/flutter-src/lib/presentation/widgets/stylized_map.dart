import 'dart:typed_data';

import 'package:flutter/material.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../../domain/models/detection.dart';
import '../../domain/repositories/species_repository.dart';

/// A fully-offline, stylised detection map. Pins are placed by normalising each
/// detection's GPS into the frame, with a graceful fallback layout when a fix is
/// missing. This keeps History working with zero network.
///
/// DROP-IN: for real tiles, replace the painted background with a
/// `flutter_map` FlutterMap + TileLayer (Mapbox/OSM) and a MarkerLayer built
/// from these same detections. See README → "Real map tiles".
class StylizedMap extends StatelessWidget {
  const StylizedMap({
    super.key,
    required this.detections,
    required this.species,
    this.animateKey = '',
  });

  final List<Detection> detections;
  final SpeciesRepository species;

  /// Changes whenever the active filter changes; used to re-key the pins so
  /// they replay their drop-in animation on each filter switch.
  final String animateKey;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Container(
      height: 186,
      clipBehavior: Clip.hardEdge,
      decoration: BoxDecoration(
        color: c.surface3,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: c.line),
      ),
      child: Stack(
        children: [
          // Dotted grid + faux streets.
          Positioned.fill(
            child: CustomPaint(
              painter: _MapBackgroundPainter(c.accentMix(9), c.fill),
            ),
          ),
          ..._pins(context),
          Positioned(
            left: 12,
            bottom: 10,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: const Color(0xB3080B11),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Text(
                context.l.detectionsNear,
                style: MozzType.mono(size: 10.5, color: c.text4),
              ),
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _pins(BuildContext context) {
    final located = detections.where((d) => d.latitude != null).toList();
    if (located.isEmpty) return const [];

    final lats = located.map((d) => d.latitude!);
    final lngs = located.map((d) => d.longitude!);
    final minLat = lats.reduce((a, b) => a < b ? a : b);
    final maxLat = lats.reduce((a, b) => a > b ? a : b);
    final minLng = lngs.reduce((a, b) => a < b ? a : b);
    final maxLng = lngs.reduce((a, b) => a > b ? a : b);
    double span(double v, double lo, double hi) =>
        hi - lo < 1e-9 ? 0.5 : (v - lo) / (hi - lo);

    var i = 0;
    return [
      for (final d in located)
        Align(
          alignment: Alignment(
            (0.12 + 0.76 * span(d.longitude!, minLng, maxLng)) * 2 - 1,
            (0.15 + 0.6 * (1 - span(d.latitude!, minLat, maxLat))) * 2 - 1,
          ),
          child: _AnimatedPin(
            key: ValueKey('$animateKey-${d.id}'),
            color: species.byId(d.speciesId)?.dotColor ?? context.c.accent,
            delay: Duration(milliseconds: 80 * i++),
          ),
        ),
    ];
  }
}

/// A map pin that drops in with a staggered pop when first shown. Re-created
/// (and thus re-animated) whenever its [ValueKey] changes on a filter switch.
class _AnimatedPin extends StatefulWidget {
  const _AnimatedPin({super.key, required this.color, required this.delay});
  final Color color;
  final Duration delay;

  @override
  State<_AnimatedPin> createState() => _AnimatedPinState();
}

class _AnimatedPinState extends State<_AnimatedPin>
    with SingleTickerProviderStateMixin {
  // mzPin: scale 0 → 1.15 (at 60%) → 1, over 0.5s with an ease-out settle.
  late final AnimationController _c = AnimationController(
    vsync: this,
    duration: const Duration(milliseconds: 500),
  );
  late final Animation<double> _scale = TweenSequence<double>([
    TweenSequenceItem(tween: Tween(begin: 0.0, end: 1.15), weight: 60),
    TweenSequenceItem(tween: Tween(begin: 1.15, end: 1.0), weight: 40),
  ]).animate(CurvedAnimation(parent: _c, curve: const Cubic(0.2, 0.8, 0.2, 1)));

  static const double _deg45 = 0.7853981633974483;

  @override
  void initState() {
    super.initState();
    Future<void>.delayed(widget.delay, () {
      if (mounted) _c.forward();
    });
  }

  @override
  void dispose() {
    _c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    // Teardrop: 22×22 with three rounded corners, sharp bottom-left, rotated
    // -45° so the point faces down. Rotation is on this inner element; the
    // scale animation is on the outer wrapper, so they don't fight.
    final teardrop = Transform.rotate(
      angle: -_deg45,
      child: Container(
        width: 22,
        height: 22,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: widget.color,
          borderRadius: const BorderRadius.only(
            topLeft: Radius.circular(11),
            topRight: Radius.circular(11),
            bottomRight: Radius.circular(11),
          ),
          border: Border.all(color: c.bg, width: 2),
          boxShadow: [
            BoxShadow(color: widget.color.withValues(alpha: 0.5), blurRadius: 12, spreadRadius: -2),
          ],
        ),
        child: Transform.rotate(
          angle: _deg45,
          child: Container(
            width: 6,
            height: 6,
            decoration: BoxDecoration(color: c.bg, shape: BoxShape.circle),
          ),
        ),
      ),
    );

    // Anchor the tip on the coordinate (like CSS translate(-50%,-100%)), and
    // scale from that tip so the pin grows out of its point.
    return FractionalTranslation(
      translation: const Offset(0, -0.5),
      child: RepaintBoundary(
        child: AnimatedBuilder(
          animation: _scale,
          builder: (context, child) => Transform.scale(
            scale: _scale.value,
            alignment: Alignment.bottomCenter,
            child: child,
          ),
          child: teardrop,
        ),
      ),
    );
  }
}

class _MapBackgroundPainter extends CustomPainter {
  _MapBackgroundPainter(this.dot, this.street);
  final Color dot;
  final Color street;

  @override
  void paint(Canvas canvas, Size size) {
    final dotPaint = Paint()..color = dot;
    const gap = 22.0;
    for (var x = 0.0; x < size.width; x += gap) {
      for (var y = 0.0; y < size.height; y += gap) {
        canvas.drawCircle(Offset(x, y), 1, dotPaint);
      }
    }
    final streetPaint = Paint()..color = street;
    canvas.save();
    canvas.translate(size.width * 0.16, 0);
    canvas.transform(_skewX(-0.24));
    canvas.drawRect(Rect.fromLTWH(0, 0, 30, size.height), streetPaint);
    canvas.restore();
    canvas.drawRect(
      Rect.fromLTWH(0, size.height * 0.44, size.width, 24),
      streetPaint,
    );
  }

  Float64List _skewX(double s) => Float64List.fromList([
        1, 0, 0, 0,
        s, 1, 0, 0,
        0, 0, 1, 0,
        0, 0, 0, 1,
      ]);

  @override
  bool shouldRepaint(_MapBackgroundPainter old) =>
      old.dot != dot || old.street != street;
}
