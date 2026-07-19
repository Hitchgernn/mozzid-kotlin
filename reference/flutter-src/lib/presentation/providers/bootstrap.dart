import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/audio/mic_audio_recorder.dart';
import '../../data/classifier/mock_species_classifier.dart';
import '../../data/detection/local_detection_repository.dart';
import '../../data/export/csv_exporter.dart';
import '../../data/local/app_database.dart';
import '../../data/local/detection_dao.dart';
import '../../data/local/settings_store.dart';
import '../../data/location/geolocator_location_service.dart';
import '../../data/notifications/notification_service.dart';
import '../../data/species/species_catalog.dart';
import '../../data/sync/sync_service.dart';
import '../../data/voice/tts_service.dart';
import '../../domain/classifier/species_classifier.dart';
import '../../domain/repositories/detection_repository.dart';
import '../../domain/repositories/species_repository.dart';
import '../../domain/services/audio_recorder.dart';
import '../../domain/services/location_service.dart';

/// All wired-up singletons, assembled once in `main` and injected via a single
/// provider override. Swapping an implementation (e.g. Mock → TFLite classifier,
/// Noop → Firebase sync) is a one-line change here — nothing else moves.
class Bootstrap {
  Bootstrap({
    required this.database,
    required this.detectionRepository,
    required this.speciesRepository,
    required this.classifier,
    required this.audioRecorder,
    required this.location,
    required this.notifications,
    required this.tts,
    required this.csvExporter,
    required this.sync,
    required this.settingsStore,
    required this.initialSettings,
  });

  final AppDatabase database;
  final DetectionRepository detectionRepository;
  final SpeciesRepository speciesRepository;
  final SpeciesClassifier classifier;
  final AudioRecorderService audioRecorder;
  final LocationService location;
  final NotificationService notifications;
  final TtsService tts;
  final CsvExporter csvExporter;
  final SyncService sync;
  final SettingsStore settingsStore;
  final Map<String, String> initialSettings;

  /// Assembles the production graph after the database is open.
  static Future<Bootstrap> create() async {
    final database = await AppDatabase.open();
    final settingsStore = SettingsStore(database.db);
    final species = SpeciesCatalog();
    final classifier = MockSpeciesClassifier(species);
    await classifier.load();
    final notifications = NotificationService();
    await notifications.init();

    return Bootstrap(
      database: database,
      detectionRepository:
          LocalDetectionRepository(DetectionDao(database.db)),
      speciesRepository: species,
      classifier: classifier,
      audioRecorder: MicAudioRecorder(),
      location: GeolocatorLocationService(),
      notifications: notifications,
      tts: TtsService(),
      csvExporter: const CsvExporter(),
      sync: const NoopSyncService(),
      settingsStore: settingsStore,
      initialSettings: await settingsStore.readAll(),
    );
  }
}

/// Overridden in `main` with the real [Bootstrap].
final bootstrapProvider = Provider<Bootstrap>(
  (ref) => throw UnimplementedError('bootstrapProvider must be overridden'),
);

final speciesRepositoryProvider = Provider<SpeciesRepository>(
  (ref) => ref.watch(bootstrapProvider).speciesRepository,
);
final detectionRepositoryProvider = Provider<DetectionRepository>(
  (ref) => ref.watch(bootstrapProvider).detectionRepository,
);
final classifierProvider = Provider<SpeciesClassifier>(
  (ref) => ref.watch(bootstrapProvider).classifier,
);
final audioRecorderProvider = Provider<AudioRecorderService>(
  (ref) => ref.watch(bootstrapProvider).audioRecorder,
);
final locationServiceProvider = Provider<LocationService>(
  (ref) => ref.watch(bootstrapProvider).location,
);
final notificationServiceProvider = Provider<NotificationService>(
  (ref) => ref.watch(bootstrapProvider).notifications,
);
final ttsServiceProvider = Provider<TtsService>(
  (ref) => ref.watch(bootstrapProvider).tts,
);
final csvExporterProvider = Provider<CsvExporter>(
  (ref) => ref.watch(bootstrapProvider).csvExporter,
);
final syncServiceProvider = Provider<SyncService>(
  (ref) => ref.watch(bootstrapProvider).sync,
);
final settingsStoreProvider = Provider<SettingsStore>(
  (ref) => ref.watch(bootstrapProvider).settingsStore,
);
