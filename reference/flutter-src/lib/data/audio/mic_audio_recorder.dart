import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:record/record.dart';

import '../../domain/classifier/species_classifier.dart';
import '../../domain/services/audio_recorder.dart';

/// Real microphone capture via the `record` plugin. Writes a short AAC/M4A clip
/// to a temp file and hands its path to the classifier as an [AudioSample].
class MicAudioRecorder implements AudioRecorderService {
  MicAudioRecorder([AudioRecorder? recorder])
      : _recorder = recorder ?? AudioRecorder();

  final AudioRecorder _recorder;
  DateTime? _startedAt;
  String? _path;

  @override
  Future<bool> hasPermission() => _recorder.hasPermission();

  @override
  Future<bool> requestPermission() => _recorder.hasPermission();

  @override
  Future<void> start() async {
    if (!await _recorder.hasPermission()) {
      throw StateError('Microphone permission denied');
    }
    final dir = await getTemporaryDirectory();
    final path = p.join(
      dir.path,
      'wingbeat_${DateTime.now().millisecondsSinceEpoch}.m4a',
    );
    await _recorder.start(
      const RecordConfig(encoder: AudioEncoder.aacLc, sampleRate: 44100),
      path: path,
    );
    _startedAt = DateTime.now();
    _path = path;
  }

  @override
  Future<AudioSample> stop() async {
    final path = await _recorder.stop() ?? _path;
    final duration = _startedAt == null
        ? Duration.zero
        : DateTime.now().difference(_startedAt!);
    if (path == null) {
      throw StateError('Recording produced no file');
    }
    return AudioSample(filePath: path, duration: duration);
  }

  @override
  Future<void> cancel() async {
    await _recorder.cancel();
    _startedAt = null;
    _path = null;
  }

  @override
  Future<void> dispose() => _recorder.dispose();
}
