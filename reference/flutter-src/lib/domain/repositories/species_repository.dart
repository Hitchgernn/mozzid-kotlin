import '../models/species.dart';

/// Read-only catalogue of known species. Backed by an on-device static table
/// today; could later hydrate from a downloaded model bundle.
abstract interface class SpeciesRepository {
  List<Species> all();
  Species? byId(String id);

  /// The species ids the classifier can currently emit, in catalogue order.
  List<String> get classifiableIds;
}
