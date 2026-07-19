package com.mozzid.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mozzid.Bootstrap
import com.mozzid.R
import com.mozzid.domain.model.AppLanguage
import com.mozzid.domain.model.AppSettings
import com.mozzid.domain.model.Detection
import com.mozzid.domain.model.ThemeBrightness
import com.mozzid.presentation.record.RecordPhase
import com.mozzid.presentation.record.RecordViewModel
import com.mozzid.presentation.theme.AppAccent
import com.mozzid.presentation.theme.Dimens
import com.mozzid.presentation.theme.MozzTheme
import com.mozzid.presentation.theme.dotColor
import kotlinx.coroutines.launch
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
            stringResource(R.string.app_name),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = MozzType.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 30.sp,
                color = c.text,
            ),
        )
        Text(
            stringResource(R.string.heard_buzz),
            style = androidx.compose.ui.text.TextStyle(
                fontFamily = MozzType.Sans, fontSize = 14.sp, color = c.text3,
            ),
        )

        Spacer(Modifier.height(12.dp))

        // Temporary switches so the live language and theme swap can be verified
        // before the real Settings screen lands.
        SettingsProbe(boot)

        Spacer(Modifier.height(16.dp))

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
            "${stringResource(R.string.history)} · ${log.size}",
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

/**
 * Throwaway control strip: flips language, brightness and accent so the live
 * settings wiring is verifiable today. The real Settings screen replaces this.
 */
@Composable
private fun SettingsProbe(boot: Bootstrap) {
    val c = MozzTheme.colors
    val scope = rememberCoroutineScope()
    val settings by boot.settingsRepository.watch().collectAsState(initial = AppSettings())

    fun edit(transform: (AppSettings) -> AppSettings) {
        scope.launch { boot.settingsRepository.update(transform) }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.gap)) {
        ProbeChip(
            label = settings.language.tag.uppercase(),
            onClick = {
                edit {
                    it.copy(
                        language = if (it.language == AppLanguage.ENGLISH) {
                            AppLanguage.INDONESIAN
                        } else {
                            AppLanguage.ENGLISH
                        },
                    )
                }
            },
        )
        ProbeChip(
            label = stringResource(
                if (settings.brightness == ThemeBrightness.DARK) R.string.dark else R.string.light,
            ),
            onClick = {
                edit {
                    it.copy(
                        brightness = if (it.brightness == ThemeBrightness.DARK) {
                            ThemeBrightness.LIGHT
                        } else {
                            ThemeBrightness.DARK
                        },
                    )
                }
            },
        )
        ProbeChip(
            label = stringResource(R.string.accent_label),
            onClick = {
                edit {
                    val accents = AppAccent.entries
                    val next = accents[(AppAccent.fromName(it.accentId).ordinal + 1) % accents.size]
                    it.copy(accentId = next.name)
                }
            },
        )
    }
    Spacer(Modifier.height(4.dp))
    Text(
        stringResource(R.string.offline),
        style = androidx.compose.ui.text.TextStyle(
            fontFamily = MozzType.Mono, fontSize = 11.sp, color = c.text4,
        ),
    )
}

@Composable
private fun ProbeChip(label: String, onClick: () -> Unit) {
    val c = MozzTheme.colors
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.chipRadius))
            .background(c.fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        style = androidx.compose.ui.text.TextStyle(
            fontFamily = MozzType.Mono, fontSize = 11.sp, color = c.accentSoftText,
        ),
    )
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
