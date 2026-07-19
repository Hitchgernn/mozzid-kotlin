import 'dart:async';

import '../../domain/models/detection.dart';
import '../../domain/repositories/detection_repository.dart';
import '../local/detection_dao.dart';

/// SQLite-backed [DetectionRepository]. Keeps an in-memory cache and a broadcast
/// stream so the history screen updates immediately on save without re-querying.
class LocalDetectionRepository implements DetectionRepository {
  LocalDetectionRepository(this._dao);

  final DetectionDao _dao;
  final _controller = StreamController<List<Detection>>.broadcast();
  List<Detection> _cache = const [];
  bool _loaded = false;

  Future<void> _ensureLoaded() async {
    if (_loaded) return;
    _cache = await _dao.all();
    _loaded = true;
  }

  @override
  Future<List<Detection>> all() async {
    await _ensureLoaded();
    return _cache;
  }

  @override
  Future<Detection> add(Detection detection) async {
    await _ensureLoaded();
    final saved = await _dao.insert(detection);
    _cache = [saved, ..._cache]
      ..sort((a, b) => b.timestamp.compareTo(a.timestamp));
    _controller.add(_cache);
    return saved;
  }

  @override
  Future<void> remove(int id) async {
    await _ensureLoaded();
    await _dao.delete(id);
    _cache = _cache.where((d) => d.id != id).toList();
    _controller.add(_cache);
  }

  @override
  Future<void> clear() async {
    await _dao.deleteAll();
    _cache = const [];
    _controller.add(_cache);
  }

  @override
  Stream<List<Detection>> watch() async* {
    await _ensureLoaded();
    yield _cache;
    yield* _controller.stream;
  }

  void dispose() => _controller.close();
}
