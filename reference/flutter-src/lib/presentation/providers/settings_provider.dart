import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/theme/accent.dart';
import '../../data/local/settings_store.dart';
import 'bootstrap.dart';

/// Live app settings. Theme, accent and language changes rebuild the whole app
/// via [AppTheme] / [AppLocalizations], and every change is persisted to the
/// offline settings table.
@immutable
class SettingsState {
  const SettingsState({
    required this.languageCode,
    required this.brightness,
    required this.accent,
    required this.voiceOutput,
    required this.backgroundListening,
    required this.onboarded,
  });

  final String languageCode; // 'en' | 'id'
  final Brightness brightness;
  final AppAccent accent;
  final bool voiceOutput;
  final bool backgroundListening;
  final bool onboarded;

  Locale get locale => Locale(languageCode);
  bool get isDark => brightness == Brightness.dark;

  SettingsState copyWith({
    String? languageCode,
    Brightness? brightness,
    AppAccent? accent,
    bool? voiceOutput,
    bool? backgroundListening,
    bool? onboarded,
  }) =>
      SettingsState(
        languageCode: languageCode ?? this.languageCode,
        brightness: brightness ?? this.brightness,
        accent: accent ?? this.accent,
        voiceOutput: voiceOutput ?? this.voiceOutput,
        backgroundListening: backgroundListening ?? this.backgroundListening,
        onboarded: onboarded ?? this.onboarded,
      );

  factory SettingsState.fromMap(Map<String, String> m) => SettingsState(
        languageCode: m['language'] ?? 'en',
        brightness: (m['theme'] ?? 'dark') == 'light'
            ? Brightness.light
            : Brightness.dark,
        accent: AppAccent.fromName(m['accent']),
        voiceOutput: m['voice'] == 'true',
        backgroundListening: m['background'] != 'false', // default on
        onboarded: m['onboarded'] == 'true',
      );
}

class SettingsNotifier extends Notifier<SettingsState> {
  late final SettingsStore _store;

  @override
  SettingsState build() {
    _store = ref.watch(settingsStoreProvider);
    return SettingsState.fromMap(ref.watch(bootstrapProvider).initialSettings);
  }

  void _persist(String key, String value) {
    // Fire-and-forget; the in-memory state is the source of truth for the UI.
    _store.write(key, value);
  }

  void setLanguage(String code) {
    state = state.copyWith(languageCode: code);
    _persist('language', code);
  }

  void setBrightness(Brightness b) {
    state = state.copyWith(brightness: b);
    _persist('theme', b == Brightness.light ? 'light' : 'dark');
  }

  void setAccent(AppAccent accent) {
    state = state.copyWith(accent: accent);
    _persist('accent', accent.name);
  }

  void setVoice(bool on) {
    state = state.copyWith(voiceOutput: on);
    _persist('voice', '$on');
  }

  void setBackground(bool on) {
    state = state.copyWith(backgroundListening: on);
    _persist('background', '$on');
  }

  void completeOnboarding() {
    state = state.copyWith(onboarded: true);
    _persist('onboarded', 'true');
  }

  void replayOnboarding() {
    state = state.copyWith(onboarded: false);
    _persist('onboarded', 'false');
  }
}

final settingsProvider =
    NotifierProvider<SettingsNotifier, SettingsState>(SettingsNotifier.new);
