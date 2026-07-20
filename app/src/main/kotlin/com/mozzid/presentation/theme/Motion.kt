package com.mozzid.presentation.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Motion tokens lifted verbatim from the design's CSS keyframes so timings do not
 * drift as screens are ported. Durations are milliseconds.
 *
 * `Emphasized` is the design's `cubic-bezier(.2,.8,.2,1)` — the overshoot-free but
 * snappy curve used for anything that enters (sheets, map pins, toasts).
 * `Screen` is `cubic-bezier(.2,.7,.2,1)`, the slightly softer screen fade.
 */
object Motion {
    val Emphasized: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val Screen: Easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
    val Standard: Easing = FastOutSlowInEasing

    const val ScreenFade = 400      // .mzScreen
    const val Pop = 350             // mzPop — notification banner
    const val Toast = 300
    const val Sheet = 400           // mzSlide — species card
    const val PinDrop = 500         // mzPin
    const val PinStagger = 80       // 0.08s between pins

    const val MascotFloat = 4000    // mzFloat
    const val WingFlap = 500        // mzWing
    const val RingPulse = 2400      // mzRing
    const val RingStagger = 800
    const val BarPulse = 700        // mzBar — listening level meter
    const val SpectroPulse = 900    // mzSpec
    const val Scan = 1600           // mzScan — analyzing sweep
    const val Glow = 3400           // mzGlow — idle record button
    const val Dot = 1000            // mzDot
}
