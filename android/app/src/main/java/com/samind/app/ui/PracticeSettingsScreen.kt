package com.samind.app.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.data.Prefs
import com.samind.app.practice.BreathPattern
import com.samind.app.practice.DURATION_OPTIONS_MINUTES
import com.samind.app.ui.components.CardShape
import com.samind.app.ui.components.PillShape
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary200
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients

/**
 * Technique choice is radio-style (picking one clears the other) and so is the
 * duration; Save is always enabled regardless of changes (design annotations).
 */
@Composable
fun PracticeSettingsScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var technique by remember { mutableStateOf(Prefs.technique(context)) }
    var minutes by remember { mutableIntStateOf(Prefs.minutes(context)) }

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.tab_settings), onBack = onDone)
            Column(
                Modifier
                    .weight(1f)
                    .padding(horizontal = ScreenMargin),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(R.string.settings_technique),
                    style = MaterialTheme.typography.titleLarge,
                    color = Neutral900,
                )
                BreathPattern.all.forEach { pattern ->
                    TechniqueRow(
                        title = pattern.id,
                        selected = technique == pattern.id,
                        onSelect = { technique = pattern.id },
                    )
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.settings_duration),
                    style = MaterialTheme.typography.titleLarge,
                    color = Neutral900,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DURATION_OPTIONS_MINUTES.forEach { option ->
                        DurationChip(
                            label = stringResource(R.string.settings_minutes, option),
                            selected = minutes == option,
                            onSelect = { minutes = option },
                        )
                    }
                }
            }
            Column(Modifier.padding(ScreenMargin)) {
                PrimaryButton(
                    text = stringResource(R.string.settings_save),
                    onClick = {
                        Prefs.setPractice(context, technique, minutes)
                        onDone()
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TechniqueRow(title: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(SamindGradients.controlActive, CardShape)
            .clickable(onClick = onSelect)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = Neutral900,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(22.dp)
                .background(
                    if (selected) Primary900 else Color.Transparent,
                    RoundedCornerShape(6.dp),
                )
                .border(1.5.dp, if (selected) Primary900 else Primary200, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

@Composable
private fun DurationChip(label: String, selected: Boolean, onSelect: () -> Unit) {
    Box(
        Modifier
            .height(44.dp)
            .background(
                if (selected) SamindGradients.controlActive else SamindGradients.controlDisabled,
                PillShape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Primary900 else Neutral900.copy(alpha = 0.5f),
        )
    }
}
