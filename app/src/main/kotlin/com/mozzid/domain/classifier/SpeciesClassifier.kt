package com.mozzid.domain.classifier

import com.mozzid.domain.model.ClassificationResult

/**
 * One captured wingbeat sample handed to the classifier. Today just the recorded
 * audio file path plus duration; when the real model lands it will also carry the
 * decoded PCM / mel-spectrogram. Keep this the single input type so the ML swap
 * does not ripple outward.
 */
data class AudioSample(
    val filePath: String,
    val durationMillis: Long,
    val sampleRate: Int = 44100,
)

/**
 * ┌────────────────────────────────────────────────────────────────────────┐
 * │  ML SEAM — SpeciesClassifier                                            │
 * │                                                                          │
 * │  The ONLY boundary between the app and the wingbeat model. Everything    │
 * │  upstream (record → analyze → result → save) talks to this interface,    │
 * │  never a concrete model.                                                 │
 * │   • Now:   MockSpeciesClassifier returns a plausible fake result.        │
 * │   • Later: TfliteSpeciesClassifier decodes audio → mel-spectrogram →     │
 * │            CNN (TFLite) → ClassificationResult. Drop it in and swap the   │
 * │            binding in Bootstrap; no UI changes required.                  │
 * └────────────────────────────────────────────────────────────────────────┘
 */
interface SpeciesClassifier {
    /** Load model weights / labels. Cheap and idempotent for the mock. */
    suspend fun load()

    /** Classify a single wingbeat sample. Runs fully on-device. */
    suspend fun classify(sample: AudioSample): ClassificationResult

    /** Release native resources (interpreter, buffers). */
    suspend fun dispose()
}
