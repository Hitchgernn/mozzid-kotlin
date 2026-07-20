package com.mozzid.presentation.morning

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mozzid.R
import com.mozzid.domain.model.Detection
import com.mozzid.domain.repository.SpeciesRepository
import com.mozzid.presentation.components.IconClose
import com.mozzid.presentation.components.Mascot
import com.mozzid.presentation.components.MascotSize
import com.mozzid.presentation.components.MozzCard
import com.mozzid.presentation.components.PrimaryButton
import com.mozzid.presentation.components.SquareIconButton
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.dotColor
import java.util.Calendar

/** One 45-minute slot of the overnight window, 10PM through 7AM. */
private const val SLOTS = 12
private const val WINDOW_START_HOUR = 22

/**
 * The payoff for leaving background listening on: what happened while you slept.
 *
 * Built from the real log rather than a canned summary — it counts only
 * detections inside last night's 10PM–7AM window, so an empty night honestly
 * shows zero rather than inventing activity.
 */
@Composable
fun MorningSummaryScreen(
    log: List<Detection>,
    species: SpeciesRepository,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors

    val overnight = remember(log) { log.filter { it.timestampMillis in lastNightWindow() } }
    val counts = remember(overnight) {
        overnight.groupingBy { it.speciesId }.eachCount().entries.sortedByDescending { it.value }
    }
    val timeline = remember(overnight) { buildTimeline(overnight) }

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.surface3, c.bg))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Mascot(MascotSize.Large)
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.good_morning).uppercase(),
                    style = MozzText.mono.copy(color = c.accent, letterSpacing = 1.sp),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    pluralStringResource(
                        R.plurals.detected_overnight,
                        overnight.size,
                        overnight.size,
                    ),
                    style = MozzText.display.copy(color = c.text),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
                counts.take(3).forEach { (speciesId, count) ->
                    val s = species.byId(speciesId)
                    MozzCard(Modifier.weight(1f), radius = Dimens.tileRadius) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "$count",
                                style = MozzText.metricLarge.copy(
                                    color = s?.dotColor ?: c.accent,
                                    fontSize = 26.sp,
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                s?.scientificName?.substringBefore(' ') ?: speciesId,
                                style = MozzText.speciesSmall.copy(
                                    color = c.text3,
                                    fontSize = 11.5.sp,
                                ),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            MozzCard(Modifier.fillMaxWidth(), radius = Dimens.tileRadius) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.overnight_timeline).uppercase(),
                        style = MozzText.overline.copy(color = c.text4),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        timeline.forEach { slot ->
                            val colour = slot.speciesId
                                ?.let { species.byId(it)?.dotColor }
                                ?: c.line
                            Box(
                                Modifier
                                    .weight(1f)
                                    // An empty slot still draws a 4% stub so the
                                    // timeline reads as a full night, not a gap.
                                    .fillMaxHeight(if (slot.count == 0) 0.04f else slot.height)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(colour),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        listOf("10PM", "1AM", "4AM", "7AM").forEach {
                            Text(it, style = MozzText.monoSmall.copy(color = c.text4))
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                label = stringResource(R.string.got_it),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(30.dp))
        }

        SquareIconButton(
            onClick = onDismiss,
            contentDescription = stringResource(R.string.close),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(22.dp),
            width = 38.dp,
            height = 38.dp,
        ) { IconClose(c.text2) }
    }
}

private data class TimelineSlot(
    val count: Int,
    val height: Float,
    /** Dominant species in this slot, or null when nothing was heard. */
    val speciesId: String?,
)

/** Last night's 10PM → 7AM window, as an absolute millisecond range. */
private fun lastNightWindow(): LongRange {
    val end = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 7)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        // Before 7AM the "night" that just happened is still in progress.
        if (timeInMillis > System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, -1)
    }
    val start = (end.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, WINDOW_START_HOUR)
    }
    return start.timeInMillis..end.timeInMillis
}

/** Buckets the night into [SLOTS] equal slices, scaled against the busiest one. */
private fun buildTimeline(overnight: List<Detection>): List<TimelineSlot> {
    val window = lastNightWindow()
    val span = (window.last - window.first).coerceAtLeast(1)
    val buckets = Array(SLOTS) { mutableListOf<String>() }

    overnight.forEach { d ->
        val index = (((d.timestampMillis - window.first).toDouble() / span) * SLOTS)
            .toInt()
            .coerceIn(0, SLOTS - 1)
        buckets[index] += d.speciesId
    }

    val busiest = buckets.maxOfOrNull { it.size } ?: 0
    return buckets.map { slot ->
        TimelineSlot(
            count = slot.size,
            height = if (busiest == 0) 0f else slot.size.toFloat() / busiest,
            speciesId = slot.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
        )
    }
}
