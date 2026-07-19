import 'package:geolocator/geolocator.dart';

import '../../domain/services/location_service.dart';

/// GPS via `geolocator`. Best-effort and non-throwing: on any denial, disabled
/// service, or error it returns [GeoFix.none] so a detection still saves.
class GeolocatorLocationService implements LocationService {
  @override
  Future<bool> hasPermission() async {
    final perm = await Geolocator.checkPermission();
    return perm == LocationPermission.always ||
        perm == LocationPermission.whileInUse;
  }

  @override
  Future<bool> requestPermission() async {
    var perm = await Geolocator.checkPermission();
    if (perm == LocationPermission.denied) {
      perm = await Geolocator.requestPermission();
    }
    return perm == LocationPermission.always ||
        perm == LocationPermission.whileInUse;
  }

  @override
  Future<GeoFix> currentFix() async {
    try {
      if (!await Geolocator.isLocationServiceEnabled()) return GeoFix.none;
      if (!await hasPermission()) return GeoFix.none;
      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.medium,
        timeLimit: const Duration(seconds: 8),
      );
      return GeoFix(latitude: pos.latitude, longitude: pos.longitude);
    } catch (_) {
      return GeoFix.none;
    }
  }
}
