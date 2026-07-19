import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../providers/bootstrap.dart';
import '../providers/settings_provider.dart';
import '../widgets/mozz_mascot.dart';

/// 3-step intro: how to record, offline promise, and mic + location primers.
class OnboardingScreen extends ConsumerStatefulWidget {
  const OnboardingScreen({super.key});

  @override
  ConsumerState<OnboardingScreen> createState() => _OnboardingScreenState();
}

class _OnboardingScreenState extends ConsumerState<OnboardingScreen> {
  int _step = 0;

  Future<void> _next() async {
    if (_step < 2) {
      setState(() => _step++);
      return;
    }
    // Final step: request the real permissions, then enter the app.
    await ref.read(audioRecorderProvider).requestPermission();
    await ref.read(locationServiceProvider).requestPermission();
    await ref.read(notificationServiceProvider).requestPermission();
    ref.read(settingsProvider.notifier).completeOnboarding();
  }

  void _skip() => ref.read(settingsProvider.notifier).completeOnboarding();

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    final pages = [
      (title: l.onboardCatchTitle, body: l.onboardCatchBody, perms: false),
      (title: l.onboardOfflineTitle, body: l.onboardOfflineBody, perms: false),
      (title: l.onboardPermsTitle, body: l.onboardPermsBody, perms: true),
    ];
    final page = pages[_step];

    return Scaffold(
      body: DecoratedBox(
        decoration: BoxDecoration(
          gradient: RadialGradient(
            center: const Alignment(0, -1),
            radius: 1.1,
            colors: [c.surface3, c.bg],
            stops: const [0, 0.6],
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(30, 8, 30, 24),
            child: Column(
              children: [
                Align(
                  alignment: Alignment.centerRight,
                  child: TextButton(
                    onPressed: _skip,
                    child: Text(l.skip,
                        style: MozzType.sans(size: 14, color: c.text4)),
                  ),
                ),
                Expanded(
                  child: SingleChildScrollView(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        const SizedBox(height: 20),
                        const MozzMascot(size: 112),
                        const SizedBox(height: 24),
                        Text(
                          page.title,
                          textAlign: TextAlign.center,
                          style: MozzType.serif(size: 27, color: c.text, height: 1.25),
                        ),
                        const SizedBox(height: 12),
                        ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 280),
                          child: Text(
                            page.body,
                            textAlign: TextAlign.center,
                            style: MozzType.sans(size: 15, color: c.text3, height: 1.55),
                          ),
                        ),
                        if (page.perms) ...[
                          const SizedBox(height: 22),
                          _PermRow(
                            icon: Icons.mic_none_rounded,
                            title: l.micTitle,
                            note: l.micNote,
                            allow: l.allow,
                          ),
                          const SizedBox(height: 10),
                          _PermRow(
                            icon: Icons.location_on_outlined,
                            title: l.locTitle,
                            note: l.locNote,
                            allow: l.allow,
                          ),
                        ],
                      ],
                    ),
                  ),
                ),
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    for (var i = 0; i < 3; i++)
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        margin: const EdgeInsets.symmetric(horizontal: 4),
                        height: 7,
                        width: i == _step ? 22 : 7,
                        decoration: BoxDecoration(
                          color: i == _step ? c.accent : c.line2,
                          borderRadius: BorderRadius.circular(4),
                        ),
                      ),
                  ],
                ),
                const SizedBox(height: 22),
                SizedBox(
                  width: double.infinity,
                  height: 54,
                  child: FilledButton(
                    onPressed: _next,
                    style: FilledButton.styleFrom(
                      backgroundColor: c.accent,
                      foregroundColor: c.accentInk,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(16),
                      ),
                    ),
                    child: Text(
                      _step < 2 ? l.next : l.startListening,
                      style: MozzType.sans(size: 16, weight: FontWeight.w700, color: c.accentInk),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _PermRow extends StatelessWidget {
  const _PermRow({
    required this.icon,
    required this.title,
    required this.note,
    required this.allow,
  });

  final IconData icon;
  final String title;
  final String note;
  final String allow;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return ConstrainedBox(
      constraints: const BoxConstraints(maxWidth: 280),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
        decoration: BoxDecoration(
          color: c.surface,
          borderRadius: BorderRadius.circular(14),
          border: Border.all(color: c.accentMix(20)),
        ),
        child: Row(
          children: [
            Icon(icon, color: c.accent, size: 22),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(title, style: MozzType.sans(size: 13.5, weight: FontWeight.w600, color: c.text)),
                  Text(note, style: MozzType.sans(size: 11.5, color: c.text4)),
                ],
              ),
            ),
            Text(allow, style: MozzType.sans(size: 12, weight: FontWeight.w600, color: c.accent)),
          ],
        ),
      ),
    );
  }
}
