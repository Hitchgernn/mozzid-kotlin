package com.mozzid.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.Motion

/**
 * The MozzID mascot — a friendly mosquito, drawn as vector paths on a 120×120
 * grid so it stays crisp at every size the design uses (44 / 84 / 112 dp).
 *
 * Two independent loops, matching the design's `mzFloat` and `mzWing`/`mzWingR`
 * keyframes: the whole body drifts up and rocks slightly over 4s, while the wings
 * flap on a much faster 0.5s cycle. The wings are mirrored rather than
 * synchronised — each rotates outward from its own hinge — which is what stops
 * the flap reading as a single rigid shape.
 *
 * Colours derive from the live accent rather than the design's hardcoded teal, so
 * the mascot recolours with the rest of the app when the accent changes.
 */
@Composable
fun Mascot(size: Dp, modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    val loop = rememberInfiniteTransition(label = "mascot")

    // mzFloat: translateY 0 → -9 → 0 with a -1° → 1° rock, over 4s.
    val floatPhase by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.MascotFloat, easing = Motion.Standard),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "float",
    )

    // mzWing: scaleY 1 → 0.7 with the hinge angle opening 28° → 34°.
    val wing by loop.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(Motion.WingFlap, easing = Motion.Standard),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "wing",
    )

    val wingFill = c.accent.copy(alpha = 0.22f)
    val wingStroke = c.accent.copy(alpha = 0.40f)
    val limb = c.accent2
    val abdomen = lerpColor(c.accent2, c.accent, 0.45f)
    val thorax = lerpColor(c.accent2, c.accent, 0.72f)
    val head = c.accentHi
    val eye = c.accentInk

    Canvas(modifier.size(size)) {
        // Everything below is authored against the design's 120×120 viewBox.
        val k = this.size.width / 120f

        translate(top = -9f * floatPhase * k) {
            rotate(degrees = -1f + 2f * floatPhase, pivot = Offset(60f * k, 70f * k)) {
                drawWing(k, hingeX = 46f, angle = -(28f + 6f * wing), squash = 1f - 0.3f * wing, fill = wingFill, stroke = wingStroke)
                drawWing(k, hingeX = 74f, angle = (28f + 6f * wing), squash = 1f - 0.3f * wing, fill = wingFill, stroke = wingStroke)

                // Six legs, splayed back and down from the thorax.
                val legs = listOf(
                    floatArrayOf(60f, 70f, 52f, 104f),
                    floatArrayOf(60f, 72f, 68f, 104f),
                    floatArrayOf(56f, 68f, 40f, 96f),
                    floatArrayOf(64f, 68f, 80f, 96f),
                )
                legs.forEach { (x1, y1, x2, y2) ->
                    drawLine(
                        color = limb,
                        start = Offset(x1 * k, y1 * k),
                        end = Offset(x2 * k, y2 * k),
                        strokeWidth = 2.5f * k,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }

                drawOval(
                    color = abdomen,
                    topLeft = Offset(45f * k, 58f * k),
                    size = Size(30f * k, 48f * k),
                )
                // Two banding stripes — the visual cue that reads as "mosquito".
                val band = eye.copy(alpha = 0.25f)
                drawLine(band, Offset(47f * k, 80f * k), Offset(73f * k, 80f * k), 2f * k)
                drawLine(band, Offset(48f * k, 90f * k), Offset(72f * k, 90f * k), 2f * k)

                drawCircle(thorax, radius = 13f * k, center = Offset(60f * k, 58f * k))

                // Proboscis, tucked under the head.
                drawLine(
                    color = limb,
                    start = Offset(60f * k, 52f * k),
                    end = Offset(60f * k, 66f * k),
                    strokeWidth = 2f * k,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )

                drawCircle(head, radius = 15f * k, center = Offset(60f * k, 42f * k))

                // Eyes with offset catchlights — the whole reason it reads as friendly.
                drawCircle(eye, radius = 5.5f * k, center = Offset(54f * k, 41f * k))
                drawCircle(eye, radius = 5.5f * k, center = Offset(66f * k, 41f * k))
                drawCircle(Color.White, radius = 1.9f * k, center = Offset(55.6f * k, 39.4f * k))
                drawCircle(Color.White, radius = 1.9f * k, center = Offset(67.6f * k, 39.4f * k))

                // Antennae.
                listOf(
                    floatArrayOf(53f, 30f, 46f, 16f),
                    floatArrayOf(67f, 30f, 74f, 16f),
                ).forEach { (x1, y1, x2, y2) ->
                    drawLine(
                        color = head,
                        start = Offset(x1 * k, y1 * k),
                        end = Offset(x2 * k, y2 * k),
                        strokeWidth = 2f * k,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
                drawCircle(head, radius = 2.6f * k, center = Offset(46f * k, 15f * k))
                drawCircle(head, radius = 2.6f * k, center = Offset(74f * k, 15f * k))
            }
        }
    }
}

/**
 * One wing: a 54×22 ellipse hinged at its inner edge, rotated by [angle] and
 * squashed vertically by [squash]. Rotating and scaling about the hinge (rather
 * than the ellipse centre) is what makes the flap look like it pivots at the body.
 */
private fun DrawScope.drawWing(
    k: Float,
    hingeX: Float,
    angle: Float,
    squash: Float,
    fill: Color,
    stroke: Color,
) {
    val pivot = Offset(hingeX * k, 44f * k)
    rotate(degrees = angle, pivot = pivot) {
        scale(scaleX = 1f, scaleY = squash, pivot = pivot) {
            val topLeft = Offset((hingeX - 27f) * k, (44f - 11f) * k)
            val size = Size(54f * k, 22f * k)
            drawOval(color = fill, topLeft = topLeft, size = size)
            drawOval(color = stroke, topLeft = topLeft, size = size, style = Stroke(width = 1f * k))
        }
    }
}

/** Straight-line blend between two colours; used to derive mascot body tones. */
private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t,
)

/** The three sizes the design uses. */
object MascotSize {
    val Large = 112.dp
    val Small = 84.dp
    val Tiny = 44.dp
}
