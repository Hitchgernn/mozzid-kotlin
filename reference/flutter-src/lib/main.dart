import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';

import 'app.dart';
import 'presentation/providers/bootstrap.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  // Offline-first: use the bundled TTFs (assets/google_fonts/) only — never
  // fetch fonts over the network at runtime.
  GoogleFonts.config.allowRuntimeFetching = false;

  // Assemble the offline graph (DB, repos, mock classifier, services).
  // Swap MockSpeciesClassifier → TfliteSpeciesClassifier and NoopSyncService →
  // FirebaseSyncService inside Bootstrap.create() when those land.
  final bootstrap = await Bootstrap.create();

  runApp(
    ProviderScope(
      overrides: [bootstrapProvider.overrideWithValue(bootstrap)],
      child: const MozzApp(),
    ),
  );
}
