package com.mozzid.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mozzid.Bootstrap
import com.mozzid.R
import com.mozzid.domain.model.AppSettings
import com.mozzid.domain.model.Species
import com.mozzid.domain.stats.LogFilter
import com.mozzid.domain.stats.applyFilters
import com.mozzid.domain.stats.computeStats
import com.mozzid.presentation.components.IconClock
import com.mozzid.presentation.components.IconMic
import com.mozzid.presentation.components.IconRecordDot
import com.mozzid.presentation.components.IconSliders
import com.mozzid.presentation.history.HistoryScreen
import com.mozzid.presentation.morning.MorningSummaryScreen
import com.mozzid.presentation.onboarding.OnboardingScreen
import com.mozzid.presentation.record.RecordError
import com.mozzid.presentation.record.RecordPhase
import com.mozzid.presentation.record.RecordScreen
import com.mozzid.presentation.record.RecordViewModel
import com.mozzid.presentation.record.ResultScreen
import com.mozzid.presentation.species.SpeciesSheet
import com.mozzid.presentation.theme.Motion
import com.mozzid.presentation.theme.MozzText
import com.mozzid.presentation.theme.MozzTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class Tab { HISTORY, RECORD, SETTINGS }

/**
 * The app shell: three tabs, plus the overlays that can appear above any of them
 * (species sheet, morning summary, onboarding, toast, notification banner).
 *
 * Navigation is flat and state-driven rather than a nav graph — there are only
 * three destinations and every overlay is a boolean, so a graph would add
 * indirection without buying anything. Back behaviour stays predictable because
 * each overlay dismisses to exactly where it opened from.
 */
@Composable
fun MozzApp(boot: Bootstrap, settings: AppSettings) {
    val c = MozzTheme.colors
    val scope = rememberCoroutineScope()

    val vm = remember {
        RecordViewModel(
            recorder = boot.audioRecorder,
            classifier = boot.classifier,
            location = boot.location,
            detections = boot.detectionRepository,
            sync = boot.sync,
        )
    }
    val recordState by vm.state.collectAsState()
    val log by boot.detectionRepository.watch().collectAsState(initial = emptyList())

    var tab by remember { mutableStateOf(Tab.RECORD) }
    var filter by remember { mutableStateOf(LogFilter()) }
    var sheetSpecies by remember { mutableStateOf<Species?>(null) }
    var morningOpen by remember { mutableStateOf(false) }
    var onboardStep by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var notificationVisible by remember { mutableStateOf(false) }

    fun update(transform: (AppSettings) -> AppSettings) {
        scope.launch { boot.settingsRepository.update(transform) }
    }

    fun showToast(message: String) {
        toast = message
    }

    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2200)
            toast = null
        }
    }
    LaunchedEffect(notificationVisible) {
        if (notificationVisible) {
            delay(4500)
            notificationVisible = false
        }
    }

    // The result view takes over the record tab entirely.
    val showingResult = tab == Tab.RECORD && recordState.phase == RecordPhase.RESULT

    val savedMessage = stringResource(R.string.saved_to_log)
    val sharedMessage = stringResource(R.string.share_created)
    val exportedMessage = stringResource(R.string.exported)
    val micDeniedMessage = stringResource(R.string.mic_denied)
    val recordFailedMessage = stringResource(R.string.recording_failed)

    // A refused mic or a recorder that will not start has to say so — otherwise
    // holding the button appears to do nothing at all.
    LaunchedEffect(recordState.error) {
        when (recordState.error) {
            RecordError.MIC_DENIED -> showToast(micDeniedMessage)
            RecordError.FAILED -> showToast(recordFailedMessage)
            RecordError.NONE -> return@LaunchedEffect
        }
        vm.clearError()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(bottom = 84.dp),
        ) {
            when {
                showingResult -> ResultScreen(
                    result = recordState.result!!,
                    voiceOutput = settings.voiceOutput,
                    onRecordAgain = vm::reset,
                    onSave = {
                        vm.saveResult {
                            tab = Tab.HISTORY
                            showToast(savedMessage)
                        }
                    },
                    onShare = { showToast(sharedMessage) },
                    onOpenSpecies = { sheetSpecies = it },
                )

                tab == Tab.RECORD -> RecordScreen(
                    state = recordState,
                    onStartHold = vm::startHold,
                    onEndHold = vm::endHold,
                    onOpenHistory = { tab = Tab.HISTORY },
                    onOpenSettings = { tab = Tab.SETTINGS },
                )

                tab == Tab.HISTORY -> {
                    val now = remember(log) { System.currentTimeMillis() }
                    HistoryScreen(
                        log = log,
                        filtered = remember(log, filter, now) { applyFilters(log, filter, now) },
                        stats = remember(log) { computeStats(log) },
                        filter = filter,
                        species = boot.speciesRepository,
                        onFilterChange = { filter = it },
                        onOpenSpecies = { sheetSpecies = it },
                    )
                }

                tab == Tab.SETTINGS -> com.mozzid.presentation.settings.SettingsScreen(
                    settings = settings,
                    onUpdate = ::update,
                    onOpenMorning = { morningOpen = true },
                    onPreviewNotification = { notificationVisible = true },
                    onExport = { showToast(exportedMessage) },
                    onReplayIntro = {
                        onboardStep = 0
                        update { it.copy(onboardingComplete = false) }
                    },
                )
            }
        }

        BottomNav(
            current = tab,
            recordActive = tab == Tab.RECORD && !showingResult,
            onSelect = { tab = it },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        SpeciesSheet(species = sheetSpecies, onDismiss = { sheetSpecies = null })

        AnimatedVisibility(
            visible = morningOpen,
            enter = fadeIn(tween(Motion.ScreenFade)),
            exit = fadeOut(tween(Motion.ScreenFade)),
        ) {
            MorningSummaryScreen(
                log = log,
                species = boot.speciesRepository,
                onDismiss = { morningOpen = false },
                modifier = Modifier.statusBarsPadding(),
            )
        }

        AnimatedVisibility(
            visible = !settings.onboardingComplete,
            enter = fadeIn(tween(Motion.Pop)),
            exit = fadeOut(tween(Motion.Pop)),
        ) {
            OnboardingScreen(
                step = onboardStep,
                onNext = {
                    if (onboardStep < 2) {
                        onboardStep++
                    } else {
                        update { it.copy(onboardingComplete = true) }
                    }
                },
                onSkip = { update { it.copy(onboardingComplete = true) } },
                modifier = Modifier.statusBarsPadding(),
            )
        }

        NotificationBanner(
            visible = notificationVisible,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        )

        Toast(
            message = toast,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp),
        )
    }
}

