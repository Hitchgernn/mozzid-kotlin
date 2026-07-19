import 'dart:io';

import 'package:csv/csv.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
import 'package:share_plus/share_plus.dart';

import '../../domain/models/detection.dart';

/// Exports the detection log to a CSV file and opens the share sheet. Works
/// offline — the file is written locally; sharing is the OS's job.
class CsvExporter {
  const CsvExporter();

  Future<File> writeCsv(List<Detection> detections) async {
    final rows = <List<Object?>>[
      ['id', 'species', 'confidence', 'wingbeat_hz', 'timestamp_iso', 'latitude', 'longitude', 'location'],
      for (final d in detections)
        [
          d.id,
          d.speciesId,
          d.confidence,
          d.wingbeatHz,
          d.timestamp.toIso8601String(),
          d.latitude ?? '',
          d.longitude ?? '',
          d.locationLabel ?? '',
        ],
    ];
    final csv = const ListToCsvConverter().convert(rows);
    final dir = await getTemporaryDirectory();
    final file = File(p.join(dir.path, 'mozzid_log.csv'));
    await file.writeAsString(csv);
    return file;
  }

  Future<void> exportAndShare(List<Detection> detections) async {
    final file = await writeCsv(detections);
    await Share.shareXFiles(
      [XFile(file.path, mimeType: 'text/csv')],
      subject: 'MozzID detection log',
    );
  }
}
