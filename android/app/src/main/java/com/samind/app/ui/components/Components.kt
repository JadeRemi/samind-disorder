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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.theme.AdaptiveContainer
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary200
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindMotion

// Reference frame 412x915; sizes measured from the 2x design exports.
val ScreenMargin = 16.dp
val ControlHeight = 68.dp
val IconButtonSize = 56.dp
val PillShape = RoundedCornerShape(100.dp)
val CardShape = RoundedCornerShape(28.dp)

/** Photographic background; static behind every screen. */
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
        AdaptiveContainer { content() }
    }
}

/**
 * The whole kit is frosted glass over the photograph: translucent white fill,
 * soft drop shadow, pale hairline — never a solid colour block.
 */
private fun Modifier.frosted(
    shape: Shape,
    elevation: Dp = 6.dp,
    enabled: Boolean = true,
) = this
    .shadow(elevation, shape, clip = false)
    .background(
        if (enabled) SamindGradients.frostedControl else SamindGradients.frostedDisabled,
        shape,
    )
    .border(1.dp, Color.White.copy(alpha = if (enabled) 0.55f else 0.35f), shape)

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(ControlHeight)
            .frosted(PillShape, enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.titleLarge,
            color = if (enabled) Primary900 else Primary900.copy(alpha = 0.4f),
        )
    }
}

@Composable
fun CircleIconButton(
    iconRes: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = IconButtonSize,
) {
    Box(
        modifier
            .size(size)
            .frosted(CircleShape, elevation = 4.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Primary900,
            modifier = Modifier.size(size * 0.36f),
        )
    }
}

/** Fixed top bar; the title stays centred whatever buttons are present. */
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
            .height(72.dp)
            .padding(horizontal = ScreenMargin),
        contentAlignment = Alignment.Center,
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, color = Primary900)
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

/**
 * Practice card: title top-left, supporting text bottom-left, chevron circle
 * bottom-right (380x157 in the design).
 */
@Composable
fun PracticeCard(
    title: String,
    supporting: String,
    onStart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(157.dp)
            .shadow(8.dp, CardShape, clip = false)
            .background(SamindGradients.frostedCard, CardShape)
            .clickable(onClick = onStart)
            .padding(horizontal = 22.dp, vertical = 18.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.displaySmall,
            color = Primary900,
            modifier = Modifier.align(Alignment.TopStart),
        )
        Text(
            supporting,
            style = MaterialTheme.typography.bodySmall,
            color = Primary900.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(end = 72.dp),
        )
        CircleIconButton(
            R.drawable.ic_chevron_right,
            "Start",
            onStart,
            Modifier.align(Alignment.BottomEnd),
            size = 52.dp,
        )
    }
}

/** 40 dots; only the fill rate changes with session length. */
@Composable
fun ProgressDotGrid(
    progress: Float,
    modifier: Modifier = Modifier,
    columns: Int = 8,
    total: Int = 40,
) {
    val filled = (progress.coerceIn(0f, 1f) * total).toInt()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (row in 0 until total / columns) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                for (col in 0 until columns) {
                    val on = row * columns + col < filled
                    val alpha by animateFloatAsState(
                        if (on) 1f else 0.45f,
                        tween(SamindMotion.SHORT, easing = SamindMotion.standard),
                        label = "dot",
                    )
                    Box(
                        Modifier
                            .size(26.dp)
                            .alpha(alpha)
                            .background(
                                if (on) Primary900 else Color.White.copy(alpha = 0.35f),
                                CircleShape,
                            )
                            .border(1.dp, Primary200.copy(alpha = 0.8f), CircleShape),
                    )
                }
            }
        }
    }
}

/** Item circle for 5-4-3-2-1: frosted disc, white tick once marked. */
@Composable
fun ItemCircle(checked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(58.dp)
            .shadow(4.dp, CircleShape, clip = false)
            .background(
                if (checked) SamindGradients.itemChecked else SamindGradients.itemUnchecked,
                CircleShape,
            )
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

/** Completion modal — not dismissible from outside. */
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
            .background(Neutral900.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .shadow(16.dp, CardShape, clip = false)
                .background(SamindGradients.frostedCard, CardShape)
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(SamindGradients.frostedControl, CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    tint = Primary900,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = Primary900,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = Primary900.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(buttonText, onFinish)
        }
    }
}
