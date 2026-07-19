/// A best-effort GPS fix. Every field is optional — the app is offline-first and
/// must log detections even when location is denied or unavailable.
class GeoFix {
  const GeoFix({this.latitude, this.longitude, this.label});

  final double? latitude;
  final double? longitude;
  final String? label;

  bool get hasCoords => latitude != null && longitude != null;

  static const GeoFix none = GeoFix();
}

abstract interface class LocationService {
  Future<bool> hasPermission();
  Future<bool> requestPermission();

  /// Current position, or [GeoFix.none] if unavailable/denied. Never throws.
  Future<GeoFix> currentFix();
}
