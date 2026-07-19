import 'package:sqflite/sqflite.dart';

import '../../domain/models/detection.dart';

/// Thin data-access object over the `detections` table. Raw SQL, no codegen.
class DetectionDao {
  DetectionDao(this._db);

  final Database _db;

  Future<List<Detection>> all() async {
    final rows = await _db.query('detections', orderBy: 'timestamp DESC');
    return rows.map(Detection.fromMap).toList();
  }

  Future<Detection> insert(Detection detection) async {
    final id = await _db.insert('detections', detection.toMap());
    return detection.copyWith(id: id);
  }

  Future<void> delete(int id) =>
      _db.delete('detections', where: 'id = ?', whereArgs: [id]);

  Future<void> deleteAll() => _db.delete('detections');
}