/**
 * Three destinations, the middle one weighted as the primary action. Icons are
 * always paired with their label — an icon-only bar would be guesswork the first
 * time, and this app is used half-awake in the dark.
 */
@Composable
private fun BottomNav(
    current: Tab,
    recordActive: Boolean,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = MozzTheme.colors
    Column(
        modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, c.bg, c.bg))),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.line),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 34.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NavItem(
                label = stringResource(R.string.history),
                selected = current == Tab.HISTORY,
                onClick = { onSelect(Tab.HISTORY) },
                modifier = Modifier.weight(1f),
            ) { tint -> IconClock(tint, size = 24.dp) }

            NavItem(
                label = stringResource(R.string.record),
                selected = recordActive,
                onClick = { onSelect(Tab.RECORD) },
                modifier = Modifier.weight(1f),
            ) { tint ->
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (recordActive) c.accentMix(15f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    IconRecordDot(ring = tint, dot = tint, size = 30.dp)
                }
            }

            NavItem(
                label = stringResource(R.string.settings),
                selected = current == Tab.SETTINGS,
                onClick = { onSelect(Tab.SETTINGS) },
                modifier = Modifier.weight(1f),
            ) { tint -> IconSliders(tint, c.bg, size = 24.dp) }
        }
    }
}

@Composable
private fun NavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (tint: Color) -> Unit,
) {
    val c = MozzTheme.colors
    val tint = if (selected) c.accent else c.text4
    Column(
        modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                role = Role.Tab,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        icon(tint)
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            style = MozzText.monoSmall.copy(
                color = tint,
                fontWeight = FontWeight.Medium,
                fontFamily = MozzText.body.fontFamily,
            ),
        )
    }
}

/** Transient confirmation, bottom-centre above the nav bar. */
@Composable
private fun Toast(message: String?, modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(tween(Motion.Toast)) + scaleIn(tween(Motion.Toast), initialScale = 0.9f),
        exit = fadeOut(tween(Motion.Toast)),
        modifier = modifier,
    ) {
        Row(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(c.surface.copy(alpha = 0.95f))
                .border(1.dp, c.accentMix(25f), RoundedCornerShape(14.dp))
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.accent))
            Text(
                message.orEmpty(),
                style = MozzText.bodySmall.copy(color = c.text, fontWeight = FontWeight.Medium),
            )
        }
    }
}

/** In-app preview of how a local notification will present. */
@Composable
private fun NotificationBanner(visible: Boolean, modifier: Modifier = Modifier) {
    val c = MozzTheme.colors
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(Motion.Pop, easing = Motion.Emphasized)) { -it } +
            fadeIn(tween(Motion.Pop)),
        exit = slideOutVertically(tween(Motion.Pop)) { -it } + fadeOut(tween(Motion.Pop)),
        modifier = modifier.padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface.copy(alpha = 0.94f))
                .border(1.dp, c.line2, RoundedCornerShape(20.dp))
                .padding(horizontal = 15.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Brush.linearGradient(listOf(c.accent, c.accent2))),
                contentAlignment = Alignment.Center,
            ) {
                IconMic(c.accentInk, size = 20.dp, filled = true)
            }
            Column(Modifier.weight(1f)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "MOZZID",
                        style = MozzText.monoSmall.copy(
                            color = c.accentSoftText,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text("now", style = MozzText.monoSmall.copy(color = c.text4))
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.notif1_title),
                    style = MozzText.bodySmall.copy(
                        color = c.text,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    stringResource(R.string.notif1_body),
                    style = MozzText.caption.copy(color = c.text2),
                )
            }
        }
    }
}
