package com.mozzid.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The design's icon set, drawn directly rather than pulled from Material.
 *
 * The design uses one consistent stroked-outline style on a 24-unit grid with
 * ~1.8 stroke weight and round caps; Material's filled defaults would read as a
 * different family sitting next to the custom mascot and severity glyphs. These
 * are line-for-line ports of the SVGs in the design file, so the two stay
 * identical, and they carry no dependency beyond Compose UI.
 *
 * Every icon here is decorative — it always sits beside a text label or inside a
 * button that carries its own `contentDescription`, so none is the sole carrier
 * of meaning.
 */
private const val GRID = 24f

/** Renders [draw] on the design's 24-unit grid, scaled to [size]. */
@Composable
private fun MozzIcon(
    size: Dp,
    modifier: Modifier = Modifier,
    draw: DrawScope.(k: Float) -> Unit,
) {
    Canvas(modifier.size(size)) { draw(this.size.width / GRID) }
}

/**
 * Parses an SVG `d` string on the 24-unit grid and scales it to pixels. Compose
 * paths have no scale operation, so this goes through a matrix transform.
 */
private fun gridPath(pathData: String, k: Float): Path {
    val path = PathParser().parsePathString(pathData).toPath()
    path.transform(androidx.compose.ui.graphics.Matrix().apply { scale(k, k) })
    return path
}

private fun DrawScope.stroke(
    pathData: String,
    color: Color,
    k: Float,
    width: Float = 1.8f,
) = drawPath(
    path = gridPath(pathData, k),
    color = color,
    style = Stroke(width = width * k, cap = StrokeCap.Round, join = StrokeJoin.Round),
)

private fun DrawScope.fill(pathData: String, color: Color, k: Float) =
    drawPath(path = gridPath(pathData, k), color = color)

@Composable
fun IconClock(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) =
    MozzIcon(size, modifier) { k ->
        drawCircle(color, radius = 8.5f * k, center = Offset(12f * k, 12f * k), style = Stroke(1.7f * k))
        stroke("M12 7.5 L12 12 L15 14", color, k, 1.7f)
    }

@Composable
fun IconSettings(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) =
    MozzIcon(size, modifier) { k ->
        drawCircle(color, radius = 9f * k, center = Offset(12f * k, 12f * k), style = Stroke(1.7f * k))
        drawCircle(color, radius = 1.6f * k, center = Offset(12f * k, 12f * k))
        stroke("M12 3 L12 6 M12 18 L12 21 M3 12 L6 12 M18 12 L21 12", color, k, 1.7f)
    }

/** Bottom-nav settings glyph — sliders rather than a gear. */
@Composable
fun IconSliders(color: Color, bg: Color, modifier: Modifier = Modifier, size: Dp = 24.dp) =
    MozzIcon(size, modifier) { k ->
        stroke("M4 7 L20 7 M4 12 L20 12 M4 17 L20 17", color, k)
        listOf(9f to 7f, 15f to 12f, 9f to 17f).forEach { (x, y) ->
            drawCircle(bg, radius = 2.4f * k, center = Offset(x * k, y * k))
            drawCircle(color, radius = 2.4f * k, center = Offset(x * k, y * k), style = Stroke(1.8f * k))
        }
    }

@Composable
fun IconMic(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp, filled: Boolean = false) =
    MozzIcon(size, modifier) { k ->
        if (filled) {
            fill("M9 3 L15 3 L15 15 L9 15 Z", color, k)
        }
        stroke("M9 6 A3 3 0 0 1 15 6 L15 12 A3 3 0 0 1 9 12 Z", color, k)
        stroke("M6 11 A6 6 0 0 0 18 11 M12 17 L12 21", color, k)
    }

@Composable
fun IconChevronRight(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) =
    MozzIcon(size, modifier) { k -> stroke("M9 6 L15 12 L9 18", color, k, 2f) }

@Composable
fun IconChevronLeft(color: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) =
    MozzIcon(size, modifier) { k -> stroke("M15 6 L9 12 L15 18", color, k, 2f) }

@Composable
fun IconShare(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) =
    MozzIcon(size, modifier) { k ->
        stroke("M12 15 L12 4 M12 4 L8 8 M12 4 L16 8", color, k, 1.9f)
        stroke("M5 13 L5 18 A2 2 0 0 0 7 20 L17 20 A2 2 0 0 0 19 18 L19 13", color, k, 1.9f)
    }

@Composable
fun IconSave(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) =
    MozzIcon(size, modifier) { k ->
        stroke("M5 4 L16 4 L19 7 L19 20 L5 20 Z", color, k)
        stroke("M8 4 L8 9 L14 9 L14 4 M8 20 L8 14 L16 14 L16 20", color, k)
    }

@Composable
fun IconDownload(color: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) =
    MozzIcon(size, modifier) { k ->
        stroke("M12 4 L12 15 M12 15 L8 11 M12 15 L16 11", color, k, 1.9f)
        stroke("M5 18 L19 18", color, k, 1.9f)
    }

@Composable
fun IconPin(color: Color, modifier: Modifier = Modifier, size: Dp = 22.dp) =
    MozzIcon(size, modifier) { k ->
        stroke("M12 21 C12 21 19 14.5 19 9 A7 7 0 1 0 5 9 C5 14.5 12 21 12 21 Z", color, k)
        drawCircle(color, radius = 2.5f * k, center = Offset(12f * k, 9f * k), style = Stroke(1.8f * k))
    }

@Composable
fun IconSpeaker(color: Color, modifier: Modifier = Modifier, size: Dp = 15.dp) =
    MozzIcon(size, modifier) { k ->
        fill("M4 9 L8 9 L13 5 L13 19 L8 15 L4 15 Z", color, k)
        stroke("M16 9 A3.5 3.5 0 0 1 16 15", color, k)
    }

@Composable
fun IconCheck(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) =
    MozzIcon(size, modifier) { k -> stroke("M5 12.5 L9 16.5 L19 6.5", color, k, 3f) }

@Composable
fun IconClose(color: Color, modifier: Modifier = Modifier, size: Dp = 18.dp) =
    MozzIcon(size, modifier) { k -> stroke("M6 6 L18 18 M18 6 L6 18", color, k, 2f) }

/** Record-tab glyph: a ring with a solid centre dot. */
@Composable
fun IconRecordDot(ring: Color, dot: Color, modifier: Modifier = Modifier, size: Dp = 30.dp) =
    MozzIcon(size, modifier) { k ->
        val r = this.size.width / 2f
        drawCircle(ring, radius = r - 1f * k, center = center, style = Stroke(2f * k))
        drawCircle(dot, radius = 5.5f * k, center = center)
    }

/** Dashed hairline used under the "preview a notification" affordance. */
internal fun DrawScope.dashedRoundRect(color: Color, strokeWidth: Float, dash: Float) {
    drawRoundRect(
        color = color,
        style = Stroke(
            width = strokeWidth,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
        ),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(13.dp.toPx()),
    )
}
