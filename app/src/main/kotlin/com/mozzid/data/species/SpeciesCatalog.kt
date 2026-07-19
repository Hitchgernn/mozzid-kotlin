package com.mozzid.data.species

import androidx.compose.ui.graphics.Color
import com.mozzid.domain.model.ActiveWindow
import com.mozzid.domain.model.Severity
import com.mozzid.domain.model.Species
import com.mozzid.domain.repository.SpeciesRepository

/**
 * On-device species knowledge base. Static and offline — the same three vectors
 * the mock (and later the TFLite model) can emit. Copy mirrors the design cards.
 */
class SpeciesCatalog : SpeciesRepository {

    private val aedes = Species(
        id = "aedes",
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
        dotColor = Color(0xFFFF8A7A),
    )

    private val culex = Species(
        id = "culex",
        scientificName = "Culex quinquefasciatus",
        commonName = "Southern house mosquito",
        diseases = "West Nile & filariasis",
        wingbeatHz = "~350 Hz",
        wingbeatRange = "300–450 Hz",
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
        dotColor = Color(0xFF7FD0FF),
    )

    private val anopheles = Species(
        id = "anopheles",
        scientificName = "Anopheles sundaicus",
        commonName = "Malaria mosquito",
        diseases = "Malaria",
        wingbeatHz = "~500 Hz",
        wingbeatRange = "400–600 Hz",
        severity = Severity.HIGH,
        activeWindow = ActiveWindow.DUSK_TO_DAWN,
        activeLabel = "Night · dusk to dawn",
        note = "The malaria vector, biting from dusk to dawn. Rests at a " +
            "distinctive head-down angle.",
        tips = listOf(
            "Sleep under an insecticide-treated net.",
            "Use indoor residual spraying where advised.",
            "Cover skin after sunset.",
        ),
        dotColor = Color(0xFFFFCF6B),
    )

    private val everySpecies = listOf(aedes, culex, anopheles)
    private val byId = everySpecies.associateBy { it.id }

    override fun all(): List<Species> = everySpecies
    override fun byId(id: String): Species? = byId[id]
    override val classifiableIds: List<String> = listOf("aedes", "culex", "anopheles")
}
