package com.mozzid.presentation.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mozzid.R
import com.mozzid.domain.model.Detection
import com.mozzid.domain.model.Species
import com.mozzid.domain.repository.SpeciesRepository
import com.mozzid.domain.stats.DateRange
import com.mozzid.domain.stats.DetectionStats
import com.mozzid.domain.stats.LogFilter
import com.mozzid.presentation.components.FilterChip
import com.mozzid.presentation.components.Mascot
import com.mozzid.presentation.components.MascotSize
import com.mozzid.presentation.components.MozzCard
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.Motion
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.dotColor
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The log. Framed as personal surveillance rather than a plain list — the map
 * and stats card are what turn a pile of readings into "where and when mosquitoes
 * show up around me", which is the reason to keep recording.
 */
@Composable
fun HistoryScreen(
    log: List<Detection>,
    filtered: List<Detection>,
    stats: DetectionStats,
    filter: LogFilter,
    species: SpeciesRepository,
    onFilterChange: (LogFilter) -> Unit,
    onOpenSpecies: (Species) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors

    if (log.isEmpty()) {
        EmptyLog(modifier)
        return
    }

    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.pagePad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(stringResource(R.string.history), style = MozzText.title.copy(color = c.text))
                Text(
                    "${stats.total} ${stringResource(R.string.logged)}",
                    style = MozzText.mono.copy(color = c.text4),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        item {
            DetectionMap(
                detections = filtered,
                species = species,
                modifier = Modifier
                    .padding(horizontal = Dimens.pagePad)
                    .fillMaxWidth()
                    .height(186.dp),
            )
            Spacer(Modifier.height(14.dp))
        }

        item {
            StatsCard(
                stats = stats,
                species = species,
                modifier = Modifier
                    .padding(horizontal = Dimens.pagePad)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.pagePad),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    label = stringResource(R.string.all_species),
                    selected = filter.isAllSpecies,
                    onClick = { onFilterChange(filter.copy(speciesId = null)) },
                )
                species.all().forEach { s ->
                    FilterChip(
                        label = s.scientificName.substringBefore(' '),
                        selected = filter.speciesId == s.id,
                        onClick = { onFilterChange(filter.copy(speciesId = s.id)) },
                    )
                }
            }
            Spacer(Modifier.height(9.dp))
            Row(
                Modifier.padding(horizontal = Dimens.pagePad),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    label = stringResource(R.string.all_time),
                    selected = filter.range == DateRange.ALL,
                    onClick = { onFilterChange(filter.copy(range = DateRange.ALL)) },
                    mono = true,
                )
                FilterChip(
                    label = stringResource(R.string.this_week),
                    selected = filter.range == DateRange.WEEK,
                    onClick = { onFilterChange(filter.copy(range = DateRange.WEEK)) },
                    mono = true,
                )
            }
            Spacer(Modifier.height(14.dp))
        }

        items(filtered, key = { it.id ?: it.timestampMillis }) { detection ->
            val s = species.byId(detection.speciesId)
            LogRow(
                detection = detection,
                species = s,
                onClick = { s?.let(onOpenSpecies) },
                modifier = Modifier
                    .padding(horizontal = Dimens.pagePad)
                    .fillMaxWidth(),
            )
            Spacer(Modifier.height(9.dp))
        }
    }
}

/**
 * Stylised offline map. Deliberately not a tile map: tiles need a network, and
 * the app has to work with no signal. This shows relative geography — where
 * detections sit against each other — which is the question the log answers.
 *
 * Coordinates are projected into the box from the bounding box of the fixes
 * themselves, so a cluster in one neighbourhood fills the frame rather than
 * collapsing into a dot.
 */
