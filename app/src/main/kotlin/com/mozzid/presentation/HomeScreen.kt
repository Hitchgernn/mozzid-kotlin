package com.mozzid.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mozzid.Bootstrap
import com.mozzid.domain.model.Detection
import com.mozzid.presentation.record.RecordPhase
import com.mozzid.presentation.record.RecordViewModel
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.MozzType

/**
 * Skeleton home screen: drives the real record→analyze→result→save flow through
 * [RecordViewModel] and shows the live History from the repository. Full screens
 * (onboarding, history map/stats, species card, settings) port next — this proves
 * the seams, DI, theming, DB, and state machine end-to-end.
 */
@Composable
fun HomeScreen(boot: Bootstrap) {
    val c = MozzTheme.colors
    val vm = remember {
        RecordViewModel(
            recorder = boot.audioRecorder,
            classifier = boot.classifier,
            location = boot.location,
            detections = boot.detectionRepository,
            sync = boot.sync,
        )
    }
    val state by vm.state.collectAsState()
    val log by boot.detectionRepository.watch().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(c.bg)
            .padding(Dimens.pagePad),
    ) {
        Text(
            "MozzID",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = MozzType.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                color = c.text,
            ),
        )
        Text(
            "Identify a mosquito by its wingbeat",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = MozzType.Sans, fontSize = 14.sp, color = c.text3,
            ),
        )

        Spacer(Modifier.height(24.dp))

        // Record button (press and hold ~4s).
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(if (state.phase == RecordPhase.IDLE) c.accent else c.accent2)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                vm.startHold()
                                tryAwaitRelease()
                                vm.endHold()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    phaseLabel(state.phase, state.progress),
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = MozzType.Mono, fontSize = 13.sp,
                        color = c.accentInk, fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        state.result?.let { r ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.cardRadius))
                    .background(c.surface)
                    .padding(16.dp),
            ) {
                Text(
                    "${r.primary.severity.glyph}  ${r.primary.commonName}",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = MozzType.Serif, fontSize = 20.sp,
                        color = c.text, fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    "${r.primary.scientificName} · ${r.confidence}% · ${r.wingbeatHz} Hz",
                    style = androidx.compose.ui.text.TextStyle(
                        fontFamily = MozzType.Mono, fontSize = 12.sp, color = c.accentSoftText,
                    ),
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "History · ${log.size}",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = MozzType.Mono, fontSize = 12.sp, color = c.text4,
            ),
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(Dimens.gap)) {
            items(log) { d -> LogRow(d, boot) }
        }
    }
}

@Composable
private fun LogRow(d: Detection, boot: Bootstrap) {
    val c = MozzTheme.colors
    val species = boot.speciesRepository.byId(d.speciesId)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Dimens.tileRadius))
            .background(c.surface2)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(species?.dotColor ?: c.accent),
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                species?.commonName ?: d.speciesId,
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = MozzType.Sans, fontSize = 14.sp,
                    color = c.text, fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                d.locationLabel ?: "—",
                style = androidx.compose.ui.text.TextStyle(
                    fontFamily = MozzType.Sans, fontSize = 12.sp, color = c.text3,
                ),
            )
        }
        Text(
            "${d.confidence}%",
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = MozzType.Mono, fontSize = 13.sp, color = c.accentSoftText,
            ),
        )
    }
}

private fun phaseLabel(phase: RecordPhase, progress: Float): String = when (phase) {
    RecordPhase.IDLE -> "HOLD"
    RecordPhase.LISTENING -> "${(progress * 100).toInt()}%"
    RecordPhase.ANALYZING -> "…"
    RecordPhase.RESULT -> "DONE"
}
