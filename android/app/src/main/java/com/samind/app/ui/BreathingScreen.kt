package com.samind.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.practice.BreathPattern
import com.samind.app.practice.BreathPhase
import com.samind.app.ui.components.BreathingSquare
import com.samind.app.ui.components.CompletionDialog
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Primary900
import kotlinx.coroutines.delay
import kotlin.math.ceil

/**
 * Layout follows the design frames exactly: phase label large and centred above
 * the figure, the counter inside it, timer just above the two stacked buttons.
 */
@Composable
fun BreathingScreen(
    patternId: String = BreathPattern.BOX_4444.id,
    sessionMinutes: Int = 10,
    onExit: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
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
            delay(16)
            val now = System.nanoTime()
            elapsed = (elapsed + (now - last) / 1_000_000_000f).coerceAtMost(total)
            last = now
            if (elapsed >= total) running = false
        }
    }

    val (phase, fraction) = pattern.at(elapsed)
    val remaining = (total - elapsed).toInt()

    // paused: the timer blinks ~1 Hz while the figure freezes
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blink.animateFloat(
        1f, 0.25f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "blinkAlpha",
    )
    val timerAlpha = if (started && !running && !finished) blinkAlpha else 1f

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(
                stringResource(R.string.practice_breathing),
                onBack = onExit,
                actionIcon = R.drawable.ic_sliders,
                onAction = onOpenSettings,
            )

            Spacer(Modifier.height(24.dp))
            Text(
                if (started) {
                    stringResource(
                        when (phase) {
                            BreathPhase.INHALE -> R.string.phase_inhale
                            BreathPhase.HOLD -> R.string.phase_hold
                            BreathPhase.EXHALE -> R.string.phase_exhale
                            BreathPhase.WAIT -> R.string.phase_wait
                        },
                    )
                } else {
                    ""
                },
                style = MaterialTheme.typography.displayMedium,
                color = Primary900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center,
            ) {
                BreathingSquare(phase, if (started) fraction else 0f, Modifier.fillMaxSize())
                val secondsLeft = pattern.seconds[phase.ordinal] * (1f - fraction)
                Text(
                    if (started) {
                        ceil(secondsLeft).toInt().coerceAtLeast(1).toString()
                    } else {
                        pattern.seconds.first().toString()
                    },
                    style = MaterialTheme.typography.displayLarge,
                    color = Primary900,
                )
            }

            Spacer(Modifier.weight(1f))
            Text(
                "%02d:%02d".format(remaining / 60, remaining % 60),
                style = MaterialTheme.typography.titleMedium,
                color = Primary900,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().alpha(timerAlpha),
            )
            Spacer(Modifier.height(14.dp))

            Column(
                Modifier.padding(horizontal = ScreenMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!started) {
                    PrimaryButton(
                        text = stringResource(R.string.practice_start),
                        onClick = { started = true; running = true },
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(
                            if (running) R.string.practice_pause else R.string.practice_continue,
                        ),
                        onClick = { running = !running },
                        enabled = !finished,
                    )
                    PrimaryButton(stringResource(R.string.practice_finish), onExit)
                }
            }
            Spacer(Modifier.height(28.dp))
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
