package com.mozzid.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mozzid.domain.classifier.AudioSample
import com.mozzid.domain.classifier.SpeciesClassifier
import com.mozzid.domain.model.Detection
import com.mozzid.domain.repository.AudioRecorderService
import com.mozzid.domain.repository.DetectionRepository
import com.mozzid.domain.repository.LocationService
import com.mozzid.domain.sync.SyncService
import kotlinx.coroutines.Job
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

        val result = classifier.classify(AudioSample(filePath = path, durationMillis = duration))
        _state.value = _state.value.copy(phase = RecordPhase.RESULT, result = result)

        // Save: real timestamp + best-effort GPS. Never blocks the result UI.
        val fix = runCatching { location.currentFix() }.getOrNull()
        val saved = detections.add(
            Detection(
                speciesId = result.primary.id,
                confidence = result.confidence,
                wingbeatHz = result.wingbeatHz,
                timestampMillis = System.currentTimeMillis(),
                latitude = fix?.latitude,
                longitude = fix?.longitude,
            ),
        )
        runCatching { sync.pushDetection(saved) } // best-effort, no-op when offline
    }

    fun reset() {
        listenJob?.cancel()
        _state.value = RecordState()
    }
}
