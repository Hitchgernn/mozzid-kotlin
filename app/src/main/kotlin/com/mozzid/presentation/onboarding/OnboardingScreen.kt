package com.mozzid.presentation.onboarding

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mozzid.R
import com.mozzid.presentation.components.IconMic
import com.mozzid.presentation.components.IconPin
import com.mozzid.presentation.components.Mascot
import com.mozzid.presentation.components.MascotSize
import com.mozzid.presentation.components.PrimaryButton
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme

/** One onboarding page. Permission primers only appear on the last one. */
private data class OnboardPage(
    val titleRes: Int,
    val bodyRes: Int,
    val ctaRes: Int,
    val showPermissions: Boolean = false,
)

private val pages = listOf(
    OnboardPage(R.string.onboard_catch_title, R.string.onboard_catch_body, R.string.next),
    OnboardPage(R.string.onboard_offline_title, R.string.onboard_offline_body, R.string.next),
    OnboardPage(
        R.string.onboard_perms_title,
        R.string.onboard_perms_body,
        R.string.start_listening,
        showPermissions = true,
    ),
)

/**
 * Three-screen intro. The permission primer is a deliberate page rather than a
 * cold system dialog on launch: explaining *why* the mic is needed before Android
 * asks is what makes the difference between a grant and a permanent denial.
 */
@Composable
fun OnboardingScreen(
    step: Int,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    val page = pages[step.coerceIn(pages.indices)]

    Column(
        modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(horizontal = 30.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                stringResource(R.string.skip),
                Modifier
                    .clickable(role = Role.Button, onClick = onSkip)
                    .padding(12.dp),
                style = MozzText.body.copy(color = c.text4),
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(MascotSize.Large)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(page.titleRes),
                style = MozzText.display.copy(color = c.text),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(page.bodyRes),
                Modifier.widthIn(max = 280.dp),
                style = MozzText.body.copy(color = c.text3),
                textAlign = TextAlign.Center,
            )

            if (page.showPermissions) {
                Spacer(Modifier.height(22.dp))
                Column(
                    Modifier.widthIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PermissionPrimer(
                        title = stringResource(R.string.mic_title),
                        note = stringResource(R.string.mic_note),
                    ) { IconMic(c.accent, size = 22.dp) }
                    PermissionPrimer(
                        title = stringResource(R.string.loc_title),
                        note = stringResource(R.string.loc_note),
                    ) { IconPin(c.accent, size = 22.dp) }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            pages.indices.forEach { i ->
                val width by animateDpAsState(
                    if (i == step) 22.dp else 7.dp,
                    tween(300),
                    label = "dot$i",
                )
                Box(
                    Modifier
                        .size(width = width, height = 7.dp)
                        .clip(CircleShape)
                        .background(if (i == step) c.accent else c.line2),
                )
            }
        }

        Spacer(Modifier.height(22.dp))
        PrimaryButton(
            label = stringResource(page.ctaRes),
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun PermissionPrimer(title: String, note: String, icon: @Composable () -> Unit) {
    val c = MozzTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.surface)
            .border(1.dp, c.accentMix(20f), RoundedCornerShape(14.dp))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MozzText.bodySmall.copy(color = c.text, fontWeight = FontWeight.SemiBold),
            )
            Text(note, style = MozzText.tiny.copy(color = c.text4))
        }
        Text(
            stringResource(R.string.allow),
            style = MozzText.caption.copy(color = c.accent, fontWeight = FontWeight.SemiBold),
        )
    }
}
