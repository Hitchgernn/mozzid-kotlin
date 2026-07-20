package com.mozzid.presentation.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mozzid.R
import com.mozzid.domain.model.AppLanguage
import com.mozzid.domain.model.AppSettings
import com.mozzid.domain.model.Severity
import com.mozzid.domain.model.ThemeBrightness
import com.mozzid.presentation.components.CardDivider
import com.mozzid.presentation.components.IconCheck
import com.mozzid.presentation.components.IconChevronRight
import com.mozzid.presentation.components.IconClock
import com.mozzid.presentation.components.IconDownload
import com.mozzid.presentation.components.Mascot
import com.mozzid.presentation.components.MascotSize
import com.mozzid.presentation.components.MozzCard
import com.mozzid.presentation.components.MozzToggle
import com.mozzid.presentation.components.dashedRoundRect
import com.mozzid.presentation.theme.AppAccent
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.color
import com.mozzid.presentation.theme.iconBg

/**
 * Settings. Appearance sits first because the accent and brightness swap is the
 * app's most visible affordance, and every control here applies immediately —
 * nothing is staged behind a save button.
 */
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onOpenMorning: () -> Unit,
    onPreviewNotification: () -> Unit,
    onExport: () -> Unit,
    onReplayIntro: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    val dark = settings.brightness == ThemeBrightness.DARK

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.pagePad),
    ) {
        Text(stringResource(R.string.settings), style = MozzText.title.copy(color = c.text))
        Spacer(Modifier.height(18.dp))

        MozzCard(Modifier.fillMaxWidth()) {
            Column {
                SettingRow(
                    title = stringResource(R.string.appearance),
                    subtitle = stringResource(R.string.appearance_note),
                ) {
                    SegmentedPair(
                        leftLabel = stringResource(R.string.dark),
                        rightLabel = stringResource(R.string.light),
                        leftSelected = dark,
                        onLeft = { onUpdate { it.copy(brightness = ThemeBrightness.DARK) } },
                        onRight = { onUpdate { it.copy(brightness = ThemeBrightness.LIGHT) } },
                    )
                }
                CardDivider()
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.accent_label),
                        style = MozzText.rowTitle.copy(color = c.text),
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        stringResource(R.string.accent_note),
                        style = MozzText.caption.copy(color = c.text4),
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                        AppAccent.entries.forEach { accent ->
                            AccentSwatch(
                                accent = accent,
                                selected = AppAccent.fromName(settings.accentId) == accent,
                                dark = dark,
                                onClick = { onUpdate { it.copy(accentId = accent.name) } },
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(Dimens.gap))

        MozzCard(Modifier.fillMaxWidth()) {
            Column {
                SettingRow(
                    title = stringResource(R.string.language),
                    subtitle = stringResource(R.string.lang_note),
                ) {
                    SegmentedPair(
                        leftLabel = "EN",
                        rightLabel = "ID",
                        leftSelected = settings.language == AppLanguage.ENGLISH,
                        onLeft = { onUpdate { it.copy(language = AppLanguage.ENGLISH) } },
                        onRight = { onUpdate { it.copy(language = AppLanguage.INDONESIAN) } },
                    )
                }
                CardDivider()
                SettingRow(
                    title = stringResource(R.string.voice_out),
                    subtitle = stringResource(R.string.voice_note),
                    onClick = { onUpdate { it.copy(voiceOutput = !it.voiceOutput) } },
                ) {
                    MozzToggle(
                        checked = settings.voiceOutput,
                        onToggle = { onUpdate { it.copy(voiceOutput = !it.voiceOutput) } },
                    )
                }
                CardDivider()
                SettingRow(
                    title = stringResource(R.string.bg_listen),
                    subtitle = stringResource(R.string.bg_note),
                    onClick = { onUpdate { it.copy(backgroundListening = !it.backgroundListening) } },
                ) {
                    MozzToggle(
                        checked = settings.backgroundListening,
                        onToggle = {
                            onUpdate { it.copy(backgroundListening = !it.backgroundListening) }
                        },
                    )
                }
            }
        }

        if (settings.backgroundListening) {
            Spacer(Modifier.height(Dimens.gap))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.tileRadius))
                    .background(
                        Brush.linearGradient(listOf(c.accentMix(12f), c.accentMix(3f))),
                    )
                    .border(1.dp, c.accentMix(22f), RoundedCornerShape(Dimens.tileRadius))
                    .clickable(role = Role.Button, onClick = onOpenMorning)
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Mascot(MascotSize.Tiny)
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.morning_ready),
                        style = MozzText.bodySmall.copy(
                            color = c.text,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                    Text(
                        stringResource(R.string.view_summary),
                        style = MozzText.caption.copy(color = c.accentSoftText),
                    )
                }
                IconChevronRight(c.accent)
            }
        }

        SectionLabel(stringResource(R.string.notifications))
        NotificationPreviewCard(
            severity = Severity.HIGH,
            title = stringResource(R.string.notif1_title),
            body = stringResource(R.string.notif1_body),
        )
        Spacer(Modifier.height(9.dp))
        NotificationPreviewCard(
            severity = Severity.LOW,
            title = stringResource(R.string.notif2_title),
            body = stringResource(R.string.notif2_body),
        )
        Spacer(Modifier.height(9.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .drawBehind {
                    dashedRoundRect(c.accentMix(35f), 1.dp.toPx(), 6.dp.toPx())
                }
                .clickable(role = Role.Button, onClick = onPreviewNotification),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.preview_notif),
                style = MozzText.bodySmall.copy(
                    color = c.accent,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }

        SectionLabel(stringResource(R.string.data))
        ActionRow(stringResource(R.string.export_csv), onExport) { IconDownload(c.text2) }
        Spacer(Modifier.height(9.dp))
        ActionRow(stringResource(R.string.replay_intro), onReplayIntro) { IconChevronRight(c.text2) }

        Spacer(Modifier.height(22.dp))
        Text(
            "MozzID · on-device · v1.0",
            Modifier.fillMaxWidth(),
            style = MozzText.monoSmall.copy(color = c.faint),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit,
) {
    val c = MozzTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MozzText.rowTitle.copy(color = c.text))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MozzText.caption.copy(color = c.text4))
        }
        trailing()
    }
}

