package com.samind.app.overlay

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samind.app.R
import com.samind.app.ui.components.CardShape
import com.samind.app.ui.components.PrimaryButton
import com.samind.app.ui.theme.Primary900
import com.samind.app.ui.theme.SamindGradients
import com.samind.app.ui.theme.SamindTheme

/**
 * The intervention overlay, built from the same kit as every in-app screen —
 * frosted card, kit buttons, kit typography. (The design file has no frame for
 * this moment; it is assembled from the UI kit rather than invented.)
 */
@Composable
fun InterventionOverlay(
    question: String,
    onRefocus: () -> Unit,
    onDismiss: () -> Unit,
) {
    SamindTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.62f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 24.dp)
                    .shadow(16.dp, CardShape, clip = false)
                    .background(SamindGradients.frostedCard, CardShape)
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                MascotBadge()
                Text(
                    question,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Primary900,
                    textAlign = TextAlign.Center,
                )
                PrimaryButton(stringResource(R.string.overlay_open), onRefocus)
                PrimaryButton(stringResource(R.string.overlay_dismiss), onDismiss)
            }
        }
    }
}

/** Floating mascot, same frosted disc language as the kit's circle buttons. */
@Composable
fun MascotBubble(onTap: () -> Unit) {
    SamindTheme {
        Box(
            Modifier
                .size(64.dp)
                .shadow(8.dp, CircleShape, clip = false)
                .background(SamindGradients.frostedControl, CircleShape)
                .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_mascot),
                contentDescription = stringResource(R.string.mascot_content_description),
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun MascotBadge() {
    Box(
        Modifier
            .size(56.dp)
            .background(SamindGradients.frostedControl, CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_mascot),
            contentDescription = null,
            modifier = Modifier.size(30.dp),
        )
    }
}
