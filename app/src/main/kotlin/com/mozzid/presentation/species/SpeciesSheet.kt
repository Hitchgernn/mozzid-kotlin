package com.mozzid.presentation.species

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mozzid.R
import com.mozzid.domain.model.Species
import com.mozzid.presentation.components.GhostButton
import com.mozzid.presentation.components.MozzCard
import com.mozzid.presentation.components.StripedPlaceholder
import com.mozzid.presentation.components.label
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.Motion
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.bg
import com.mozzid.presentation.theme.border
import com.mozzid.presentation.theme.color

/**
 * Species reference card, reachable by tapping a species anywhere in the app.
 *
 * A bottom sheet rather than a screen: it is always supplementary to whatever is
 * behind it, and sliding it away returns you exactly where you were.
 */
@Composable
fun BoxScope.SpeciesSheet(
    species: Species?,
    onDismiss: () -> Unit,
) {
    val c = MozzTheme.colors

    AnimatedVisibility(
        visible = species != null,
        enter = fadeIn(tween(Motion.Sheet)),
        exit = fadeOut(tween(Motion.Sheet)),
        modifier = Modifier.fillMaxSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color(0x99040610))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onDismiss,
                ),
        )
    }

    AnimatedVisibility(
        visible = species != null,
        enter = slideInVertically(tween(Motion.Sheet, easing = Motion.Emphasized)) { it },
        exit = slideOutVertically(tween(Motion.Sheet, easing = Motion.Emphasized)) { it },
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        // Retained through the exit animation so the content does not blank out
        // mid-slide when `species` goes null.
        val shown = remember(species) { species } ?: return@AnimatedVisibility

        // Hugs its content, but never taller than 88% of the screen — a short
        // species card should not stretch into a full-height panel.
        val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f

        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .clip(
                    RoundedCornerShape(
                        topStart = Dimens.sheetRadius,
                        topEnd = Dimens.sheetRadius,
                    ),
                )
                .background(c.surface3)
                .border(
                    1.dp,
                    c.line2,
                    RoundedCornerShape(topStart = Dimens.sheetRadius, topEnd = Dimens.sheetRadius),
                )
                // Swallow taps so they do not reach the dismiss scrim behind.
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .size(width = 44.dp, height = 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(c.line2),
                )
            }

            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.pagePad)
                    .padding(top = 16.dp, bottom = 30.dp),
            ) {
                val photoModifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(20.dp))

                if (shown.photoRes != null) {
                    Image(
                        painter = painterResource(shown.photoRes),
                        contentDescription = shown.scientificName,
                        contentScale = ContentScale.Crop,
                        modifier = photoModifier,
                    )
                } else {
                    StripedPlaceholder(
                        photoModifier,
                        radius = 20.dp,
                        label = "${shown.scientificName} — ${stringResource(R.string.photo_placeholder)}",
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text(shown.scientificName, style = MozzText.species.copy(color = c.text))
                Spacer(Modifier.height(2.dp))
                Text(shown.commonName, style = MozzText.bodySmall.copy(color = c.text3))

                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MozzCard(Modifier.weight(1f), radius = 14.dp) {
                        Column(Modifier.padding(13.dp)) {
                            Text(
                                stringResource(R.string.wingbeat).uppercase(),
                                style = MozzText.overline.copy(
                                    color = c.text4,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                shown.wingbeatRange,
                                style = MozzText.mono.copy(color = c.accent, fontSize = 17.sp),
                            )
                        }
                    }
                    MozzCard(Modifier.weight(1f), radius = 14.dp) {
                        Column(Modifier.padding(13.dp)) {
                            Text(
                                stringResource(R.string.active_hours).uppercase(),
                                style = MozzText.overline.copy(
                                    color = c.text4,
                                    letterSpacing = 0.5.sp,
                                ),
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                shown.activeLabel,
                                style = MozzText.body.copy(
                                    color = c.text,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(shown.severity.bg)
                        .border(1.dp, shown.severity.border, RoundedCornerShape(14.dp))
                        .padding(horizontal = 15.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                ) {
                    Text(
                        shown.severity.glyph,
                        style = MozzText.mono.copy(
                            color = shown.severity.color,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        ),
                    )
                    Column {
                        Text(
                            shown.severity.label().uppercase(),
                            style = MozzText.overline.copy(
                                color = shown.severity.color,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp,
                            ),
                        )
                        Text(
                            shown.diseases,
                            style = MozzText.body.copy(
                                color = c.text,
                                fontWeight = FontWeight.SemiBold,
                            ),
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(R.string.prevention).uppercase(),
                    style = MozzText.overline.copy(color = c.text4),
                )
                Spacer(Modifier.height(9.dp))
                shown.tips.forEach { tip ->
                    Row(
                        Modifier.padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp),
                    ) {
                        Box(
                            Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(c.accent),
                        )
                        Text(tip, style = MozzText.bodySmall.copy(color = c.text2))
                    }
                }

                Spacer(Modifier.height(14.dp))
                GhostButton(
                    label = stringResource(R.string.close),
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
