import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'core/theme/app_theme.dart';
import 'l10n/gen/app_localizations.dart';
import 'presentation/providers/settings_provider.dart';
import 'presentation/screens/home_shell.dart';
import 'presentation/screens/onboarding_screen.dart';

/// Root widget. Theme (brightness + accent) and locale are driven live by
/// [settingsProvider]; onboarding gates the main shell.
class MozzApp extends ConsumerWidget {
  const MozzApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings = ref.watch(settingsProvider);
    return MaterialApp(
      title: 'MozzID',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.build(settings.brightness, settings.accent),
      locale: settings.locale,
      supportedLocales: AppLocalizations.supportedLocales,
      localizationsDelegates: const [
        AppLocalizations.delegate,
        GlobalMaterialLocalizations.delegate,
        GlobalWidgetsLocalizations.delegate,
        GlobalCupertinoLocalizations.delegate,
      ],
      home: settings.onboarded ? const HomeShell() : const OnboardingScreen(),
    );
  }
}