@Composable
private fun DetectionMap(
    detections: List<Detection>,
    species: SpeciesRepository,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    val located = detections.filter { it.latitude != null && it.longitude != null }

    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    // Pins drop in staggered on entry; keyed to the filter result so changing a
    // filter replays the drop rather than silently swapping pins.
    var shown by remember(located.map { it.id }) { mutableStateOf(false) }
    LaunchedEffect(located) { shown = true }

    val minLat = located.minOfOrNull { it.latitude!! } ?: 0.0
    val maxLat = located.maxOfOrNull { it.latitude!! } ?: 0.0
    val minLon = located.minOfOrNull { it.longitude!! } ?: 0.0
    val maxLon = located.maxOfOrNull { it.longitude!! } ?: 0.0
    val latSpan = (maxLat - minLat).takeIf { it > 1e-6 } ?: 1.0
    val lonSpan = (maxLon - minLon).takeIf { it > 1e-6 } ?: 1.0

    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface3)
            .border(1.dp, c.line, RoundedCornerShape(20.dp))
            .onSizeChanged { boxSize = it },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Dotted survey grid.
            val step = 22.dp.toPx()
            var y = step / 2
            while (y < size.height) {
                var x = step / 2
                while (x < size.width) {
                    drawCircle(c.accent.copy(alpha = 0.09f), radius = 1.dp.toPx(), center = Offset(x, y))
                    x += step
                }
                y += step
            }
            // Abstract road bands — enough structure to read as a place.
            drawRect(
                color = c.fill,
                topLeft = Offset(size.width * 0.16f, -size.height),
                size = Size(34.dp.toPx(), size.height * 3),
            )
            drawRect(
                color = c.fill,
                topLeft = Offset(size.width * 0.62f, -size.height),
                size = Size(20.dp.toPx(), size.height * 3),
            )
            drawRect(
                color = c.fill,
                topLeft = Offset(0f, size.height * 0.44f),
                size = Size(size.width, 26.dp.toPx()),
            )
        }

        located.forEachIndexed { index, detection ->
            val fx = ((detection.longitude!! - minLon) / lonSpan).toFloat()
            val fy = 1f - ((detection.latitude!! - minLat) / latSpan).toFloat()
            // Inset so pins never touch the rounded corners.
            val x = 0.12f + fx * 0.76f
            val y = 0.18f + fy * 0.64f

            val scale by animateFloatAsState(
                targetValue = if (shown) 1f else 0f,
                animationSpec = tween(
                    durationMillis = Motion.PinDrop,
                    delayMillis = index * Motion.PinStagger,
                    easing = Motion.Emphasized,
                ),
                label = "pin$index",
            )

            // Offset, not padding: the anchor point puts the pin's tip on the
            // coordinate, so the x/y here are routinely negative near the edges
            // and padding rejects negative values outright.
            val pinShape = RoundedCornerShape(
                topStart = 11.dp,
                topEnd = 11.dp,
                bottomEnd = 11.dp,
                bottomStart = 0.dp,
            )
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            x = (boxSize.width * x).roundToInt() - 11.dp.roundToPx(),
                            y = (boxSize.height * y).roundToInt() - 22.dp.roundToPx(),
                        )
                    }
                    .size(22.dp)
                    .scale(scale)
                    .rotate(-45f)
                    .clip(pinShape)
                    .background(species.byId(detection.speciesId)?.dotColor ?: c.accent)
                    .border(2.dp, c.bg, pinShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.size(6.dp).rotate(45f).clip(CircleShape).background(c.bg))
            }
        }

        Text(
            stringResource(R.string.detections_near),
            style = MozzText.monoSmall.copy(color = c.text4),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(c.bg.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

/** Total, peak window, and the species split as a single stacked bar plus legend. */
@Composable
private fun StatsCard(
    stats: DetectionStats,
    species: SpeciesRepository,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    MozzCard(modifier) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text("${stats.total}", style = MozzText.metricLarge.copy(color = c.text))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.total_detections),
                        style = MozzText.tiny.copy(color = c.text4),
                    )
                }
                stats.peakWindow?.let { peak ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            peak,
                            style = MozzText.bodySmall.copy(
                                color = c.accent,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            stringResource(R.string.most_active),
                            style = MozzText.monoSmall.copy(color = c.text4),
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(9.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                stats.breakdown.forEach { share ->
                    Box(
                        Modifier
                            .weight(share.percent.coerceAtLeast(1).toFloat())
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .background(species.byId(share.speciesId)?.dotColor ?: c.accent),
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.breakdown.forEach { share ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(species.byId(share.speciesId)?.dotColor ?: c.accent),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            species.byId(share.speciesId)?.scientificName?.substringBefore(' ')
                                ?: share.speciesId,
                            style = MozzText.caption.copy(color = c.text2),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("${share.percent}%", style = MozzText.caption.copy(color = c.text4))
                    }
                }
            }
        }
    }
}

@Composable
private fun LogRow(
    detection: Detection,
    species: Species?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(15.dp))
            .background(c.surface2)
            .border(1.dp, c.line, RoundedCornerShape(15.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(11.dp)
                .clip(CircleShape)
                .background(species?.dotColor ?: c.accent),
        )
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                species?.scientificName ?: detection.speciesId,
                style = MozzText.speciesSmall.copy(color = c.text),
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatTime(detection.timestampMillis),
                    style = MozzText.tiny.copy(color = c.text4),
                )
                detection.locationLabel?.let {
                    Text(" • $it", style = MozzText.tiny.copy(color = c.text4))
                }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${detection.confidence}%", style = MozzText.mono.copy(color = c.text2))
            Spacer(Modifier.height(2.dp))
            Text(
                formatRelativeDay(detection.timestampMillis),
                style = MozzText.monoSmall.copy(color = c.text4),
            )
        }
    }
}

@Composable
private fun EmptyLog(modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Mascot(MascotSize.Small)
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(R.string.empty_title),
            style = MozzText.title.copy(color = c.text2, fontSize = MozzText.display.fontSize),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.empty_body),
            style = MozzText.bodySmall.copy(color = c.text3),
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

/** "Tonight" / "Yesterday" / "N days ago" — friendlier than a bare date. */
@Composable
private fun formatRelativeDay(millis: Long): String {
    val days = remember(millis) {
        val then = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()
        listOf(Calendar.HOUR_OF_DAY, Calendar.MINUTE, Calendar.SECOND, Calendar.MILLISECOND)
            .forEach { field -> then.set(field, 0); now.set(field, 0) }
        abs((now.timeInMillis - then.timeInMillis) / (24L * 60 * 60 * 1000)).toInt()
    }
    return when (days) {
        0 -> stringResource(R.string.tonight)
        else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(millis))
    }
}
