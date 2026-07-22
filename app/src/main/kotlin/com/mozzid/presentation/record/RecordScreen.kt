package com.mozzid.presentation.record

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mozzid.R
import com.mozzid.presentation.components.GhostButton
import com.mozzid.presentation.components.IconClock
import com.mozzid.presentation.components.IconMic
import com.mozzid.presentation.components.IconSettings
import com.mozzid.presentation.components.OfflineChip
import com.mozzid.presentation.components.SquareIconButton
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.Motion
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import kotlin.math.abs
import kotlin.math.sin

/**
 * The hero screen. One 264dp stage holds all three capture phases so the
 * transition between them reads as one object changing state rather than three
 * screens swapping — that continuity is the whole point of the design.
 */
@Composable
fun RecordScreen(
    state: RecordState,
    onStartHold: () -> Unit,
    onEndHold: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onWavFileSelected: (Uri) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let { onWavFileSelected(it) }
    }
    val c = MozzTheme.colors

    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SquareIconButton(
                onClick = onOpenHistory,
                contentDescription = stringResource(R.string.history),
            ) { IconClock(c.text2) }
            OfflineChip()
            SquareIconButton(
                onClick = onOpenSettings,
                contentDescription = stringResource(R.string.settings),
            ) { IconSettings(c.text2) }
        }

        Spacer(Modifier.height(30.dp))

        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.heard_buzz),
                style = MozzText.display.copy(color = c.text),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.tap_hold),
                style = MozzText.body.copy(color = c.text3),
                textAlign = TextAlign.Center,
            )
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            when (state.phase) {
                RecordPhase.IDLE -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    IdleButton(onStartHold, onEndHold)
                    Spacer(Modifier.height(20.dp))
                    GhostButton(
                        label = stringResource(R.string.upload_wav_demo),
                        onClick = { launcher.launch("audio/*") },
                    )
                }
                RecordPhase.LISTENING -> ListeningStage(state.progress, onEndHold)
                RecordPhase.ANALYZING -> AnalyzingStage()
                RecordPhase.RESULT -> Unit // the result view replaces this screen
            }
        }

        val hint = when (state.phase) {
            RecordPhase.LISTENING -> stringResource(R.string.listening_hold)
            else -> stringResource(R.string.hold_steady)
        }
        Text(
            hint,
            Modifier.fillMaxWidth(),
            style = if (state.phase == RecordPhase.LISTENING) {
                MozzText.bodySmall.copy(color = c.accentSoftText)
            } else {
                MozzText.mono.copy(color = c.text4)
            },
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

/**
 * Resting state: a softly breathing accent orb inside two faint guide rings.
 * The slow 3.4s glow is the only thing moving on an otherwise still screen, which
 * is what marks it as the thing to touch without needing an explicit prompt.
 */
@Composable
private fun IdleButton(onStartHold: () -> Unit, onEndHold: () -> Unit) {
    val c = MozzTheme.colors
    val loop = rememberInfiniteTransition(label = "idle")
    val glow by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(Motion.Glow, easing = Motion.Standard),
            RepeatMode.Reverse,
        ),
        label = "glow",
    )
    val recordLabel = stringResource(R.string.record)

    Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(264.dp)
                .border(1.dp, c.accentMix(14f), CircleShape),
        )
        Box(
            Modifier
                .size(212.dp)
                .border(1.dp, c.accentMix(10f), CircleShape),
        )
        // The glow is a scaling translucent halo rather than a shadow: Compose has
        // no coloured drop-shadow, and a halo also reads on light backgrounds.
        Box(
            Modifier
                .size(184.dp)
                .scale(1f + 0.10f * glow)
                .alpha(0.18f + 0.16f * glow)
                .clip(CircleShape)
                .background(c.accent),
        )
        AccentOrb(
            Modifier
                .semantics { contentDescription = recordLabel }
                .pointerInput(Unit) {
                    detectTapGestures(onPress = {
                        onStartHold()
                        tryAwaitRelease()
                        onEndHold()
                    })
                },
        ) {
            IconMic(c.accentInk, size = 52.dp, filled = true)
        }
    }
}

/**
 * The 184dp accent orb both live phases share. The highlight sits at 38% height
 * rather than centre, which is what gives it the lit-from-above sphere read.
 */
@Composable
private fun AccentOrb(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    val c = MozzTheme.colors
    Box(
        modifier
            .size(184.dp)
            .clip(CircleShape)
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(c.accentHi, c.accent2, c.accent2),
                        center = Offset(size.width * 0.5f, size.height * 0.38f),
                        radius = size.maxDimension * 0.62f,
                    ),
                )
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * Capture in progress. Three staggered rings pulse outward on a 2.4s loop while
 * the 4s progress arc closes around them, so "something is happening" and "how
 * much longer" are two separate, non-competing signals.
 */
