package com.samind.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary200
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindMotion

// Design constants (412x915 reference frame)
val ScreenMargin = 16.dp
val ControlHeight = 68.dp
val IconButtonSize = 48.dp
val PillShape = RoundedCornerShape(100.dp)
val CardShape = RoundedCornerShape(24.dp)

/** The shared photographic background with its static gradient veil. */
@Composable
fun SamindBackground(
    modifier: Modifier = Modifier,
    signIn: Boolean = false,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        Image(
            painterResource(if (signIn) R.drawable.bg_signin else R.drawable.bg_surface),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
        // static veil: never scrolls or reacts (design annotation)
        Box(Modifier.fillMaxSize().background(SamindGradients.surfaceVeil))
        content()
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val brush: Brush =
        if (enabled) SamindGradients.controlActive else SamindGradients.controlDisabled
    Box(
        modifier
            .fillMaxWidth()
            .height(ControlHeight)
            .background(brush, PillShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) Neutral900 else Neutral900.copy(alpha = 0.38f),
        )
    }
}

@Composable
fun CircleIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .size(IconButtonSize)
            .background(SamindGradients.controlActive, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Primary900,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Top app bar: fixed, title always centred regardless of the actions present. */
@Composable
fun SamindTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actionIcon: Int? = null,
    onAction: (() -> Unit)? = null,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = ScreenMargin),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Neutral900)
        if (onBack != null) {
            CircleIconButton(
                R.drawable.ic_chevron_left, "Back", onBack,
                Modifier.align(Alignment.CenterStart),
            )
        }
        if (actionIcon != null && onAction != null) {
            CircleIconButton(
                actionIcon, null, onAction,
                Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
fun PracticeCard(
    title: String,
    supporting: String,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(157.dp)
            .background(SamindGradients.controlActive, CardShape)
            .clickable(onClick = onStart)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Neutral900)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = Neutral900)
        }
        CircleIconButton(R.drawable.ic_play, "Start", onStart)
    }
}

/**
 * 40 dots, always — only the fill rate changes with session length
 * (design annotation: one dot = duration / 40).
 */
@Composable
fun ProgressDotGrid(
    progress: Float,
    modifier: Modifier = Modifier,
    columns: Int = 8,
    total: Int = 40,
) {
    val filled = (progress.coerceIn(0f, 1f) * total).toInt()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in 0 until total / columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    val on = index < filled
                    val alpha by animateFloatAsState(
                        if (on) 1f else 0.35f,
                        tween(SamindMotion.SHORT, easing = SamindMotion.standard),
                        label = "dot",
                    )
                    Box(
                        Modifier
                            .size(22.dp)
                            .alpha(alpha)
                            .background(
                                if (on) Primary900 else androidx.compose.ui.graphics.Color.Transparent,
                                CircleShape,
                            )
                            .border(1.5.dp, Primary200, CircleShape),
                    )
                }
            }
        }
    }
}

/** Completion modal: cannot be dismissed by tapping outside (design constraint). */
@Composable
fun CompletionDialog(
    title: String,
    body: String,
    buttonText: String,
    onFinish: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Neutral900.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 32.dp)
                .background(SamindGradients.dialog, CardShape)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier
                    .size(IconButtonSize)
                    .background(SamindGradients.controlActive, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = Primary900,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(title, style = MaterialTheme.typography.headlineMedium, color = Neutral900)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = Neutral900,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(buttonText, onFinish)
        }
    }
}
