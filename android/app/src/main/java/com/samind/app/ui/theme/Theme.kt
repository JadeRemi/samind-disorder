package com.samind.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samind.app.R

// Design tokens — see docs/DESIGN_SYSTEM.md. Values come from the Figma file,
// not from taste: change them there first.

val Primary900 = Color(0xFF4B6350)
val Primary800 = Color(0xFF738C78)
val Primary400 = Color(0xFF96AC9B)
val Primary200 = Color(0xFFBAC5BC)
val Neutral900 = Color(0xFF1D1B20)
val Neutral800 = Color(0xFF3D3A36)
val Neutral0 = Color(0xFFFFFFFF)
val SurfaceTint = Color(0xFFF4EFF7)
val SemanticInfo = Color(0xFFEFF6FF)
val SemanticError = Color(0xFFBA1A1A)

val NunitoSans = FontFamily(Font(R.font.nunito_sans))

// control fills are gradients throughout the design, never flat
object SamindGradients {
    val controlActive = Brush.verticalGradient(
        listOf(Color(0xFFDCE8DD), Color(0xFFBDD2C0)),
    )
    val controlDisabled = Brush.verticalGradient(
        listOf(Color(0xFFE9E9E9), Color(0xFFD6D6D6)),
    )
    val controlProcess = Brush.verticalGradient(
        listOf(Color(0xFFDCE6F5), Color(0xFFC3D3EC)),
    )
    val controlError = Brush.verticalGradient(
        listOf(Color(0xFFF6DADA), Color(0xFFECC0C1)),
    )
    val surfaceVeil = Brush.verticalGradient(
        listOf(Color(0x00FFFFFF), Color(0x33FFFFFF)),
    )
    val dialog = Brush.verticalGradient(
        listOf(Color(0xFFF2F7F2), Color(0xFFDDEADE)),
    )
    val voiceOrb = Brush.linearGradient(
        listOf(Color(0xFFDCF0FF), Color(0xFFDFF6DC), Color(0xFFCFE9F7)),
    )
}

// one easing family for every interactive transition
object SamindMotion {
    val standard = CubicBezierEasing(0.22f, 0.61f, 0.36f, 1f)
    const val SHORT = 180
    const val MEDIUM = 280
    const val LONG = 420
}

private val colors = lightColorScheme(
    primary = Primary900,
    onPrimary = Neutral0,
    secondary = Primary800,
    background = Color(0xFFEFF3EE),
    surface = Neutral0,
    onBackground = Neutral900,
    onSurface = Neutral900,
    error = SemanticError,
)

// radii: pill 100, card 24, chip 6 (design file)
private val shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(100.dp),
)

private fun nunito(weight: Int, size: Int, line: Int) = TextStyle(
    fontFamily = NunitoSans,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = line.sp,
)

private val typography = Typography(
    displayLarge = nunito(900, 100, 120),   // practice counters
    displayMedium = nunito(400, 36, 43),    // Text 4
    headlineMedium = nunito(600, 24, 29),
    titleLarge = nunito(600, 18, 22),       // Text 1 SemiBold
    titleMedium = nunito(600, 16, 19),      // Text 2
    bodyLarge = nunito(400, 18, 22),        // Text 1
    bodyMedium = nunito(200, 22, 26),       // Text 3 (ExtraLight)
    bodySmall = nunito(400, 16, 19),
    labelLarge = nunito(500, 16, 19),       // Link 1
    labelMedium = nunito(500, 14, 17),
    labelSmall = nunito(500, 12, 15),
)

@Composable
fun SamindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        shapes = shapes,
        typography = typography,
        content = content,
    )
}
