import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import 'history_screen.dart';
import 'record_screen.dart';
import 'settings_screen.dart';

/// Hosts the three primary tabs with the custom bottom nav from the design.
/// Record is the centre / default tab.
class HomeShell extends ConsumerStatefulWidget {
  const HomeShell({super.key});

  @override
  ConsumerState<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends ConsumerState<HomeShell> {
  int _index = 1; // record

  static const _tabs = [HistoryScreen(), RecordScreen(), SettingsScreen()];

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: IndexedStack(index: _index, children: _tabs),
      bottomNavigationBar: _BottomNav(
        index: _index,
        onTap: (i) => setState(() => _index = i),
      ),
    );
  }
}

class _BottomNav extends StatelessWidget {
  const _BottomNav({required this.index, required this.onTap});

  final int index;
  final ValueChanged<int> onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    Color colorFor(int i) => index == i ? c.accent : c.text4;

    return Container(
      height: 84,
      decoration: BoxDecoration(
        color: c.bg,
        border: Border(top: BorderSide(color: c.line)),
      ),
      padding: const EdgeInsets.only(top: 12),
      child: Row(
        children: [
          _NavItem(
            icon: Icons.access_time_rounded,
            label: l.history,
            color: colorFor(0),
            onTap: () => onTap(0),
          ),
          _RecordNavItem(
            active: index == 1,
            label: l.record,
            onTap: () => onTap(1),
          ),
          _NavItem(
            icon: Icons.tune_rounded,
            label: l.settings,
            color: colorFor(2),
            onTap: () => onTap(2),
          ),
        ],
      ),
    );
  }
}

class _NavItem extends StatelessWidget {
  const _NavItem({
    required this.icon,
    required this.label,
    required this.color,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final Color color;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Expanded(
      child: InkWell(
        onTap: onTap,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, color: color, size: 24),
            const SizedBox(height: 5),
            Text(label, style: MozzType.sans(size: 10.5, weight: FontWeight.w500, color: color)),
          ],
        ),
      ),
    );
  }
}

class _RecordNavItem extends StatelessWidget {
  const _RecordNavItem({
    required this.active,
    required this.label,
    required this.onTap,
  });

  final bool active;
  final String label;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final color = active ? c.accent : c.text4;
    return Expanded(
      child: InkWell(
        onTap: onTap,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 30,
              height: 30,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: active ? c.accentMix(15) : Colors.transparent,
                shape: BoxShape.circle,
                border: Border.all(color: color, width: 2),
              ),
              child: Container(
                width: 11,
                height: 11,
                decoration: BoxDecoration(color: color, shape: BoxShape.circle),
              ),
            ),
            const SizedBox(height: 5),
            Text(label, style: MozzType.sans(size: 10.5, weight: FontWeight.w500, color: color)),
          ],
        ),
      ),
    );
  }
}
