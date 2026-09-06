package com.samind.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.practice.BreathPattern
import com.samind.app.practice.BreathPhase
import com.samind.app.ui.components.CompletionDialog
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary200
import com.samind.app.ui.theme.Primary900
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private const val TICK_MS = 16L

/**
 * Box breathing. Design rules honoured here:
 *  - each side fills continuously in real time, never stepped per second;
 *  - the phase label changes at the same instant the active side changes
 *    (both read the same clock, so they cannot drift);
 *  - pausing freezes the fill exactly where it is and blinks the timer at 1 Hz;
 *  - the same button relabels Pause <-> Continue, it is not replaced.
 */
@Composable
fun BreathingScreen(
    patternId: String = BreathPattern.BOX_4444.id,
    sessionMinutes: Int = 10,
    onExit: () -> Unit = {},
) {
    val pattern = remember(patternId) { BreathPattern.byId(patternId) }
    val total = sessionMinutes * 60f
    var elapsed by remember { mutableFloatStateOf(0f) }
    var running by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    val finished = elapsed >= total

    LaunchedEffect(running) {
        var last = System.nanoTime()
        while (running) {
            delay(TICK_MS)
            val now = System.nanoTime()
            elapsed = (elapsed + (now - last) / 1_000_000_000f).coerceAtMost(total)
            last = now
            if (elapsed >= total) running = false
        }
    }

    val (phase, fraction) = pattern.at(elapsed)
    val remaining = (total - elapsed).roundToInt()

    // paused timer blinks ~1 Hz
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blink.animateFloat(
        1f, 0.25f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "blinkAlpha",
    )
    val timerAlpha = if (started && !running && !finished) blinkAlpha else 1f

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.practice_breathing), onBack = onExit)

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(
                            when (phase) {
                                BreathPhase.INHALE -> R.string.phase_inhale
                                BreathPhase.HOLD -> R.string.phase_hold
                                BreathPhase.EXHALE -> R.string.phase_exhale
                                BreathPhase.WAIT -> R.string.phase_wait
                            },
                        ),
                        style = MaterialTheme.typography.displayMedium,
                        color = Neutral900,
                        modifier = Modifier.alpha(if (started) 1f else 0f),
                    )
                    Spacer(Modifier.height(16.dp))
                    Box(contentAlignment = Alignment.Center) {
                        BreathingSquare(phase, fraction, Modifier.fillMaxWidth().aspectRatio(1f))
                        val secondsLeft = pattern.seconds[phase.ordinal] * (1f - fraction)
                        Text(
                            if (started) secondsLeft.roundToInt().coerceAtLeast(1).toString()
                            else pattern.seconds.first().toString(),
                            style = MaterialTheme.typography.displayLarge,
                            color = Neutral900,
                        )
                    }
                }
            }

            Text(
                "%02d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.titleMedium,
                color = Neutral900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().alpha(timerAlpha),
            )
            Spacer(Modifier.height(12.dp))

            Column(
                Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!started) {
                    PrimaryButton(
                        text = stringResource(R.string.practice_start),
                        onClick = { started = true; running = true },
                    )
                } else {
                    // one button that relabels — never a second button
                    PrimaryButton(
                        stringResource(
                            if (running) R.string.practice_pause else R.string.practice_continue,
                        ),
                        onClick = { running = !running },
                        enabled = !finished,
                    )
                    PrimaryButton(stringResource(R.string.practice_finish), onExit)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (finished) {
            CompletionDialog(
                title = stringResource(R.string.breathing_done_title),
                body = stringResource(R.string.breathing_done_body),
                buttonText = stringResource(R.string.practice_finish),
                onFinish = onExit,
            )
        }
    }
}

/** Four sides; only the active one animates, previous sides stay filled. */
@Composable
private fun BreathingSquare(phase: BreathPhase, fraction: Float, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 18.dp.toPx()
        val inset = stroke / 2
        val w = size.width - stroke
        val h = size.height - stroke
        val corners = listOf(
            Offset(inset, inset) to Offset(inset + w, inset),          // top: inhale
            Offset(inset + w, inset) to Offset(inset + w, inset + h),  // right: hold
            Offset(inset + w, inset + h) to Offset(inset, inset + h),  // bottom: exhale
            Offset(inset, inset + h) to Offset(inset, inset),          // left: wait
        )
        corners.forEach { (from, to) ->
            drawLine(Primary200, from, to, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        val active = phase.ordinal
        for (index in 0 until active) {
            val (from, to) = corners[index]
            drawLine(Primary900, from, to, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
        val (from, to) = corners[active]
        drawLine(
            Primary900,
            from,
            Offset(from.x + (to.x - from.x) * fraction, from.y + (to.y - from.y) * fraction),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
        )
    }
}
