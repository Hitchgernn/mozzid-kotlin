package com.mozzid.data.classifier

import com.mozzid.domain.classifier.AudioSample
import com.mozzid.domain.classifier.SpeciesClassifier
import com.mozzid.domain.model.ClassificationResult
import com.mozzid.domain.repository.SpeciesRepository
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.random.Random

/**
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  STUB — MockSpeciesClassifier                                           │
 * │                                                                          │
 * │  Stands in for the real wingbeat model until the TFLite CNN lands. It    │
 * │  ignores the audio content and returns a plausible random result         │
 * │  (primary + runner-up + a wingbeat frequency near the species range),     │
 * │  after a short delay that mimics on-device inference.                     │
 * │                                                                          │
 * │  Replace with TfliteSpeciesClassifier — no caller changes.               │
 * └────────────────────────────────────────────────────────────────────────┘
 */
class MockSpeciesClassifier(
    private val species: SpeciesRepository,
    private val random: Random = Random.Default,
) : SpeciesClassifier {

    override suspend fun load() {
        // Nothing to load for the mock. The real model would read weights + labels.
    }

    override suspend fun classify(sample: AudioSample): ClassificationResult {
        // Simulate inference latency (~1.7s, matching the design's analyze state).
        delay(1700)

        val ids = species.classifiableIds
        val primaryId = ids[random.nextInt(ids.size)]
        var runnerId = ids[random.nextInt(ids.size)]
        while (runnerId == primaryId) {
            runnerId = ids[random.nextInt(ids.size)]
        }

        val primary = species.byId(primaryId)!!
        val runner = species.byId(runnerId)!!

        val confidence = 78 + random.nextInt(17) // 78..94
        val runnerConfidence = max(3, 100 - confidence - random.nextInt(6))

        return ClassificationResult(
            primary = primary,
            runner = runner,
            confidence = confidence,
            runnerConfidence = runnerConfidence,
            wingbeatHz = freqFor(primaryId),
        )
    }

    /** A believable measured frequency inside each species' known range. */
    private fun freqFor(id: String): Int = when (id) {
        "aedes_aegypti", "aedes" -> 450 + random.nextInt(251)          // 450..700 Hz
        "aedes_albopictus" -> 450 + random.nextInt(201)                // 450..650 Hz
        "anopheles_gambiae", "anopheles" -> 400 + random.nextInt(201)  // 400..600 Hz
        "anopheles_albimanus" -> 400 + random.nextInt(181)             // 400..580 Hz
        "culex_pipiens" -> 300 + random.nextInt(151)                   // 300..450 Hz
        "culex_quinquefasciatus", "culex" -> 300 + random.nextInt(181) // 300..480 Hz
        else -> 300 + random.nextInt(400)
    }

    override suspend fun dispose() {}
}
