package com.mozzid.data.audio

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.mozzid.data.permission.PermissionBridge
import com.mozzid.domain.repository.AudioRecorderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Real microphone capture via [AudioRecord], written as 16-bit PCM WAV.
 *
 * The Flutter build recorded AAC/M4A, but the deferred wingbeat model wants raw
 * PCM — capturing it directly means the TFLite swap reads the file as-is instead
 * of decoding first. Clips are ~4s, so the size cost over AAC is negligible.
 *
 * Files land in `cacheDir/wingbeats` and are pruned to the most recent few, since
 * only the classifier reads them and it does so immediately.
 */
class MicAudioRecorder(
    context: Context,
    private val permissions: PermissionBridge,
) : AudioRecorderService {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var recorder: AudioRecord? = null
    private var writeJob: Job? = null
    private var outputFile: File? = null
    private var startedAtMillis = 0L

    override suspend fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    override suspend fun requestPermission(): Boolean =
        hasPermission() || permissions.request(arrayOf(Manifest.permission.RECORD_AUDIO))

    @SuppressLint("MissingPermission") // guarded by the hasPermission() check below
    override suspend fun start() = withContext(Dispatchers.IO) {
        check(hasPermission()) { "Microphone permission denied" }
        releaseRecorder()

        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
        check(minBuffer != AudioRecord.ERROR && minBuffer != AudioRecord.ERROR_BAD_VALUE) {
            "AudioRecord unavailable at ${SAMPLE_RATE}Hz"
        }
        // Oversize the buffer so a slow write never drops wingbeat samples.
        val bufferSize = minBuffer * 2

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            ENCODING,
            bufferSize,
        )
        check(record.state == AudioRecord.STATE_INITIALIZED) {
            record.release()
            "AudioRecord failed to initialise"
        }

        val file = newOutputFile()
        record.startRecording()
        recorder = record
        outputFile = file
        startedAtMillis = System.currentTimeMillis()

        writeJob = scope.launch {
            val buffer = ByteArray(bufferSize)
            file.outputStream().use { out ->
                // Leave a 44-byte gap for the RIFF header — its sizes are only
                // known once capture ends, so stop() patches it in afterwards.
                out.write(ByteArray(WAV_HEADER_BYTES))
                while (isActiveRecording(record)) {
                    val read = record.read(buffer, 0, buffer.size)
                    if (read > 0) out.write(buffer, 0, read)
                }
            }
        }
    }

    override suspend fun stop(): Pair<String, Long> = withContext(Dispatchers.IO) {
        val file = outputFile
        val durationMillis =
            if (startedAtMillis == 0L) 0L else System.currentTimeMillis() - startedAtMillis

        recorder?.let { record ->
            // Flip to STOPPED first so the writer loop exits, then join before
            // releasing — releasing under an in-flight read() crashes natively.
            runCatching { record.stop() }
            writeJob?.cancelAndJoin()
            runCatching { record.release() }
        }
        recorder = null
        writeJob = null
        outputFile = null
        startedAtMillis = 0L

        checkNotNull(file) { "stop() called without a start()" }
        writeWavHeader(file)
        file.absolutePath to durationMillis
    }

    private fun isActiveRecording(record: AudioRecord) =
        record.recordingState == AudioRecord.RECORDSTATE_RECORDING

    private fun releaseRecorder() {
        recorder?.let { runCatching { it.stop() }; runCatching { it.release() } }
        recorder = null
        writeJob?.cancel()
        writeJob = null
    }

    private fun newOutputFile(): File {
        val dir = File(appContext.cacheDir, "wingbeats").apply { mkdirs() }
        // Only the classifier reads these, and it reads immediately — keep a few
        // for debugging and drop the rest so the cache cannot grow unbounded.
        dir.listFiles()
            ?.sortedByDescending { it.lastModified() }
            ?.drop(KEEP_RECENT_CLIPS)
            ?.forEach { it.delete() }
        return File(dir, "wingbeat_${System.currentTimeMillis()}.wav")
    }

    /** Patches the RIFF header now that the PCM payload size is known. */
    private fun writeWavHeader(file: File) {
        val pcmBytes = (file.length() - WAV_HEADER_BYTES).coerceAtLeast(0)
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8

        val header = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt((36 + pcmBytes).toInt())
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)                                   // PCM subchunk size
        header.putShort(1)                                  // format: PCM
        header.putShort(CHANNELS.toShort())
        header.putInt(SAMPLE_RATE)
        header.putInt(byteRate)
        header.putShort((CHANNELS * BITS_PER_SAMPLE / 8).toShort()) // block align
        header.putShort(BITS_PER_SAMPLE.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmBytes.toInt())

        RandomAccessFile(file, "rw").use {
            it.seek(0)
            it.write(header.array())
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44100
        const val CHANNELS = 1
        const val BITS_PER_SAMPLE = 16
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        const val WAV_HEADER_BYTES = 44
        const val KEEP_RECENT_CLIPS = 3
    }
}
