package com.samind.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.practice.GROUNDING_STEP_ITEMS
import com.samind.app.practice.sessionComplete
import com.samind.app.practice.stepComplete
import com.samind.app.ui.components.CompletionDialog
import com.samind.app.ui.components.ItemCircle
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Primary900

/**
 * 5-4-3-2-1, laid out as in the design: the step's number huge and centred, the
 * prompt beneath it, then a single row of item circles. Step counter sits just
 * above the two stacked buttons.
 */
@Composable
fun GroundingScreen(
    initialTechniqueId: String? = null,
    designState: String? = null,
    onExit: () -> Unit = {},
) {
    val presetStep = DesignState.groundingStep(designState)
    var started by remember { mutableStateOf(initialTechniqueId != null || presetStep != null) }
    var step by remember { mutableIntStateOf(presetStep ?: 0) }
    var marked by remember {
        mutableStateOf(
            DesignState.groundingMarked(designState, GROUNDING_STEP_ITEMS[presetStep ?: 0]),
        )
    }
    val done = sessionComplete(step, marked)

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.practice_grounding), onBack = onExit)

            Spacer(Modifier.height(72.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!started) {
                    Text(
                        stringResource(R.string.grounding_intro_title),
                        style = MaterialTheme.typography.displayMedium,
                        color = Primary900,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        stringResource(R.string.grounding_intro_body),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Primary900,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        GROUNDING_STEP_ITEMS[step].toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Primary900,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(GROUNDING_PROMPTS[step]),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Primary900,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(28.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        repeat(GROUNDING_STEP_ITEMS[step]) { index ->
                            ItemCircle(
                                checked = index in marked,
                                onClick = { marked = marked + index },
                                modifier = Modifier.weight(1f, fill = false),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            if (started) {
                Text(
                    "${step + 1} / ${GROUNDING_STEP_ITEMS.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary900,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(14.dp))
            }

            Column(
                Modifier.padding(horizontal = ScreenMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!started) {
                    PrimaryButton(
                        text = stringResource(R.string.practice_start),
                        onClick = { started = true },
                    )
                } else {
                    PrimaryButton(
                        text = stringResource(R.string.practice_next),
                        enabled = stepComplete(marked, step) &&
                            step < GROUNDING_STEP_ITEMS.lastIndex,
                        onClick = { step++; marked = emptySet() },
                    )
                    PrimaryButton(stringResource(R.string.practice_finish), onExit)
                }
            }
            Spacer(Modifier.height(28.dp))
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
