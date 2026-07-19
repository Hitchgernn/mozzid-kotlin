import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/l10n_ext.dart';
import '../../core/theme/accent.dart';
import '../../core/theme/app_theme.dart';
import '../../core/theme/typography.dart';
import '../providers/bootstrap.dart';
import '../providers/detection_providers.dart';
import '../providers/settings_provider.dart';
import '../widgets/morning_summary_sheet.dart';

class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final c = context.c;
    final l = context.l;
    final settings = ref.watch(settingsProvider);
    final s = ref.read(settingsProvider.notifier);

    return SafeArea(
      child: ListView(
        padding: const EdgeInsets.fromLTRB(22, 8, 22, 24),
        children: [
          Text(l.settings, style: MozzType.serif(size: 26, color: c.text)),
          const SizedBox(height: 18),

          // Appearance
          _Card(children: [
            _Row(
              title: l.appearance,
              note: l.appearanceNote,
              trailing: _Segment(
                options: [l.dark, l.light],
                selected: settings.isDark ? 0 : 1,
                onSelect: (i) => s.setBrightness(i == 0 ? Brightness.dark : Brightness.light),
              ),
            ),
            _Divider(),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l.accentLabel, style: MozzType.sans(size: 14.5, weight: FontWeight.w600, color: c.text)),
                  const SizedBox(height: 2),
                  Text(l.accentNote, style: MozzType.sans(size: 12, color: c.text4)),
                  const SizedBox(height: 14),
                  Row(
                    children: [
                      for (final a in AppAccent.values)
                        Padding(
                          padding: const EdgeInsets.only(right: 13),
                          child: GestureDetector(
                            onTap: () => s.setAccent(a),
                            child: Container(
                              width: 46,
                              height: 46,
                              alignment: Alignment.center,
                              decoration: BoxDecoration(
                                color: a.color,
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(
                                  color: settings.accent == a
                                      ? (settings.isDark ? Colors.white : const Color(0xFF0F1720))
                                      : Colors.transparent,
                                  width: 2,
                                ),
                              ),
                              child: settings.accent == a
                                  ? Icon(Icons.check_rounded, size: 18, color: a.ink)
                                  : null,
                            ),
                          ),
                        ),
                    ],
                  ),
                ],
              ),
            ),
          ]),
          const SizedBox(height: 12),

          // Language + toggles
          _Card(children: [
            _Row(
              title: l.language,
              note: l.langNote,
              trailing: _Segment(
                options: const ['EN', 'ID'],
                selected: settings.languageCode == 'en' ? 0 : 1,
                onSelect: (i) => s.setLanguage(i == 0 ? 'en' : 'id'),
              ),
            ),
            _Divider(),
            _ToggleRow(
              title: l.voiceOut,
              note: l.voiceNote,
              value: settings.voiceOutput,
              onChanged: s.setVoice,
            ),
            _Divider(),
            _ToggleRow(
              title: l.bgListen,
              note: l.bgNote,
              value: settings.backgroundListening,
              onChanged: s.setBackground,
            ),
          ]),

          if (settings.backgroundListening) ...[
            const SizedBox(height: 12),
            _MorningButton(onTap: () => showMorningSummary(context)),
          ],

          const SizedBox(height: 22),
          _SectionLabel(l.notifications),
          _NotifCard(
            color: const Color(0xFFFF8A7A),
            glyph: '▲',
            title: l.notif1Title,
            body: l.notif1Body,
          ),
          const SizedBox(height: 9),
          _NotifCard(
            icon: Icons.schedule_rounded,
            color: const Color(0xFF7FD0FF),
            title: l.notif2Title,
            body: l.notif2Body,
          ),
          const SizedBox(height: 9),
          OutlinedButton(
            onPressed: () => ref
                .read(notificationServiceProvider)
                .showActivityAlert(l.notif1Title, l.notif1Body),
            style: OutlinedButton.styleFrom(
              side: BorderSide(color: c.accentMix(35)),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(13)),
              padding: const EdgeInsets.all(11),
            ),
            child: Text(l.previewNotif,
                style: MozzType.sans(size: 13, weight: FontWeight.w600, color: c.accent)),
          ),

          const SizedBox(height: 22),
          _SectionLabel(l.data),
          _ActionRow(
            title: l.exportCsv,
            icon: Icons.file_download_outlined,
            onTap: () async {
              final log = ref.read(detectionsProvider).valueOrNull ?? const [];
              await ref.read(csvExporterProvider).exportAndShare(log);
            },
          ),
          const SizedBox(height: 9),
          _ActionRow(
            title: l.replayIntro,
            icon: Icons.chevron_right_rounded,
            onTap: s.replayOnboarding,
          ),
          const SizedBox(height: 22),
          Center(
            child: Text('MozzID · on-device · v1.0',
                style: MozzType.mono(size: 11, color: c.faint)),
          ),
        ],
      ),
    );
  }
}

