package com.samind.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.practice.GROUNDING_STEP_ITEMS
import com.samind.app.practice.sessionComplete
import com.samind.app.practice.stepComplete
import com.samind.app.ui.components.CompletionDialog
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary200
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindMotion

/**
 * 5-4-3-2-1. Design rules: tapping a circle marks it immediately; "Next" is
 * enabled only when every circle of the step is marked; marking the last item
 * of the last step opens the completion modal by itself.
 */
@Composable
fun GroundingScreen(initialTechniqueId: String? = null, onExit: () -> Unit = {}) {
    var started by remember { mutableStateOf(initialTechniqueId != null) }
    var step by remember { mutableIntStateOf(0) }
    var marked by remember { mutableStateOf(setOf<Int>()) }
    val done = sessionComplete(step, marked)

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.practice_grounding), onBack = onExit)

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
                        stringResource(R.string.grounding_intro_title),
                        style = MaterialTheme.typography.displayMedium,
                        color = Neutral900,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.grounding_intro_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Neutral900,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        stringResource(GROUNDING_PROMPTS[step]),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Neutral900,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        repeat(GROUNDING_STEP_ITEMS[step]) { index ->
                            ItemCircle(
                                checked = index in marked,
                                onClick = { marked = marked + index },
                            )
                        }
                    }
                }
            }

            if (started) {
                Text(
                    "${step + 1} / ${GROUNDING_STEP_ITEMS.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Neutral900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }

            Column(
                Modifier.padding(horizontal = ScreenMargin, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!started) {
                    PrimaryButton(stringResource(R.string.practice_start)) { started = true }
                } else {
                    PrimaryButton(
                        stringResource(R.string.practice_next),
                        enabled = stepComplete(marked, step) && step < GROUNDING_STEP_ITEMS.lastIndex,
                        onClick = { step++; marked = emptySet() },
                    )
                    PrimaryButton(stringResource(R.string.practice_finish), onExit)
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        if (done) {
            CompletionDialog(
                title = stringResource(R.string.grounding_done_title),
                body = stringResource(R.string.grounding_done_body),
                buttonText = stringResource(R.string.practice_finish),
                onFinish = onExit,
            )
        }
    }
}

private val GROUNDING_PROMPTS = listOf(
    R.string.grounding_step_1,
    R.string.grounding_step_2,
    R.string.grounding_step_3,
    R.string.grounding_step_4,
    R.string.grounding_step_5,
)

@Composable
private fun ItemCircle(checked: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(
        if (checked) 1f else 0f,
        tween(SamindMotion.SHORT, easing = SamindMotion.standard),
        label = "item",
    )
    Box(
        Modifier
            .size(44.dp)
            .background(Primary900.copy(alpha = alpha), CircleShape)
            .border(1.5.dp, if (checked) Color.Transparent else Primary200, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
