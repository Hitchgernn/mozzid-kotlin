package com.mozzid

import com.mozzid.data.classifier.AudioPreprocessor
import com.mozzid.data.classifier.MockSpeciesClassifier
import com.mozzid.data.classifier.TfliteSpeciesClassifier
import com.mozzid.data.species.SpeciesCatalog
import com.mozzid.domain.classifier.AudioSample
import com.mozzid.domain.model.ActiveWindow
import com.mozzid.domain.model.Severity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpeciesClassifierTest {

    private val speciesCatalog = SpeciesCatalog()

    @Test
    fun `species catalog contains all 6 potamitis species`() {
        val species = speciesCatalog.all()
        assertEquals(6, species.size)

        val ids = speciesCatalog.classifiableIds
        assertEquals(6, ids.size)
        assertTrue(ids.contains("aedes_aegypti"))
        assertTrue(ids.contains("aedes_albopictus"))
        assertTrue(ids.contains("anopheles_gambiae"))
        assertTrue(ids.contains("anopheles_albimanus"))
        assertTrue(ids.contains("culex_pipiens"))
        assertTrue(ids.contains("culex_quinquefasciatus"))
    }

    @Test
    fun `species catalog lookup returns correct metadata`() {
        val aedesAegypti = speciesCatalog.byId("aedes_aegypti")
        assertNotNull(aedesAegypti)
        assertEquals("Aedes aegypti", aedesAegypti?.scientificName)
        assertEquals(Severity.HIGH, aedesAegypti?.severity)
        assertEquals(ActiveWindow.DAY, aedesAegypti?.activeWindow)

        val culexPipiens = speciesCatalog.byId("culex_pipiens")
        assertNotNull(culexPipiens)
        assertEquals("Culex pipiens", culexPipiens?.scientificName)
        assertEquals(Severity.MODERATE, culexPipiens?.severity)
        assertEquals(ActiveWindow.NIGHT, culexPipiens?.activeWindow)
    }

    @Test
    fun `mock species classifier handles all 6 species frequency ranges`() = runTest {
        val classifier = MockSpeciesClassifier(speciesCatalog)
        classifier.load()
        val result = classifier.classify(AudioSample(filePath = "dummy.wav", durationMillis = 2000))
        assertNotNull(result.primary)
        assertNotNull(result.runner)
        assertTrue(result.wingbeatHz in 300..750)
    }

    @Test
    fun `tflite species classifier falls back cleanly to mock classifier when asset is missing`() = runTest {
        val classifier = TfliteSpeciesClassifier(species = speciesCatalog, context = null)
        classifier.load()
        val result = classifier.classify(AudioSample(filePath = "nonexistent.wav", durationMillis = 2000))
        assertNotNull(result.primary)
        assertNotNull(result.runner)
        assertTrue(result.wingbeatHz > 0)
    }

    @Test
    fun `audio preprocessor estimates wingbeat frequency accurately`() {
        val sampleRate = 44100
        val targetFreq = 500 // 500 Hz wingbeat
        val durationSec = 0.5
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = FloatArray(numSamples)

        for (i in 0 until numSamples) {
            samples[i] = kotlin.math.sin(2.0 * Math.PI * targetFreq * i / sampleRate).toFloat()
        }

        val estimatedFreq = AudioPreprocessor.estimateWingbeatFrequency(samples, sampleRate)
        assertTrue("Estimated frequency $estimatedFreq should be close to $targetFreq", kotlin.math.abs(estimatedFreq - targetFreq) <= 35)
    }

    @Test
    fun `audio preprocessor parses synthetic WAV byte stream`() {
        val sampleRate = 44100
        val numSamples = 1000
        val pcmData = ByteArray(numSamples * 2)
        val buffer = ByteBuffer.wrap(pcmData).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until numSamples) {
            buffer.putShort(16384)
        }

        val wavBytes = ByteArrayOutputStream().apply {
            write("RIFF".toByteArray())
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(36 + pcmData.size).array())
            write("WAVE".toByteArray())
            write("fmt ".toByteArray())
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(16).array())
            write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // Audio format = PCM
            write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(1).array()) // Channels = 1
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate).array())
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(sampleRate * 2).array())
            write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(2).array())
            write(ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(16).array())
            write("data".toByteArray())
            write(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(pcmData.size).array())
            write(pcmData)
        }.toByteArray()

        val audioData = AudioPreprocessor.readWavStream(wavBytes.inputStream())
        assertEquals(sampleRate, audioData.sampleRate)
        assertEquals(1, audioData.channels)
        assertEquals(numSamples, audioData.samples.size)
        assertEquals(0.5f, audioData.samples[0], 0.01f)
    }
}
