package com.mozzid.presentation.record

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mozzid.domain.classifier.AudioSample
import com.mozzid.domain.classifier.SpeciesClassifier
import com.mozzid.domain.model.Detection
import com.mozzid.domain.repository.AudioRecorderService
import com.mozzid.domain.repository.DetectionRepository
import com.mozzid.domain.repository.GeoFix
import com.mozzid.domain.repository.LocationService
import com.mozzid.domain.sync.SyncService
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Drives the capture flow: idle → listening (hold ~4s) → analyzing → result.
 * Talks only to the injected services and the [SpeciesClassifier] interface —
 * never a concrete model. Ports the Flutter RecordController state machine.
 */
class RecordViewModel(
    private val recorder: AudioRecorderService,
    private val classifier: SpeciesClassifier,
    private val location: LocationService,
    private val detections: DetectionRepository,
    private val sync: SyncService,
) : ViewModel() {

    private val _state = MutableStateFlow(RecordState())
    val state: StateFlow<RecordState> = _state.asStateFlow()

    private var listenJob: Job? = null

    /** GPS fix resolved during the result screen, attached on save. */
    private var pendingFix: GeoFix? = null

    /** When the clip was actually captured — not when the user got round to saving. */
    private var capturedAt: Long = 0L

    companion object {
        const val CAPTURE_MILLIS = 4000L
        private const val TICK_MILLIS = 16L
    }

    /** Begin the press-and-hold capture. No-op unless idle. */
    fun startHold() {
        if (_state.value.phase != RecordPhase.IDLE) return
        listenJob = viewModelScope.launch {
            if (!recorder.hasPermission() && !recorder.requestPermission()) {
                _state.value = _state.value.copy(error = RecordError.MIC_DENIED)
                return@launch
            }
            try {
                recorder.start()
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = RecordError.FAILED)
                return@launch
            }

            _state.value = RecordState(phase = RecordPhase.LISTENING, progress = 0f)
            val startedAt = System.currentTimeMillis()
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startedAt
                val p = (elapsed.toFloat() / CAPTURE_MILLIS).coerceIn(0f, 1f)
                _state.value = _state.value.copy(progress = p)
                if (p >= 1f) break
                delay(TICK_MILLIS)
            }
            finishListening()
        }
    }

    /** Released before the hold completed: cancel back to idle. */
    fun endHold() {
        if (_state.value.phase != RecordPhase.LISTENING) return
        if (_state.value.progress >= 1f) return // already completing
        listenJob?.cancel()
        viewModelScope.launch { runCatching { recorder.stop() } }
        _state.value = RecordState()
    }

    private suspend fun finishListening() {
        val (path, duration) = runCatching { recorder.stop() }.getOrElse { "" to CAPTURE_MILLIS }
        _state.value = _state.value.copy(phase = RecordPhase.ANALYZING)

        // Run classification and GPS concurrently — both finish before result is shown,
        // so pendingFix is always populated when the user taps Save.
        coroutineScope {
            val resultDeferred = async {
                classifier.classify(AudioSample(filePath = path, durationMillis = duration))
            }
            val fixDeferred = async {
                runCatching {
                    if (location.requestPermission()) location.currentFix() else null
                }.getOrNull()
            }
            val result = resultDeferred.await()
            pendingFix = fixDeferred.await()
            capturedAt = System.currentTimeMillis()
            _state.value = _state.value.copy(phase = RecordPhase.RESULT, result = result)
        }
    }

    /**
     * Persist the result the user is looking at. Explicit rather than automatic:
     * the design makes "Save to log" a deliberate action, and "Record again"
     * discards. Uses [capturedAt], not now — saving is not when it was heard.
     */
    fun saveResult(onSaved: () -> Unit = {}) {
        val result = _state.value.result ?: return
        viewModelScope.launch {
            val saved = detections.add(
                Detection(
                    speciesId = result.primary.id,
                    confidence = result.confidence,
                    wingbeatHz = result.wingbeatHz,
                    timestampMillis = capturedAt,
                    latitude = pendingFix?.latitude,
                    longitude = pendingFix?.longitude,
                ),
            )
            runCatching { sync.pushDetection(saved) } // best-effort, no-op when offline
            reset()
            onSaved()
        }
    }

    /** Acknowledge a surfaced error so the same one is not reported twice. */
    fun clearError() {
        if (_state.value.error == RecordError.NONE) return
        _state.value = _state.value.copy(error = RecordError.NONE)
    }

    fun analyzeWavFile(uri: Uri, context: Context) {
        if (_state.value.phase != RecordPhase.IDLE) return
        viewModelScope.launch {
            try {
                val destFile = File(context.cacheDir, "demo_upload.wav")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                } ?: run {
                    _state.value = _state.value.copy(error = RecordError.FAILED)
                    return@launch
                }

                val duration = runCatching {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(context, uri)
                    val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    retriever.release()
                    durationStr?.toLongOrNull() ?: CAPTURE_MILLIS
                }.getOrDefault(CAPTURE_MILLIS)

                _state.value = _state.value.copy(phase = RecordPhase.ANALYZING)

                // Run classification and GPS concurrently — both finish before result is shown.
                coroutineScope {
                    val resultDeferred = async {
                        classifier.classify(
                            AudioSample(
                                filePath = destFile.absolutePath,
                                durationMillis = duration,
                            ),
                        )
                    }
                    val fixDeferred = async {
                        runCatching {
                            if (location.requestPermission()) location.currentFix() else null
                        }.getOrNull()
                    }
                    val result = resultDeferred.await()
                    pendingFix = fixDeferred.await()
                    capturedAt = System.currentTimeMillis()
                    _state.value = _state.value.copy(phase = RecordPhase.RESULT, result = result)
                }
            } catch (_: Exception) {
                _state.value = _state.value.copy(error = RecordError.FAILED)
            }
        }
    }

    fun reset() {
        listenJob?.cancel()
        pendingFix = null
        _state.value = RecordState()
    }
}
