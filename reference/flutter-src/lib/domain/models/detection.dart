/// A persisted detection: what was heard, how sure, when and where. This is the
/// offline-first record — every field is captured on-device and survives without
/// a network.
class Detection {
  const Detection({
    this.id,
    required this.speciesId,
    required this.confidence,
    required this.wingbeatHz,
    required this.timestamp,
    this.latitude,
    this.longitude,
    this.locationLabel,
  });

  /// Local row id (null until inserted).
  final int? id;
  final String speciesId;
  final int confidence; // 0–100
  final int wingbeatHz;
  final DateTime timestamp;

  /// GPS, null when location was unavailable/denied.
  final double? latitude;
  final double? longitude;

  /// Optional human tag (e.g. "Bedroom"); falls back to coordinates.
  final String? locationLabel;

  Detection copyWith({int? id, String? locationLabel}) => Detection(
        id: id ?? this.id,
        speciesId: speciesId,
        confidence: confidence,
        wingbeatHz: wingbeatHz,
        timestamp: timestamp,
        latitude: latitude,
        longitude: longitude,
        locationLabel: locationLabel ?? this.locationLabel,
      );

  Map<String, Object?> toMap() => {
        if (id != null) 'id': id,
        'species_id': speciesId,
        'confidence': confidence,
        'wingbeat_hz': wingbeatHz,
        'timestamp': timestamp.millisecondsSinceEpoch,
        'latitude': latitude,
        'longitude': longitude,
        'location_label': locationLabel,
      };

  factory Detection.fromMap(Map<String, Object?> m) => Detection(
        id: m['id'] as int?,
        speciesId: m['species_id'] as String,
        confidence: (m['confidence'] as num).toInt(),
        wingbeatHz: (m['wingbeat_hz'] as num).toInt(),
        timestamp:
            DateTime.fromMillisecondsSinceEpoch((m['timestamp'] as num).toInt()),
        latitude: (m['latitude'] as num?)?.toDouble(),
        longitude: (m['longitude'] as num?)?.toDouble(),
        locationLabel: m['location_label'] as String?,
      );
}
