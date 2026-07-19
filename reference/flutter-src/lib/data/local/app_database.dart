import 'package:path/path.dart' as p;
import 'package:sqflite/sqflite.dart';

/// Owns the single on-device SQLite database. Offline-first: this is the source
/// of truth; any future sync layer reconciles against it, never replaces it.
class AppDatabase {
  AppDatabase._(this.db);

  final Database db;

  static const _fileName = 'mozzid.db';
  static const _version = 1;

  /// Set false to launch with an empty History. Seeds a handful of demo
  /// detections on first install so the map/stats have something to show.
  static const bool seedDemoData = true;

  static Future<AppDatabase> open() async {
    final dir = await getDatabasesPath();
    final path = p.join(dir, _fileName);
    final db = await openDatabase(
      path,
      version: _version,
      onConfigure: (db) => db.execute('PRAGMA foreign_keys = ON'),
      onCreate: _onCreate,
    );
    return AppDatabase._(db);
  }

  static Future<void> _onCreate(Database db, int version) async {
    await db.execute('''
      CREATE TABLE detections (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        species_id TEXT NOT NULL,
        confidence INTEGER NOT NULL,
        wingbeat_hz INTEGER NOT NULL,
        timestamp INTEGER NOT NULL,
        latitude REAL,
        longitude REAL,
        location_label TEXT
      )
    ''');
    await db.execute(
      'CREATE INDEX idx_detections_timestamp ON detections(timestamp DESC)',
    );
    await db.execute('''
      CREATE TABLE settings (
        key TEXT PRIMARY KEY,
        value TEXT NOT NULL
      )
    ''');

    if (seedDemoData) await _seed(db);
  }

  static Future<void> _seed(Database db) async {
    final now = DateTime.now();
    DateTime at(int daysAgo, int hour, int minute) => DateTime(
          now.year,
          now.month,
          now.day - daysAgo,
          hour,
          minute,
        );
    final rows = <Map<String, Object?>>[
      {'species_id': 'aedes', 'confidence': 87, 'wingbeat_hz': 612, 'timestamp': at(0, 23, 42).millisecondsSinceEpoch, 'latitude': -6.2005, 'longitude': 106.8166, 'location_label': 'Bedroom'},
      {'species_id': 'culex', 'confidence': 79, 'wingbeat_hz': 372, 'timestamp': at(0, 22, 5).millisecondsSinceEpoch, 'latitude': -6.2011, 'longitude': 106.8172, 'location_label': 'Balcony'},
      {'species_id': 'aedes', 'confidence': 91, 'wingbeat_hz': 640, 'timestamp': at(1, 6, 20).millisecondsSinceEpoch, 'latitude': -6.1998, 'longitude': 106.8159, 'location_label': 'Kitchen'},
      {'species_id': 'anopheles', 'confidence': 74, 'wingbeat_hz': 510, 'timestamp': at(2, 2, 15).millisecondsSinceEpoch, 'latitude': -6.2021, 'longitude': 106.8181, 'location_label': 'Bedroom'},
      {'species_id': 'aedes', 'confidence': 83, 'wingbeat_hz': 588, 'timestamp': at(4, 19, 50).millisecondsSinceEpoch, 'latitude': -6.1989, 'longitude': 106.8150, 'location_label': 'Garden'},
      {'species_id': 'culex', 'confidence': 68, 'wingbeat_hz': 341, 'timestamp': at(6, 21, 30).millisecondsSinceEpoch, 'latitude': -6.2030, 'longitude': 106.8190, 'location_label': 'Living rm'},
    ];
    final batch = db.batch();
    for (final r in rows) {
      batch.insert('detections', r);
    }
    await batch.commit(noResult: true);
  }
}
