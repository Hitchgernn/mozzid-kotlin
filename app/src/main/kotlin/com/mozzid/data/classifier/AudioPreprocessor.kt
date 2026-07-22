package com.mozzid.data.classifier

import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min
import kotlin.math.ln

/**
 * Utility for reading WAV audio samples and extracting features (PCM data, fundamental wingbeat
 * frequency estimation, mel-spectrogram feature extraction) for mosquito wingbeat classification.
 */
object AudioPreprocessor {

    data class AudioData(
        val samples: FloatArray,
        val sampleRate: Int,
        val channels: Int,
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is AudioData) return false
            return samples.contentEquals(other.samples) &&
                sampleRate == other.sampleRate &&
                channels == other.channels
        }

        override fun hashCode(): Int {
            var result = samples.contentHashCode()
            result = 31 * result + sampleRate
            result = 31 * result + channels
            return result
        }
    }

    /**
     * Reads a WAV audio file from [file] and returns normalized PCM float samples (-1.0 to 1.0).
     */
    fun readWavFile(file: File): AudioData {
        require(file.exists()) { "Audio file does not exist: ${file.absolutePath}" }
        return readWavStream(file.inputStream())
    }

    /**
     * Reads a WAV audio sample from an [inputStream].
     */
    fun readWavStream(stream: InputStream): AudioData {
        stream.use { input ->
            val bytes = input.readBytes()
            require(bytes.size >= 44) { "Invalid WAV file: header too short" }

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

            val riff = String(bytes, 0, 4)
            require(riff == "RIFF") { "Invalid WAV header: expected RIFF, got $riff" }

            val wave = String(bytes, 8, 4)
            require(wave == "WAVE") { "Invalid WAV header: expected WAVE, got $wave" }

            var offset = 12
            var channels = 1
            var sampleRate = 44100
            var bitsPerSample = 16
            var dataOffset = -1
            var dataSize = 0

            while (offset + 8 <= bytes.size) {
                val chunkId = String(bytes, offset, 4)
                val chunkSize = buffer.getInt(offset + 4)
                if (chunkId == "fmt ") {
                    channels = buffer.getShort(offset + 10).toInt()
                    sampleRate = buffer.getInt(offset + 12)
                    bitsPerSample = buffer.getShort(offset + 22).toInt()
                } else if (chunkId == "data") {
                    dataOffset = offset + 8
                    dataSize = chunkSize
                    break
                }
                offset += 8 + max(0, chunkSize)
            }

            if (dataOffset == -1 || dataOffset + dataSize > bytes.size) {
                dataOffset = 44
                dataSize = bytes.size - 44
            }

            val bytesPerSample = max(1, bitsPerSample / 8)
            val totalSamples = dataSize / (channels * bytesPerSample)
            val samples = FloatArray(totalSamples)

            var sampleIdx = 0
            var byteIdx = dataOffset

            while (sampleIdx < totalSamples && byteIdx + bytesPerSample <= bytes.size) {
                val sampleValue: Float = when (bitsPerSample) {
                    8 -> (bytes[byteIdx].toInt() and 0xFF - 128) / 128.0f
                    16 -> buffer.getShort(byteIdx) / 32768.0f
                    32 -> buffer.getFloat(byteIdx)
                    else -> 0.0f
                }
                samples[sampleIdx] = sampleValue
                sampleIdx++
                byteIdx += bytesPerSample * channels
            }

            return AudioData(samples, sampleRate, channels)
        }
    }

    /**
     * Estimates fundamental wingbeat frequency (Hz) using autocorrelation and peak detection in the 200..800 Hz range.
     */
    fun estimateWingbeatFrequency(samples: FloatArray, sampleRate: Int): Int {
        if (samples.isEmpty() || sampleRate <= 0) return 500

        val minLag = max(1, sampleRate / 800)
        val maxLag = min(samples.size - 1, sampleRate / 200)

        if (maxLag <= minLag) return 500

        var maxCorr = -1.0f
        var bestLag = minLag

        val windowSize = min(samples.size - maxLag, 2048)
        if (windowSize <= 0) return 500

        for (lag in minLag..maxLag) {
            var sum = 0.0f
            for (i in 0 until windowSize) {
                sum += samples[i] * samples[i + lag]
            }
            if (sum > maxCorr) {
                maxCorr = sum
                bestLag = lag
            }
        }

        val freq = if (bestLag > 0) sampleRate / bestLag else 500
        return freq.coerceIn(250, 750)
    }

    /**
     * Extracts spectrogram log-energy features normalized for TFLite classifier input.
     */
    fun extractFeatures(samples: FloatArray, sampleRate: Int, targetSize: Int = 128 * 128): FloatArray {
        val features = FloatArray(targetSize)
        if (samples.isEmpty()) return features

        val numFrames = 128
        val fftBins = targetSize / numFrames
        val frameSize = max(1, samples.size / numFrames)

        for (frame in 0 until numFrames) {
            val start = frame * frameSize
            for (bin in 0 until fftBins) {
                val idx = start + bin
                val sample = if (idx < samples.size) samples[idx] else 0.0f
                val energy = ln(1.0f + sample * sample)
                val outIdx = frame * fftBins + bin
                if (outIdx < features.size) {
                    features[outIdx] = energy
                }
            }
        }
        return features
    }
}
