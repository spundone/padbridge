package dev.padbridge.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.activity.enableEdgeToEdge
import dev.padbridge.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import dev.padbridge.C

/*
 * PadBridge's own palette, used when wallpaper (dynamic) color is off or unavailable (Android 9-11).
 * Seeded from brand blue #3E64E5 with a slate secondary and a teal tertiary, laid out on the
 * Material 3 tone scale (primary 40/90/100/10 light, 80/20/30/90 dark).
 */
private val BrandLight = lightColorScheme(
    primary = Color(0xFF3557C8), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF00164F),
    inversePrimary = Color(0xFFB6C4FF),
    secondary = Color(0xFF595D72), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDEE1F9), onSecondaryContainer = Color(0xFF161B2C),
    tertiary = Color(0xFF006A60), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9EF2E4), onTertiaryContainer = Color(0xFF00201C),
    error = Color(0xFFBA1A1A), onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1A1B21),
    surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1A1B21),
    surfaceVariant = Color(0xFFE2E1EC), onSurfaceVariant = Color(0xFF45464F),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4), surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE3E1E9),
    outline = Color(0xFF767680), outlineVariant = Color(0xFFC6C5D0),
    inverseSurface = Color(0xFF2F3036), inverseOnSurface = Color(0xFFF2F0F7),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFFB6C4FF), onPrimary = Color(0xFF002780),
    primaryContainer = Color(0xFF1B3EAF), onPrimaryContainer = Color(0xFFDCE1FF),
    inversePrimary = Color(0xFF3557C8),
    secondary = Color(0xFFC2C5DD), onSecondary = Color(0xFF2B3042),
    secondaryContainer = Color(0xFF424659), onSecondaryContainer = Color(0xFFDEE1F9),
    tertiary = Color(0xFF82D5C8), onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048), onTertiaryContainer = Color(0xFF9EF2E4),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121318), onBackground = Color(0xFFE3E1E9),
    surface = Color(0xFF121318), onSurface = Color(0xFFE3E1E9),
    surfaceVariant = Color(0xFF45464F), onSurfaceVariant = Color(0xFFC6C5D0),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25), surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    outline = Color(0xFF90909A), outlineVariant = Color(0xFF45464F),
    inverseSurface = Color(0xFFE3E1E9), inverseOnSurface = Color(0xFF2F3036),
)

/** AMOLED: true-black background, containers lifted just enough to stay distinguishable. */
private fun ColorScheme.pureBlack() = copy(
    background = Color.Black, surface = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0B0B0E),
    surfaceContainer = Color(0xFF121216), surfaceContainerHigh = Color(0xFF1A1B20),
    surfaceContainerHighest = Color(0xFF24252B),
)

/** Bumped by the settings screen so theme changes apply immediately. */
object ThemeState {
    var version by mutableIntStateOf(0)
}

/** Material 3 Expressive theme: wallpaper or PadBridge colors, light/dark/system, optional pure black. */
@Composable
fun PadBridgeTheme(content: @Composable () -> Unit) {
    ThemeState.version // recompose on settings changes
    val systemDark = isSystemInDarkTheme()
    val dark = when (Settings.themeMode) { 1 -> false; 2 -> true; else -> systemDark }
    val ctx = LocalContext.current
    val dynamic = Settings.dynamicColor && Build.VERSION.SDK_INT >= 31
    var scheme = when {
        dynamic -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> BrandDark
        else -> BrandLight
    }
    if (dark && Settings.pureBlack) scheme = scheme.pureBlack()
    syncViewColors(scheme)
    SideEffect {
        syncViewColors(scheme)
        // Status/navigation bar icons follow the app's mode, not just the system's.
        (ctx as? androidx.activity.ComponentActivity)?.enableEdgeToEdge(
            statusBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
        )
    }
    MaterialExpressiveTheme(colorScheme = scheme, motionScheme = MotionScheme.expressive(), content = content)
}

private val AodText = Color(0xFF8A8A8A)
private val AodLit = Color(0xFFBDBDBD)
private val AodScheme = darkColorScheme(
    primary = AodLit, onPrimary = Color.Black,
    primaryContainer = Color.Black, onPrimaryContainer = AodLit,
    secondary = AodText, onSecondary = Color.Black,
    secondaryContainer = Color.Black, onSecondaryContainer = AodText,
    tertiary = AodText, onTertiary = Color.Black,
    tertiaryContainer = Color.Black, onTertiaryContainer = AodText,
    background = Color.Black, onBackground = AodText,
    surface = Color.Black, onSurface = AodText,
    surfaceVariant = Color.Black, onSurfaceVariant = Color(0xFF6A6A6A),
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color.Black, surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black, surfaceContainerHighest = Color.Black,
    outline = Color(0xFF4A4A4A), outlineVariant = Color(0xFF333333),
)

/**
 * Always-on-display look, like a watch face in ambient mode: the same screens and controls,
 * drawn as thin dim outlines on true black (OLED pixels off). Restores the normal colors on exit.
 */
@Composable
fun AodTheme(content: @Composable () -> Unit) {
    val outer = androidx.compose.material3.MaterialTheme.colorScheme
    applyAodViewColors()
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { C.AOD = false; syncViewColors(outer) }
    }
    androidx.compose.material3.MaterialTheme(colorScheme = AodScheme, content = content)
}

private fun applyAodViewColors() {
    C.AOD = true
    C.BG = android.graphics.Color.BLACK
    C.SURFACE = android.graphics.Color.BLACK
    C.SURFACE2 = android.graphics.Color.BLACK
    C.SURFACE3 = android.graphics.Color.BLACK
    C.STROKE = 0xFF3C3C3C.toInt()
    C.AOD_LINE = 0xFF3C3C3C.toInt()
    C.ACCENT = AodLit.toArgb()
    C.ON_ACCENT = AodLit.toArgb()
    C.ACCENT_DIM = android.graphics.Color.BLACK
    C.ON_ACCENT_DIM = 0xFF9E9E9E.toInt()
    C.TERTIARY = AodText.toArgb()
    C.TEXT = AodText.toArgb()
    C.MUTED = 0xFF5E5E5E.toInt()
}

/** The Canvas-drawn trackpad / keyboard / gamepad read their colors from [C]. */
internal fun syncViewColors(s: ColorScheme) {
    if (C.AOD) return
    C.BG = s.surface.toArgb()
    C.SURFACE = s.surfaceContainer.toArgb()
    C.SURFACE2 = s.surfaceContainerHigh.toArgb()
    C.SURFACE3 = s.surfaceContainerHighest.toArgb()
    C.STROKE = s.outlineVariant.toArgb()
    C.ACCENT = s.primary.toArgb()
    C.ON_ACCENT = s.onPrimary.toArgb()
    C.ACCENT_DIM = s.primaryContainer.toArgb()
    C.ON_ACCENT_DIM = s.onPrimaryContainer.toArgb()
    C.TERTIARY = s.tertiary.toArgb()
    C.TEXT = s.onSurface.toArgb()
    C.MUTED = s.onSurfaceVariant.toArgb()
}
