package com.samind.app.ui.theme

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Screen-size buckets. The design is drawn at 412 dp wide; everything else is
 * derived from it rather than re-designed.
 */
enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

data class WindowInfo(
    val widthClass: WidthClass,
    val landscape: Boolean,
    /** content never grows past this — a 900 dp-wide button looks broken */
    val contentMaxWidth: Dp,
    val horizontalMargin: Dp,
)

val LocalWindowInfo: ProvidableCompositionLocal<WindowInfo> = compositionLocalOf {
    WindowInfo(WidthClass.COMPACT, false, 480.dp, 16.dp)
}

/**
 * True when the user asked the system to remove animations. Honouring this is
 * not optional: users with vestibular disorders get motion sick, and low-end
 * devices stutter. Continuous practice animations (the breathing fill) stay —
 * they *are* the exercise — everything decorative freezes.
 */
val LocalReducedMotion: ProvidableCompositionLocal<Boolean> = compositionLocalOf { false }

@Composable
fun ProvideAdaptiveEnvironment(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val context = LocalContext.current

    val widthDp = configuration.screenWidthDp
    val info = remember(widthDp, configuration.orientation) {
        val widthClass = when {
            widthDp < 600 -> WidthClass.COMPACT
            widthDp < 840 -> WidthClass.MEDIUM
            else -> WidthClass.EXPANDED
        }
        WindowInfo(
            widthClass = widthClass,
            landscape = configuration.orientation ==
                android.content.res.Configuration.ORIENTATION_LANDSCAPE,
            contentMaxWidth = when (widthClass) {
                WidthClass.COMPACT -> 480.dp
                WidthClass.MEDIUM -> 520.dp
                WidthClass.EXPANDED -> 560.dp
            },
            horizontalMargin = if (widthDp < 360) 12.dp else 16.dp,
        )
    }

    val reduced = remember(context) {
        // 0 disables animations system-wide; some OEMs only zero the transition scale
        val scales = listOf(
            Settings.Global.ANIMATOR_DURATION_SCALE,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
        )
        scales.any { key ->
            Settings.Global.getFloat(context.contentResolver, key, 1f) == 0f
        }
    }

    CompositionLocalProvider(
        LocalWindowInfo provides info,
        LocalReducedMotion provides reduced,
        content = content,
    )
}

/**
 * Centres and caps the content column so phone layouts stay phone-shaped on
 * tablets and foldables instead of stretching.
 */
@Composable
fun AdaptiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val info = LocalWindowInfo.current
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Box(Modifier.widthIn(max = info.contentMaxWidth).fillMaxSize()) { content() }
    }
}
