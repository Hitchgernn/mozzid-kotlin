package com.mozzid.presentation.record

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mozzid.R
import com.mozzid.domain.model.ClassificationResult
import com.mozzid.domain.model.Species
import com.mozzid.presentation.components.ConfidenceRing
import com.mozzid.presentation.components.FrequencySparkline
import com.mozzid.presentation.components.GhostButton
import com.mozzid.presentation.components.IconChevronLeft
import com.mozzid.presentation.components.IconSave
import com.mozzid.presentation.components.IconShare
import com.mozzid.presentation.components.IconSpeaker
import com.mozzid.presentation.components.MozzCard
import com.mozzid.presentation.components.PrimaryButton
import com.mozzid.presentation.components.ProportionBar
import com.mozzid.presentation.components.SeverityBanner
import com.mozzid.presentation.components.SquareIconButton
import com.mozzid.presentation.components.StripedPlaceholder
import com.mozzid.presentation.components.label
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * The payoff screen. Ordered by what the user actually needs first: risk, then
 * which species, then how sure we are — the identification is a *claim*, so the
 * confidence and the runner-up are given equal visual weight to the answer.
 */
@Composable
fun ResultScreen(
    result: ClassificationResult,
    voiceOutput: Boolean,
    onRecordAgain: () -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onOpenSpecies: (Species) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    val species = result.primary

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.pagePad),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GhostButton(
                label = stringResource(R.string.record_again),
                onClick = onRecordAgain,
                leading = { IconChevronLeft(c.text2) },
            )
            if (voiceOutput) SpeakingChip()
        }

        Spacer(Modifier.height(16.dp))
        SeverityBanner(species.severity, species.diseases, Modifier.fillMaxWidth())

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            StripedPlaceholder(Modifier.size(88.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    stringResource(R.string.identified_as).uppercase(),
                    style = MozzText.overline.copy(color = c.text4),
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    species.scientificName,
                    style = MozzText.species.copy(color = c.text),
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = { onOpenSpecies(species) },
                    ),
                )
                Spacer(Modifier.height(4.dp))
                Text(species.commonName, style = MozzText.mono.copy(color = c.text3))
            }
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            MozzCard(Modifier.weight(1f)) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ConfidenceRing(result.confidence)
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("${result.confidence}%", style = MozzText.metric.copy(color = c.text))
                        Text(
                            stringResource(R.string.confidence),
                            style = MozzText.monoSmall.copy(color = c.text4),
                        )
                    }
                }
            }
            MozzCard(Modifier.weight(1f)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${result.wingbeatHz} Hz", style = MozzText.metric.copy(color = c.text))
                    Text(
                        stringResource(R.string.wingbeat),
                        style = MozzText.monoSmall.copy(color = c.text4),
                    )
                    Spacer(Modifier.height(8.dp))
                    FrequencySparkline(Modifier.fillMaxWidth())
                }
            }
        }

        Spacer(Modifier.height(Dimens.gap))
        ActiveTimeCheck(species)

        Spacer(Modifier.height(16.dp))
        Text(species.note, style = MozzText.bodySmall.copy(color = c.text2))

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.could_also).uppercase(),
            style = MozzText.overline.copy(color = c.text4),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface2)
                .border(1.dp, c.line, RoundedCornerShape(14.dp))
                .clickable(role = Role.Button) { onOpenSpecies(result.runner) }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                result.runner.scientificName,
                style = MozzText.speciesSmall.copy(color = c.text2),
                modifier = Modifier.weight(1f),
            )
            Text(
                "${result.runnerConfidence}%",
                style = MozzText.mono.copy(color = c.text3),
            )
            ProportionBar(result.runnerConfidence / 100f, Modifier.width(60.dp))
        }

        Spacer(Modifier.height(22.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(11.dp)) {
            SquareIconButton(
                onClick = onShare,
                contentDescription = stringResource(R.string.share_created),
                width = 56.dp,
                height = 54.dp,
            ) { IconShare(c.text2) }
            PrimaryButton(
                label = stringResource(R.string.save_log),
                onClick = onSave,
                modifier = Modifier.weight(1f),
                leading = { IconSave(c.accentInk) },
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Live indicator shown while the result is being spoken aloud. */
@Composable
private fun SpeakingChip() {
    val c = MozzTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(Dimens.chipRadius))
            .background(c.accentMix(10f))
            .border(1.dp, c.accentMix(22f), RoundedCornerShape(Dimens.chipRadius))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        IconSpeaker(c.accent)
        Text(
            stringResource(R.string.speaking),
            style = MozzText.tiny.copy(color = c.accentSoftText, fontWeight = FontWeight.SemiBold),
        )
    }
}

/**
 * Cross-checks the detection against the species' known biting window.
 *
 * A daytime biter heard at night is the case worth flagging — it is the most
 * likely sign of a misidentification, so the app says so rather than letting a
 * confident-looking number stand unchallenged.
 */
@Composable
private fun ActiveTimeCheck(species: Species, modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val consistent = species.activeWindow.includesHour(hour)

    val tint = if (consistent) c.accent else Color(0xFFFFCF6B)
    val time = remember { SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date()) }
    val window = species.activeWindow.label()

    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.tileRadius))
            .background(tint.copy(alpha = 0.08f))
            .border(1.dp, tint.copy(alpha = 0.24f), RoundedCornerShape(Dimens.tileRadius))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.padding(top = 1.dp)) {
            com.mozzid.presentation.components.IconClock(tint, size = 20.dp)
        }
        Column {
            Text(
                stringResource(if (consistent) R.string.consistent_hours else R.string.unusual_hour),
                style = MozzText.bodySmall.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(
                    if (consistent) R.string.consistent_hours_note else R.string.unusual_hour_note,
                    time,
                    window,
                ),
                style = MozzText.caption.copy(color = c.text3),
            )
        }
    }
}