@Composable
private fun ListeningStage(progress: Float, onEndHold: () -> Unit) {
    val c = MozzTheme.colors
    val loop = rememberInfiniteTransition(label = "listening")

    Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
        listOf(0 to 16f, Motion.RingStagger to 13f, Motion.RingStagger * 2 to 10f)
            .forEach { (delay, tint) ->
                val t by loop.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        tween(Motion.RingPulse, easing = LinearEasing),
                        RepeatMode.Restart,
                        initialStartOffset = StartOffset(delay),
                    ),
                    label = "ring$delay",
                )
                // mzRing: scale 0.55 → 1.5, opacity 0.55 → 0.
                Box(
                    Modifier
                        .size(190.dp)
                        .scale(0.55f + 0.95f * t)
                        .alpha((1f - t) * 0.55f)
                        .clip(CircleShape)
                        .background(c.accentMix(tint)),
                )
            }

        // 4-second progress arc. Drawn rather than animated: it tracks the state
        // machine's own progress so the ring can never disagree with the capture.
        Canvas(Modifier.size(264.dp)) {
            val stroke = 5.dp.toPx()
            val inset = stroke / 2f + 1.dp.toPx()
            drawArc(
                color = c.line,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke),
            )
            drawArc(
                color = c.accent,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2, size.height - inset * 2),
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }

        AccentOrb(
            Modifier.pointerInput(Unit) {
                detectTapGestures(onPress = { tryAwaitRelease(); onEndHold() })
            },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LevelMeter()
                Spacer(Modifier.height(12.dp))
                Text(
                    "${(progress * 100).toInt()}%",
                    style = MozzText.mono.copy(color = c.accentInk),
                )
            }
        }
    }
}

/** Six bars bouncing out of phase — a stand-in for live input level. */
@Composable
private fun LevelMeter() {
    val c = MozzTheme.colors
    val loop = rememberInfiniteTransition(label = "meter")
    val delays = listOf(0, 120, 240, 360, 180, 300)

    Row(
        Modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        delays.forEach { delay ->
            val h by loop.animateFloat(
                initialValue = 0.28f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(Motion.BarPulse, easing = Motion.Standard),
                    RepeatMode.Reverse,
                    initialStartOffset = StartOffset(delay),
                ),
                label = "bar$delay",
            )
            Box(
                Modifier
                    .width(5.dp)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(3.dp))
                    .background(c.accentInk),
            )
        }
    }
}

/**
 * On-device inference. A frozen spectrogram with a sweeping read-head — the
 * point is to show that real signal work is happening locally, not to spin a
 * generic loader.
 */
@Composable
private fun AnalyzingStage() {
    val c = MozzTheme.colors
    val loop = rememberInfiniteTransition(label = "analyzing")

    // Stable bar shape: a sine ridge so it reads as a frequency signature rather
    // than noise. Seeded once so it does not reshuffle on every recomposition.
    val bars = remember {
        List(32) { i -> 0.15f + 0.60f * abs(sin(i * 0.6)).toFloat() + 0.20f * ((i * 37 % 17) / 17f) }
    }
    val scan by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Motion.Scan, easing = LinearEasing)),
        label = "scan",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .width(280.dp)
                .height(120.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(c.surface3)
                .border(1.dp, c.accentMix(14f), RoundedCornerShape(16.dp)),
        ) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                bars.forEachIndexed { i, height ->
                    val pulse by loop.animateFloat(
                        initialValue = 0.55f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(Motion.SpectroPulse, easing = Motion.Standard),
                            RepeatMode.Reverse,
                            initialStartOffset = StartOffset(i * 30),
                        ),
                        label = "spec$i",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight((height * pulse).coerceIn(0.06f, 1f))
                            .alpha(0.85f)
                            .clip(RoundedCornerShape(1.dp))
                            .background(Brush.verticalGradient(listOf(c.accent, c.accent2))),
                    )
                }
            }
            // Read-head sweeping left to right across the captured clip.
            Canvas(Modifier.fillMaxSize()) {
                val bandWidth = 60.dp.toPx()
                val x = -bandWidth + (size.width + bandWidth * 2) * scan
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            c.accent.copy(alpha = 0f),
                            c.accent.copy(alpha = 0.28f),
                            c.accent.copy(alpha = 0f),
                        ),
                        startX = x,
                        endX = x + bandWidth,
                    ),
                    topLeft = Offset(x, 0f),
                    size = Size(bandWidth, size.height),
                )
            }
        }

        Spacer(Modifier.height(26.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val blink by loop.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(Motion.Dot / 2, easing = LinearEasing),
                    RepeatMode.Reverse,
                ),
                label = "blink",
            )
            Box(
                Modifier
                    .size(6.dp)
                    .alpha(blink)
                    .clip(CircleShape)
                    .background(c.accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.matching),
                style = MozzText.mono.copy(color = c.accentSoftText),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.on_device),
            style = MozzText.caption.copy(color = c.text4),
        )
    }
}
