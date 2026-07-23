package com.mozzid.data.species

import com.mozzid.R
import com.mozzid.domain.model.ActiveWindow
import com.mozzid.domain.model.Severity
import com.mozzid.domain.model.Species
import com.mozzid.domain.repository.SpeciesRepository

/**
 * On-device species knowledge base containing the 6 species from the Potamitis Wingbeats dataset.
 * Static and offline — vectors the mock and TFLite model emit.
 */
class SpeciesCatalog : SpeciesRepository {

    private val aedesAegypti = Species(
        id = "aedes_aegypti",
        scientificName = "Aedes aegypti",
        commonName = "Yellow fever mosquito",
        diseases = "Dengue & Zika",
        wingbeatHz = "~600 Hz",
        wingbeatRange = "450–700 Hz",
        severity = Severity.HIGH,
        activeWindow = ActiveWindow.DAY,
        activeLabel = "Day · dawn/dusk",
        note = "A daytime biter thriving in urban water containers. The primary " +
            "vector of dengue across tropical cities.",
        tips = listOf(
            "Empty standing water in pots, tyres and gutters weekly.",
            "Use screens and repellent during daylight hours.",
            "Wear long sleeves at dawn and dusk.",
        ),
        dotColorArgb = 0xFFFF8A7A,
        photoRes = R.drawable.mosquito_aedes_aegypti,
    )

    private val aedesAlbopictus = Species(
        id = "aedes_albopictus",
        scientificName = "Aedes albopictus",
        commonName = "Asian tiger mosquito",
        diseases = "Dengue & Chikungunya",
        wingbeatHz = "~550 Hz",
        wingbeatRange = "450–650 Hz",
        severity = Severity.HIGH,
        activeWindow = ActiveWindow.DAY,
        activeLabel = "Day · dawn/dusk",
        note = "Aggressive daytime biter with distinctive white stripes. Adapts well to cooler environments.",
        tips = listOf(
            "Clear small artificial water containers outdoors.",
            "Wear protective clothing in shaded garden areas.",
            "Apply EPA-registered insect repellent.",
        ),
        dotColorArgb = 0xFFFF6B6B,
        photoRes = R.drawable.mosquito_aedes_albopictus,
    )

    private val anophelesGambiae = Species(
        id = "anopheles_gambiae",
        scientificName = "Anopheles gambiae",
        commonName = "African malaria mosquito",
        diseases = "Malaria",
        wingbeatHz = "~500 Hz",
        wingbeatRange = "400–600 Hz",
        severity = Severity.HIGH,
        activeWindow = ActiveWindow.DUSK_TO_DAWN,
        activeLabel = "Night · dusk to dawn",
        note = "The primary malaria vector in sub-Saharan Africa. Biting from dusk to dawn, " +
            "rests at a distinctive head-down angle.",
        tips = listOf(
            "Sleep under an insecticide-treated net.",
            "Use indoor residual spraying where advised.",
            "Cover skin after sunset.",
        ),
        dotColorArgb = 0xFFFFCF6B,
        photoRes = R.drawable.mosquito_anopheles_gambiae,
    )

    private val anophelesAlbimanus = Species(
        id = "anopheles_albimanus",
        scientificName = "Anopheles albimanus",
        commonName = "Central American malaria mosquito",
        diseases = "Malaria",
        wingbeatHz = "~480 Hz",
        wingbeatRange = "400–580 Hz",
        severity = Severity.HIGH,
        activeWindow = ActiveWindow.DUSK_TO_DAWN,
        activeLabel = "Night · dusk to dawn",
        note = "Major malaria vector in Central America and the Caribbean. Breeds in sunlit freshwater pools.",
        tips = listOf(
            "Sleep under insecticide-treated nets.",
            "Avoid unscreened outdoor areas at dusk.",
            "Support community mosquito control programs.",
        ),
        dotColorArgb = 0xFFFFD97D,
        photoRes = R.drawable.mosquito_anopheles_albimanus,
    )

    private val culexPipiens = Species(
        id = "culex_pipiens",
        scientificName = "Culex pipiens",
        commonName = "Common house mosquito",
        diseases = "West Nile virus",
        wingbeatHz = "~350 Hz",
        wingbeatRange = "300–450 Hz",
        severity = Severity.MODERATE,
        activeWindow = ActiveWindow.NIGHT,
        activeLabel = "Night",
        note = "Common house mosquito in temperate regions. Bites birds and humans, transmitting West Nile virus.",
        tips = listOf(
            "Clear standing water in gutters and bird baths.",
            "Fit window and door screens securely.",
            "Use sleep mosquito nets if unscreened.",
        ),
        dotColorArgb = 0xFF7FD0FF,
        photoRes = R.drawable.mosquito_culex_pipiens,
    )

    private val culexQuinquefasciatus = Species(
        id = "culex_quinquefasciatus",
        scientificName = "Culex quinquefasciatus",
        commonName = "Southern house mosquito",
        diseases = "West Nile & filariasis",
        wingbeatHz = "~380 Hz",
        wingbeatRange = "300–480 Hz",
        severity = Severity.MODERATE,
        activeWindow = ActiveWindow.NIGHT,
        activeLabel = "Night",
        note = "A night-active house mosquito, drawn to polluted stagnant water. " +
            "A vector of lymphatic filariasis.",
        tips = listOf(
            "Clear drains and polluted stagnant water.",
            "Sleep under a bed net at night.",
            "Fit window and door screens.",
        ),
        dotColorArgb = 0xFF4AC3FF,
        photoRes = R.drawable.mosquito_culex_quinquefasciatus,
    )

    private val everySpecies = listOf(
        aedesAegypti,
        aedesAlbopictus,
        anophelesGambiae,
        anophelesAlbimanus,
        culexPipiens,
        culexQuinquefasciatus,
    )
    private val byIdMap = everySpecies.associateBy { it.id }

    override fun all(): List<Species> = everySpecies
    override fun byId(id: String): Species? = byIdMap[id] ?: when (id) {
        "aedes" -> aedesAegypti
        "anopheles" -> anophelesGambiae
        "culex" -> culexQuinquefasciatus
        else -> null
    }

    override val classifiableIds: List<String> = everySpecies.map { it.id }
}

