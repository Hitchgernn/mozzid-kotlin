import 'package:flutter/material.dart';

import '../../domain/models/severity.dart';
import '../../domain/models/species.dart';
import '../../domain/repositories/species_repository.dart';

/// On-device species knowledge base. Static and offline — the same three
/// vectors the mock (and later the TFLite model) can emit. Copy mirrors the
/// design's reference cards.
class SpeciesCatalog implements SpeciesRepository {
  SpeciesCatalog();

  static const _aedes = Species(
    id: 'aedes',
    scientificName: 'Aedes aegypti',
    commonName: 'Yellow fever mosquito',
    diseases: 'Dengue & Zika',
    wingbeatHz: '~600 Hz',
    wingbeatRange: '450–700 Hz',
    severity: Severity.high,
    activeWindow: ActiveWindow.day,
    activeLabel: 'Day · dawn/dusk',
    note:
        'A daytime biter thriving in urban water containers. The primary vector '
        'of dengue across tropical cities.',
    tips: [
      'Empty standing water in pots, tyres and gutters weekly.',
      'Use screens and repellent during daylight hours.',
      'Wear long sleeves at dawn and dusk.',
    ],
    dotColor: Color(0xFFFF8A7A),
  );

  static const _culex = Species(
    id: 'culex',
    scientificName: 'Culex quinquefasciatus',
    commonName: 'Southern house mosquito',
    diseases: 'West Nile & filariasis',
    wingbeatHz: '~350 Hz',
    wingbeatRange: '300–450 Hz',
    severity: Severity.moderate,
    activeWindow: ActiveWindow.night,
    activeLabel: 'Night',
    note:
        'A night-active house mosquito, drawn to polluted stagnant water. A '
        'vector of lymphatic filariasis.',
    tips: [
      'Clear drains and polluted stagnant water.',
      'Sleep under a bed net at night.',
      'Fit window and door screens.',
    ],
    dotColor: Color(0xFF7FD0FF),
  );

  static const _anopheles = Species(
    id: 'anopheles',
    scientificName: 'Anopheles sundaicus',
    commonName: 'Malaria mosquito',
    diseases: 'Malaria',
    wingbeatHz: '~500 Hz',
    wingbeatRange: '400–600 Hz',
    severity: Severity.high,
    activeWindow: ActiveWindow.duskToDawn,
    activeLabel: 'Night · dusk to dawn',
    note:
        'The malaria vector, biting from dusk to dawn. Rests at a distinctive '
        'head-down angle.',
    tips: [
      'Sleep under an insecticide-treated net.',
      'Use indoor residual spraying where advised.',
      'Cover skin after sunset.',
    ],
    dotColor: Color(0xFFFFCF6B),
  );

  static const List<Species> _all = [_aedes, _culex, _anopheles];
  late final Map<String, Species> _byId = {for (final s in _all) s.id: s};

  @override
  List<Species> all() => _all;

  @override
  Species? byId(String id) => _byId[id];

  @override
  List<String> get classifiableIds => const ['aedes', 'culex', 'anopheles'];
}
