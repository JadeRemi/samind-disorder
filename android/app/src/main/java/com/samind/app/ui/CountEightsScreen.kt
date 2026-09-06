package com.samind.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import com.samind.app.practice.eightsCount
import com.samind.app.ui.components.CompletionDialog
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.ProgressDotGrid
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Count-eights. The dot grid always holds 40 dots; only the fill rate changes
 * with the chosen session length (design annotation).
 */
@Composable
fun CountEightsScreen(sessionMinutes: Int = 10, onExit: () -> Unit = {}) {
    val pattern = BreathPattern.EIGHTS
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

    val remaining = (total - elapsed).roundToInt()
    val blink = rememberInfiniteTransition(label = "blink")
    val blinkAlpha by blink.animateFloat(
        1f, 0.25f,
        infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
        label = "blinkAlpha",
    )
    val timerAlpha = if (started && !running && !finished) blinkAlpha else 1f

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.practice_eights), onBack = onExit)

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (!started) {
                    Text(
                        stringResource(R.string.eights_intro),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Neutral900,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.eights_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral900.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                } else {
                    Text(
                        eightsCount(elapsed, pattern.cycleSeconds).toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Neutral900,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.eights_active),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Neutral900,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                }
                ProgressDotGrid(progress = elapsed / total)
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
                    PrimaryButton(stringResource(R.string.practice_start)) {
                        started = true; running = true
                    }
                } else {
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
                title = stringResource(R.string.eights_done_title),
                body = stringResource(R.string.eights_done_body),
                buttonText = stringResource(R.string.practice_finish),
                onFinish = onExit,
            )
        }
    }
}
