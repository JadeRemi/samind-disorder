package com.samind.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.theme.LocalReducedMotion
import com.samind.app.ui.theme.Neutral900
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindMotion
import com.samind.app.ui.theme.SemanticError

/**
 * Shared empty / error / loading states, built from the same kit as every other
 * screen (no design frames exist for these — see DEV_PLAN_REDESIGN open q. 5).
 */

@Composable
fun EmptyState(
    iconRes: Int,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(SamindGradients.controlActive, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = Primary900,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = Neutral900,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = Neutral900.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(24.dp))
            PrimaryButton(actionText, onAction)
        }
    }
}

@Composable
fun ErrorState(
    title: String,
    body: String,
    retryText: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(72.dp)
                .background(SamindGradients.controlError, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_info),
                contentDescription = null,
                tint = SemanticError,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = Neutral900,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = Neutral900.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton(retryText, onRetry)
    }
}

/** Bot "typing" placeholder: fixed bar widths, gentle opacity pulse (design). */
@Composable
fun TypingIndicator(modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val pulse = rememberInfiniteTransition(label = "typing")
    val animatedAlpha by pulse.animateFloat(
        0.35f, 0.9f,
        infiniteRepeatable(
            tween(SamindMotion.LONG, easing = SamindMotion.standard),
            RepeatMode.Reverse,
        ),
        label = "typingAlpha",
    )
    val alpha = if (reduced) 0.7f else animatedAlpha
    Column(modifier.alpha(alpha), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(220.dp, 180.dp, 120.dp).forEach { barWidth ->
            Box(
                Modifier
                    .width(barWidth)
                    .height(14.dp)
                    .background(SamindGradients.controlActive, RoundedCornerShape(100.dp)),
            )
        }
    }
}

/** Inline banner for recoverable problems, e.g. the model failing to load. */
@Composable
fun InlineNotice(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .background(SamindGradients.controlProcess, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_info),
            contentDescription = null,
            tint = Primary900,
            modifier = Modifier.size(18.dp),
        )
        Text(text, style = MaterialTheme.typography.bodySmall, color = Neutral900)
    }
}
