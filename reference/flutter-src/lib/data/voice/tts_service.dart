import 'package:flutter_tts/flutter_tts.dart';

/// Speaks the result aloud — for night use and low-literacy / low-vision users.
/// Language follows the app locale so ID results are read in Indonesian.
class TtsService {
  TtsService([FlutterTts? tts]) : _tts = tts ?? FlutterTts();

  final FlutterTts _tts;

  Future<void> speak(String text, {required String localeCode}) async {
    await _tts.setLanguage(localeCode == 'id' ? 'id-ID' : 'en-US');
    await _tts.setSpeechRate(0.45);
    await _tts.stop();
    await _tts.speak(text);
  }

  Future<void> stop() => _tts.stop();
}
