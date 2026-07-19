package com.mozzid.domain.model

/**
 * Output of a [com.mozzid.domain.classifier.SpeciesClassifier] run over one
 * wingbeat sample: the best match plus the runner-up and the measured frequency.
 * Pure data — no persistence.
 */
data class ClassificationResult(
    val primary: Species,
    val runner: Species,
    val confidence: Int,       // 0..100
    val runnerConfidence: Int, // 0..100
    val wingbeatHz: Int,       // measured fundamental, e.g. 612
)