class _Card extends StatelessWidget {
  const _Card({required this.children});
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Container(
      clipBehavior: Clip.hardEdge,
      decoration: BoxDecoration(
        color: c.surface,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: c.line),
      ),
      child: Column(children: children),
    );
  }
}

class _Divider extends StatelessWidget {
  @override
  Widget build(BuildContext context) =>
      Container(height: 1, color: context.c.line);
}

class _Row extends StatelessWidget {
  const _Row({required this.title, required this.note, required this.trailing});
  final String title;
  final String note;
  final Widget trailing;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: MozzType.sans(size: 14.5, weight: FontWeight.w600, color: c.text)),
                const SizedBox(height: 2),
                Text(note, style: MozzType.sans(size: 12, color: c.text4)),
              ],
            ),
          ),
          trailing,
        ],
      ),
    );
  }
}

class _ToggleRow extends StatelessWidget {
  const _ToggleRow({
    required this.title,
    required this.note,
    required this.value,
    required this.onChanged,
  });
  final String title;
  final String note;
  final bool value;
  final ValueChanged<bool> onChanged;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return _Row(
      title: title,
      note: note,
      trailing: Switch(
        value: value,
        onChanged: onChanged,
        activeTrackColor: c.accent2,
        thumbColor: const WidgetStatePropertyAll(Colors.white),
      ),
    );
  }
}

class _Segment extends StatelessWidget {
  const _Segment({required this.options, required this.selected, required this.onSelect});
  final List<String> options;
  final int selected;
  final ValueChanged<int> onSelect;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Container(
      padding: const EdgeInsets.all(3),
      decoration: BoxDecoration(
        color: c.bg,
        borderRadius: BorderRadius.circular(11),
        border: Border.all(color: c.line),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var i = 0; i < options.length; i++)
            GestureDetector(
              onTap: () => onSelect(i),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 13, vertical: 7),
                decoration: BoxDecoration(
                  color: selected == i ? c.accent : Colors.transparent,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  options[i],
                  style: MozzType.sans(
                    size: 12.5,
                    weight: FontWeight.w600,
                    color: selected == i ? c.accentInk : c.text3,
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _MorningButton extends StatelessWidget {
  const _MorningButton({required this.onTap});
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    final l = context.l;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          gradient: LinearGradient(colors: [c.accentMix(12), c.accentMix(3)]),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: c.accentMix(22)),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(l.morningReady, style: MozzType.sans(size: 14, weight: FontWeight.w600, color: c.text)),
                  const SizedBox(height: 2),
                  Text(l.viewSummary, style: MozzType.sans(size: 12, color: c.accentSoftText)),
                ],
              ),
            ),
            Icon(Icons.chevron_right_rounded, color: c.accent, size: 20),
          ],
        ),
      ),
    );
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel(this.text);
  final String text;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Padding(
      padding: const EdgeInsets.only(left: 4, bottom: 10),
      child: Text(text.toUpperCase(),
          style: MozzType.sans(size: 11, weight: FontWeight.w600, color: c.text4, letterSpacing: 1)),
    );
  }
}

class _NotifCard extends StatelessWidget {
  const _NotifCard({
    required this.color,
    required this.title,
    required this.body,
    this.glyph,
    this.icon,
  });
  final Color color;
  final String title;
  final String body;
  final String? glyph;
  final IconData? icon;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 15, vertical: 13),
      decoration: BoxDecoration(
        color: c.surface2,
        borderRadius: BorderRadius.circular(15),
        border: Border.all(color: c.line),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 34,
            height: 34,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.14),
              borderRadius: BorderRadius.circular(10),
            ),
            child: glyph != null
                ? Text(glyph!, style: MozzType.mono(size: 15, weight: FontWeight.w700, color: color))
                : Icon(icon, size: 18, color: color),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(title, style: MozzType.sans(size: 13, weight: FontWeight.w600, color: c.text)),
                const SizedBox(height: 2),
                Text(body, style: MozzType.sans(size: 12, color: c.text3, height: 1.4)),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionRow extends StatelessWidget {
  const _ActionRow({required this.title, required this.icon, required this.onTap});
  final String title;
  final IconData icon;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = context.c;
    return GestureDetector(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 15),
        decoration: BoxDecoration(
          color: c.surface,
          borderRadius: BorderRadius.circular(15),
          border: Border.all(color: c.line),
        ),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(title, style: MozzType.sans(size: 14, weight: FontWeight.w600, color: c.text)),
            Icon(icon, color: c.text2, size: 20),
          ],
        ),
      ),
    );
  }
}
