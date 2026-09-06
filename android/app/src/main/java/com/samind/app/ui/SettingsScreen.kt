package com.samind.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.samind.app.BuildConfig
import com.samind.app.R
import com.samind.app.data.Prefs
import com.samind.app.practice.BreathPattern
import com.samind.app.practice.DURATION_OPTIONS_MINUTES
import com.samind.app.ui.components.CardShape
import com.samind.app.ui.components.PillShape
import com.samind.app.ui.components.SamindBackground
import com.samind.app.ui.components.SamindTopBar
import com.samind.app.ui.components.ScreenMargin
import com.samind.app.ui.theme.Neutral0
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary200
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindMotion

/**
 * Full settings screen. No design frame exists for this tab, so it is composed
 * strictly from the kit: sectioned cards, pill chips, the shared switch style.
 */
@Composable
fun SettingsScreen(
    onOpenAccessibility: () -> Unit = {},
    onOpenVoice: () -> Unit = {},
) {
    val context = LocalContext.current
    var monitoring by remember { mutableStateOf(Prefs.monitoringEnabled(context)) }
    var technique by remember { mutableStateOf(Prefs.technique(context)) }
    var minutes by remember { mutableIntStateOf(Prefs.minutes(context)) }
    val name = Prefs.displayName(context)

    SamindBackground {
        Column(Modifier.fillMaxSize()) {
            SamindTopBar(stringResource(R.string.tab_settings))
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ScreenMargin),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionTitle(stringResource(R.string.settings_general))
                SettingCard {
                    SwitchRow(
                        label = stringResource(R.string.monitoring_on),
                        checked = monitoring,
                        onChange = {
                            monitoring = it
                            Prefs.setMonitoringEnabled(context, it)
                        },
                    )
                    // the hint only matters while monitoring is on
                    AnimatedVisibility(
                        visible = monitoring,
                        enter = fadeIn(tween(SamindMotion.MEDIUM, easing = SamindMotion.standard)) +
                            expandVertically(tween(SamindMotion.MEDIUM, easing = SamindMotion.standard)),
                        exit = fadeOut(tween(SamindMotion.SHORT)) + shrinkVertically(tween(SamindMotion.SHORT)),
                    ) {
                        Column {
                            Divider()
                            NavRow(
                                label = stringResource(R.string.settings_open_accessibility),
                                onClick = onOpenAccessibility,
                            )
                        }
                    }
                    Divider()
                    ValueRow(
                        label = stringResource(R.string.settings_name),
                        value = name.ifBlank { "—" },
                    )
                }

                SectionTitle(stringResource(R.string.settings_practice))
                SettingCard {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            stringResource(R.string.settings_technique),
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral900,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BreathPattern.all.forEach { pattern ->
                                Chip(pattern.id, technique == pattern.id) {
                                    technique = pattern.id
                                    Prefs.setPractice(context, technique, minutes)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.settings_duration),
                            style = MaterialTheme.typography.titleMedium,
                            color = Neutral900,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DURATION_OPTIONS_MINUTES.forEach { option ->
                                Chip(
                                    stringResource(R.string.settings_minutes, option),
                                    minutes == option,
                                ) {
                                    minutes = option
                                    Prefs.setPractice(context, technique, minutes)
                                }
                            }
                        }
                    }
                }

                SectionTitle(stringResource(R.string.voice_title))
                SettingCard {
                    NavRow(stringResource(R.string.voice_title), onOpenVoice)
                }

                SectionTitle(stringResource(R.string.settings_privacy))
                SettingCard {
                    Text(
                        stringResource(R.string.settings_privacy_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = Neutral900,
                        modifier = Modifier.padding(16.dp),
                    )
                }

                SectionTitle(stringResource(R.string.settings_about))
                SettingCard {
                    ValueRow(
                        label = stringResource(R.string.app_name),
                        value = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                    )
                    Divider()
                    Text(
                        stringResource(R.string.disclaimer),
                        style = MaterialTheme.typography.labelSmall,
                        color = Neutral900.copy(alpha = 0.7f),
                        modifier = Modifier.padding(16.dp),
                    )
                }
                Spacer(Modifier.height(110.dp))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleLarge,
        color = Neutral900,
        modifier = Modifier.padding(top = 12.dp, start = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(SamindGradients.controlActive, CardShape),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 16.dp)
            .background(Primary200.copy(alpha = 0.6f)),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = Neutral900,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Neutral0,
                checkedTrackColor = Primary900,
                uncheckedTrackColor = Primary200,
            ),
        )
    }
}

@Composable
private fun ValueRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = Neutral900,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodySmall, color = Neutral900.copy(alpha = 0.7f))
    }
}

@Composable
private fun NavRow(label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = Neutral900,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(28.dp)
                .background(Neutral0.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_chevron_left),
                contentDescription = null,
                tint = Primary900,
                // the kit ships a left chevron only; mirror it for "forward"
                modifier = Modifier
                    .size(14.dp)
                    .graphicsLayer { scaleX = -1f },
            )
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onSelect: () -> Unit) {
    Box(
        Modifier
            .height(40.dp)
            .background(
                if (selected) SamindGradients.controlActive else SamindGradients.controlDisabled,
                PillShape,
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleMedium,
            color = if (selected) Primary900 else Neutral900.copy(alpha = 0.5f),
        )
    }
}
