package com.mozzid.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mozzid.R
import com.mozzid.domain.model.ActiveWindow
import com.mozzid.presentation.theme.Motion
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme

/** Localised biting-window label. Species *names* stay Latin; this is plain copy. */
@Composable
fun ActiveWindow.label(): String = stringResource(
    when (this) {
        ActiveWindow.DAY -> R.string.active_day
        ActiveWindow.NIGHT -> R.string.active_night
        ActiveWindow.DUSK_TO_DAWN -> R.string.active_dusk_dawn
    },
)

/**
 * Diagonal-hatch stand-in for a species photograph. Deliberately reads as an
 * empty slot rather than a broken image — no real photographs ship yet.
 */
@Composable
fun StripedPlaceholder(
    modifier: Modifier = Modifier,
    radius: Dp = 18.dp,
    label: String = stringResource(R.string.photo_placeholder),
) {
    val c = MozzTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .border(1.dp, c.line, RoundedCornerShape(radius)),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(c.surface2)
            val step = 14.dp.toPx()
            val band = step / 2f
            var x = -size.height
            while (x < size.width + size.height) {
                drawLine(
                    color = c.surface3,
                    start = Offset(x, size.height),
                    end = Offset(x + size.height, 0f),
                    strokeWidth = band,
                )
                x += step
            }
        }
        Text(label, style = MozzText.monoSmall.copy(color = c.faint))
    }
}

/**
 * Circular confidence gauge. Animates up from zero on first show so the number
 * lands as a measurement being taken rather than a static badge — and it is
 * always paired with the numeric percentage beside it.
 */
@Composable
fun ConfidenceRing(
    percent: Int,
    modifier: Modifier = Modifier,
    diameter: Dp = 58.dp,
    strokeWidth: Dp = 5.dp,
) {
    val c = MozzTheme.colors
    val sweep by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(Motion.ScreenFade * 2, easing = Motion.Emphasized),
        label = "confidence",
    )
    Canvas(modifier.size(diameter)) {
        val stroke = strokeWidth.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = c.line,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(stroke),
        )
        drawArc(
            color = c.accent,
            startAngle = -90f,
            sweepAngle = 360f * sweep,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
    }
}

/**
 * Six-bar frequency sparkline beside the wingbeat reading. The lit bars mark
 * where the measurement sits inside the species' known range.
 */
@Composable
fun FrequencySparkline(modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    val heights = listOf(0.30f to false, 0.70f to true, 1.0f to true, 0.60f to true, 0.35f to false, 0.20f to false)
    Row(
        modifier.height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        heights.forEach { (h, lit) ->
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(h)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (lit) c.accent else c.line2),
            )
        }
    }
}

/** Horizontal proportion bar, used for the runner-up confidence. */
@Composable
fun ProportionBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    fill: Color = MozzTheme.colors.text4,
) {
    val c = MozzTheme.colors
    Box(
        modifier
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(c.line2),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(fill),
        )
    }
}
