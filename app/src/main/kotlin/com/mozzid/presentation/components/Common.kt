package com.mozzid.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mozzid.R
import com.mozzid.domain.model.Severity
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.Motion
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.bg
import com.mozzid.presentation.theme.border
import com.mozzid.presentation.theme.color
import com.mozzid.presentation.theme.iconBg

/** Localised risk label. Always rendered next to [Severity.glyph], never alone. */
@Composable
fun Severity.label(): String = stringResource(
    when (this) {
        Severity.HIGH -> R.string.high_risk
        Severity.MODERATE -> R.string.moderate_risk
        Severity.LOW -> R.string.low_risk
    },
)

/** The standard raised panel: surface fill, hairline border, 18dp radius. */
@Composable
fun MozzCard(
    modifier: Modifier = Modifier,
    background: Color = MozzTheme.colors.surface,
    radius: androidx.compose.ui.unit.Dp = Dimens.cardRadius,
    content: @Composable () -> Unit,
) {
    val c = MozzTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(radius))
            .background(background)
            .border(BorderStroke(1.dp, c.line), RoundedCornerShape(radius)),
    ) { content() }
}

/** Hairline divider used between rows inside a settings card. */
@Composable
fun CardDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MozzTheme.colors.line),
    )
}

/**
 * Filter pill. Selection is carried by fill, border *and* text colour together —
 * a single tinted background would be too subtle at night on a dim screen.
 */
@Composable
fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    mono: Boolean = false,
) {
    val c = MozzTheme.colors
    val bg by animateColorAsState(
        if (selected) c.accentMix(14f) else c.fill,
        tween(Motion.Toast),
        label = "chipBg",
    )
    val fg by animateColorAsState(
        if (selected) c.accentSoftText else c.text3,
        tween(Motion.Toast),
        label = "chipFg",
    )
    Text(
        text = label,
        style = (if (mono) MozzText.mono else MozzText.bodySmall).copy(
            color = fg,
            fontWeight = FontWeight.Medium,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.chipRadius))
            .background(bg)
            .border(
                BorderStroke(1.dp, if (selected) c.accentMix(30f) else c.line),
                RoundedCornerShape(Dimens.chipRadius),
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 8.dp),
    )
}

/** Primary call to action — the accent gradient button. */
@Composable
fun PrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 54.dp,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = MozzTheme.colors
    Row(
        modifier
            .height(height)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.verticalGradient(listOf(c.accent, c.accent2)))
            .clickable(role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MozzText.button.copy(color = c.accentInk))
    }
}

/** Secondary action — outlined, low emphasis. */
@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = MozzTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(c.fill)
            .border(BorderStroke(1.dp, c.line2), RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke()
        Text(label, style = MozzText.mono.copy(color = c.text2))
    }
}

/** 44dp square icon button — the minimum comfortable one-handed target. */
@Composable
fun SquareIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 44.dp,
    height: androidx.compose.ui.unit.Dp = 44.dp,
    icon: @Composable () -> Unit,
) {
    val c = MozzTheme.colors
    Box(
        modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(14.dp))
            .background(c.fill)
            .border(BorderStroke(1.dp, c.line2), RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClickLabel = contentDescription, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { icon() }
}

/** iOS-style switch, 48×28 with a 22dp thumb, matching the design. */
@Composable
fun MozzToggle(checked: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    val track by animateColorAsState(
        if (checked) c.accent2 else c.line2,
        tween(200),
        label = "track",
    )
    val offset by animateDpAsState(
        if (checked) 20.dp else 0.dp,
        tween(200, easing = Motion.Emphasized),
        label = "thumb",
    )
    Box(
        modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(role = Role.Switch, onClick = onToggle)
            .padding(3.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .padding(start = offset)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

/**
 * Risk banner. Carries the meaning three ways at once — glyph, written label and
 * colour — so it survives colour blindness and a dimmed night screen. Never
 * reduce this to colour alone.
 */
@Composable
fun SeverityBanner(severity: Severity, diseases: String, modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(Dimens.cardRadius))
            .background(severity.bg)
            .border(BorderStroke(1.dp, severity.border), RoundedCornerShape(Dimens.cardRadius))
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(severity.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                severity.glyph,
                style = MozzText.metric.copy(
                    color = severity.color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                ),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                severity.label().uppercase(),
                style = MozzText.overline.copy(
                    color = severity.color,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                "${stringResource(R.string.carries)} $diseases",
                style = MozzText.body.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Small "Works offline" pill in the record header. */
@Composable
fun OfflineChip(modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    Row(
        modifier
            .clip(RoundedCornerShape(Dimens.chipRadius))
            .background(c.accentMix(9f))
            .border(BorderStroke(1.dp, c.accentMix(22f)), RoundedCornerShape(Dimens.chipRadius))
            .padding(horizontal = 13.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(c.accent))
        Text(
            stringResource(R.string.offline),
            style = MozzText.tiny.copy(
                color = c.accentSoftText,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/** Full-bleed scrim used behind the species sheet. */
@Composable
fun Scrim(onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(Color(0x99040610))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDismiss,
            ),
    )
}
