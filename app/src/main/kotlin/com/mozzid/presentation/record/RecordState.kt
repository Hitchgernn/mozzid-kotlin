package com.mozzid.presentation.record

import com.mozzid.domain.model.ClassificationResult

enum class RecordPhase { IDLE, LISTENING, ANALYZING, RESULT }

enum class RecordError { NONE, MIC_DENIED, FAILED }

data class RecordState(
    val phase: RecordPhase = RecordPhase.IDLE,
    val progress: Float = 0f, // 0..1 during listening
    val result: ClassificationResult? = null,
    val error: RecordError = RecordError.NONE,
)
