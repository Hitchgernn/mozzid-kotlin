import 'package:sqflite/sqflite.dart';

/// Key/value persistence for user settings (language, theme, accent, toggles,
/// onboarding), living in the same offline database as detections.
class SettingsStore {
  SettingsStore(this._db);

  final Database _db;

  Future<Map<String, String>> readAll() async {
    final rows = await _db.query('settings');
    return {
      for (final r in rows) r['key']! as String: r['value']! as String,
    };
  }

  Future<void> write(String key, String value) => _db.insert(
        'settings',
        {'key': key, 'value': value},
        conflictAlgorithm: ConflictAlgorithm.replace,
      );
}
