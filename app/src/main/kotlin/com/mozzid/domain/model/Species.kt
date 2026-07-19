package com.mozzid.domain.model

/**
 * When a species is typically biting. Used for the result screen's active-time
 * cross-check ("detected at an unusual hour").
 */
enum class ActiveWindow {
    DAY,          // daytime + dawn/dusk biter (e.g. Aedes)
    NIGHT,        // strictly night
    DUSK_TO_DAWN; // dusk through dawn (e.g. Anopheles)

    val isDayBiter: Boolean get() = this == DAY
}

/**
 * A mosquito species reference record. Static, on-device knowledge — no network.
 * [id] is the stable key stored on every [Detection].
 */
data class Species(
    val id: String,
    val scientificName: String,
    val commonName: String,
    val diseases: String,
    val wingbeatHz: String,     // representative, e.g. "~600 Hz"
    val wingbeatRange: String,  // full range, e.g. "450–700 Hz"
    val severity: Severity,
    val activeWindow: ActiveWindow,
    val activeLabel: String,    // human label, e.g. "Day · dawn/dusk"
    val note: String,
    val tips: List<String>,
    val dotColorArgb: Long,     // map-pin / list colour, as ARGB (see [Severity])
)
