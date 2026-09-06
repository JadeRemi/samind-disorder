package com.samind.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindMotion
import kotlinx.coroutines.launch

/**
 * Home's arm control. Design rules:
 *  - the knob must be dragged the FULL width of the track to switch;
 *    a partial swipe snaps back and counts as nothing;
 *  - label and icon swap sides smoothly as the state changes.
 */
@Composable
fun SwipeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    offLabel: String,
    onLabel: String,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val trackHeight = 100.dp
    val knob = 84.dp
    val scope = rememberCoroutineScope()
    val drag = remember { Animatable(0f) }
    var trackWidthPx by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val knobPx = with(density) { knob.toPx() }
    val padPx = with(density) { 8.dp.toPx() }

    // settle to the position implied by state whenever it changes externally
    LaunchedEffect(checked, trackWidthPx) {
        val travel = (trackWidthPx - knobPx - padPx * 2).coerceAtLeast(0f)
        drag.animateTo(
            if (checked) travel else 0f,
            tween(SamindMotion.MEDIUM, easing = SamindMotion.standard),
        )
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(trackHeight)
            .background(SamindGradients.controlActive, PillShape),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .pointerInput(checked, trackWidthPx) {
                    trackWidthPx = size.width.toFloat()
                    val travel = (trackWidthPx - knobPx - padPx * 2).coerceAtLeast(1f)
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                // full travel required — anything less snaps back
                                val complete = drag.value >= travel * 0.92f
                                if (complete != checked) onCheckedChange(complete)
                                drag.animateTo(
                                    if (complete) travel else 0f,
                                    tween(SamindMotion.MEDIUM, easing = SamindMotion.standard),
                                )
                            }
                        },
                        onHorizontalDrag = { _, delta ->
                            scope.launch {
                                drag.snapTo((drag.value + delta).coerceIn(0f, travel))
                            }
                        },
                    )
                },
        )

        val travel = (trackWidthPx - knobPx - padPx * 2).coerceAtLeast(1f)
        val fraction = (drag.value / travel).coerceIn(0f, 1f)

        // label crossfades and shifts to the opposite side as the knob travels
        Text(
            text = if (fraction > 0.5f) onLabel else offLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = Neutral900,
            modifier = Modifier
                .align(if (fraction > 0.5f) Alignment.CenterStart else Alignment.CenterEnd)
                .padding(horizontal = 40.dp)
                .alpha(1f - (0.6f * kotlin.math.abs(0.5f - fraction) * 2f).coerceIn(0f, 0.6f)),
        )

        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset(drag.value.toInt() + padPx.toInt(), 0) }
                .align(Alignment.CenterStart)
                .size(knob)
                .background(SamindGradients.controlActive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(if (fraction > 0.5f) R.drawable.ic_check else R.drawable.ic_play),
                contentDescription = null,
                tint = Primary900,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
