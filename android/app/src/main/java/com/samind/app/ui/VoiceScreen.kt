package com.samind.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindMotion
import kotlin.math.sin

enum class VoiceState { IDLE, LISTENING, THINKING, SPEAKING, ERROR }

/**
 * Voice conversation screen. Design rule: the orb's colour never changes
 * between states — only the speed and amplitude of its motion do.
 */
@Composable
fun VoiceScreen(onExit: () -> Unit = {}) {
    var state by remember { mutableStateOf(VoiceState.IDLE) }

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.voice_title), onBack = onExit)

            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                VoiceOrb(state, Modifier.size(240.dp))
                Spacer(Modifier.height(32.dp))
                Text(
                    stringResource(
                        when (state) {
                            VoiceState.IDLE -> R.string.voice_idle
                            VoiceState.LISTENING -> R.string.voice_listening
                            VoiceState.THINKING -> R.string.voice_thinking
                            VoiceState.SPEAKING -> R.string.voice_speaking
                            VoiceState.ERROR -> R.string.voice_error
                        },
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Neutral900,
                    textAlign = TextAlign.Center,
                )
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                MicButton(state) {
                    state = when (state) {
                        VoiceState.IDLE, VoiceState.ERROR -> VoiceState.LISTENING
                        VoiceState.LISTENING -> VoiceState.THINKING
                        VoiceState.THINKING -> VoiceState.SPEAKING
                        VoiceState.SPEAKING -> VoiceState.IDLE
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceOrb(state: VoiceState, modifier: Modifier = Modifier) {
    // motion, not colour, expresses the state
    val speed = when (state) {
        VoiceState.IDLE -> 9000
        VoiceState.LISTENING -> 2600
        VoiceState.THINKING -> 1400
        VoiceState.SPEAKING -> 1900
        VoiceState.ERROR -> 6000
    }
    val amplitude = when (state) {
        VoiceState.IDLE -> 0.03f
        VoiceState.LISTENING -> 0.10f
        VoiceState.THINKING -> 0.06f
        VoiceState.SPEAKING -> 0.14f
        VoiceState.ERROR -> 0.02f
    }
    val transition = rememberInfiniteTransition(label = "orb")
    val phase by transition.animateFloat(
        0f, (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(speed, easing = LinearEasing), RepeatMode.Restart),
        label = "orbPhase",
    )
    val amp by animateFloatAsState(
        amplitude,
        tween(SamindMotion.LONG, easing = SamindMotion.standard),
        label = "orbAmp",
    )

    Canvas(modifier) {
        val base = size.minDimension / 2f
        // three offset lobes breathing at different rates give the fluid look
        repeat(3) { index ->
            val local = phase + index * 2.1f
            val radius = base * (1f - index * 0.08f) * (1f + amp * sin(local))
            val offsetX = base * amp * 0.5f * sin(local * 0.7f)
            val offsetY = base * amp * 0.5f * sin(local * 1.1f)
            drawCircle(
                brush = SamindGradients.voiceOrb,
                radius = radius,
                center = center.copy(center.x + offsetX, center.y + offsetY),
                alpha = 0.55f,
            )
        }
    }
}

@Composable
private fun MicButton(state: VoiceState, onClick: () -> Unit) {
    val brush: Brush = when (state) {
        VoiceState.THINKING -> SamindGradients.controlProcess
        VoiceState.ERROR -> SamindGradients.controlError
        else -> SamindGradients.controlActive
    }
    Box(
        Modifier
            .size(84.dp)
            .background(brush, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(
                if (state == VoiceState.LISTENING) R.drawable.ic_waveform else R.drawable.ic_mic,
            ),
            contentDescription = stringResource(R.string.voice_title),
            tint = Primary900,
            modifier = Modifier.size(30.dp),
        )
    }
}
