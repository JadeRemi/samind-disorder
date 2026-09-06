package com.samind.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.samind.app.practice.BreathPhase
import com.samind.app.ui.theme.Primary900

/**
 * The design draws the box-breathing figure as four separate rounded capsules
 * arranged in a pinwheel — not a continuous square outline. Each capsule is
 * hairline-outlined and fills solid from its leading end; the previous sides
 * stay filled, only the active one animates.
 */
@Composable
fun BreathingSquare(
    phase: BreathPhase,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val thickness = 64.dp.toPx()
            val length = size.minDimension * 0.80f
            val outline = Primary900.copy(alpha = 0.55f)

            // pinwheel: each bar is offset so the ends never meet in a corner
            val capsules = listOf(
                // top bar, aligned left
                Capsule(Offset(0f, 0f), Size(length, thickness), horizontal = true, forward = true),
                // right bar, aligned top
                Capsule(
                    Offset(size.width - thickness, thickness * 0.35f),
                    Size(thickness, length),
                    horizontal = false, forward = true,
                ),
                // bottom bar, aligned right
                Capsule(
                    Offset(size.width - length - thickness * 0.35f, size.height - thickness),
                    Size(length, thickness),
                    horizontal = true, forward = false,
                ),
                // left bar, aligned bottom
                Capsule(
                    Offset(0f, size.height - length - thickness * 0.35f),
                    Size(thickness, length),
                    horizontal = false, forward = false,
                ),
            )

            capsules.forEachIndexed { index, capsule ->
                drawCapsuleOutline(capsule, outline, thickness)
                val fill = when {
                    index < phase.ordinal -> 1f
                    index == phase.ordinal -> fraction.coerceIn(0f, 1f)
                    else -> 0f
                }
                if (fill > 0f) drawCapsuleFill(capsule, fill, thickness)
            }
        }
        Box(Modifier.fillMaxSize())
    }
}

private data class Capsule(
    val topLeft: Offset,
    val size: Size,
    val horizontal: Boolean,
    val forward: Boolean,
)

private fun DrawScope.drawCapsuleOutline(capsule: Capsule, color: Color, thickness: Float) {
    drawRoundRect(
        color = color,
        topLeft = capsule.topLeft,
        size = capsule.size,
        cornerRadius = CornerRadius(thickness / 2),
        style = Stroke(width = 1.6.dp.toPx()),
    )
}

private fun DrawScope.drawCapsuleFill(capsule: Capsule, fraction: Float, thickness: Float) {
    val (clipOffset, clipSize) = when {
        capsule.horizontal && capsule.forward ->
            capsule.topLeft to Size(capsule.size.width * fraction, capsule.size.height)
        capsule.horizontal && !capsule.forward ->
            Offset(
                capsule.topLeft.x + capsule.size.width * (1 - fraction),
                capsule.topLeft.y,
            ) to Size(capsule.size.width * fraction, capsule.size.height)
        !capsule.horizontal && capsule.forward ->
            capsule.topLeft to Size(capsule.size.width, capsule.size.height * fraction)
        else ->
            Offset(
                capsule.topLeft.x,
                capsule.topLeft.y + capsule.size.height * (1 - fraction),
            ) to Size(capsule.size.width, capsule.size.height * fraction)
    }
    clipRect(
        left = clipOffset.x,
        top = clipOffset.y,
        right = clipOffset.x + clipSize.width,
        bottom = clipOffset.y + clipSize.height,
    ) {
        drawRoundRect(
            color = Primary900,
            topLeft = capsule.topLeft,
            size = capsule.size,
            cornerRadius = CornerRadius(thickness / 2),
        )
    }
}