/** Two-option segmented control (dark/light, EN/ID). */
@Composable
private fun SegmentedPair(
    leftLabel: String,
    rightLabel: String,
    leftSelected: Boolean,
    onLeft: () -> Unit,
    onRight: () -> Unit,
) {
    val c = MozzTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(11.dp))
            .background(c.bg)
            .border(1.dp, c.line, RoundedCornerShape(11.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        listOf(leftLabel to true, rightLabel to false).forEach { (label, isLeft) ->
            val selected = leftSelected == isLeft
            Text(
                label,
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) c.accent else Color.Transparent)
                    .clickable(role = Role.RadioButton) { if (isLeft) onLeft() else onRight() }
                    .padding(horizontal = 13.dp, vertical = 7.dp),
                style = MozzText.caption.copy(
                    color = if (selected) c.accentInk else c.text3,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
        }
    }
}

/**
 * Accent chip. The selected one gets a contrasting ring *and* a check mark —
 * a ring alone would be the only cue, and it disappears on the accent closest
 * to the background.
 */
@Composable
private fun AccentSwatch(
    accent: AppAccent,
    selected: Boolean,
    dark: Boolean,
    onClick: () -> Unit,
) {
    val ring = if (selected) {
        if (dark) Color.White else Color(0xFF0F1720)
    } else {
        Color.Transparent
    }
    Box(
        Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(accent.color)
            .border(2.dp, ring, RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton, onClickLabel = accent.name, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) IconCheck(accent.ink)
    }
}

@Composable
private fun SectionLabel(text: String) {
    val c = MozzTheme.colors
    Spacer(Modifier.height(22.dp))
    Text(
        text.uppercase(),
        Modifier.padding(start = 4.dp, bottom = 10.dp),
        style = MozzText.overline.copy(color = c.text4),
    )
}

/** A mock of how a local notification will look, shown inline in settings. */
@Composable
private fun NotificationPreviewCard(severity: Severity, title: String, body: String) {
    val c = MozzTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(c.surface2)
            .border(1.dp, c.line, RoundedCornerShape(15.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(severity.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            if (severity == Severity.HIGH) {
                Text(
                    severity.glyph,
                    style = MozzText.mono.copy(
                        color = severity.color,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            } else {
                IconClock(severity.color, size = 18.dp)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MozzText.bodySmall.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
            Spacer(Modifier.height(2.dp))
            Text(body, style = MozzText.caption.copy(color = c.text3))
        }
    }
}

@Composable
private fun ActionRow(label: String, onClick: () -> Unit, trailing: @Composable () -> Unit) {
    val c = MozzTheme.colors
    MozzCard(Modifier.fillMaxWidth(), radius = 15.dp) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                label,
                style = MozzText.body.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
            trailing()
        }
    }
}
