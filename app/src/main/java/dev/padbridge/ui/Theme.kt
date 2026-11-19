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
 * Generated with Material Color Utilities: SchemeVibrant from brand blue #3E64E5 (saturated blue
 * primary, blue-tinted surfaces instead of flat grey), a secondary on the same blue hue at chroma 30
 * so chips and the nav indicator stay in the family, and a warm coral tertiary (#FF7A50 hue) for
 * accents. Regenerate rather than hand-editing single values, so the tones stay consistent.
 */
private val BrandLight = lightColorScheme(
    primary = Color(0xFF004CED), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE1FF), onPrimaryContainer = Color(0xFF0038B6),
    inversePrimary = Color(0xFFB7C4FF), secondary = Color(0xFF525C88),
    onSecondary = Color(0xFFFFFFFF), secondaryContainer = Color(0xFFDCE1FF),
    onSecondaryContainer = Color(0xFF0D1840), tertiary = Color(0xFFA73A15),
    onTertiary = Color(0xFFFFFFFF), tertiaryContainer = Color(0xFFFFDBD0),
    onTertiaryContainer = Color(0xFF3A0A00), error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF), errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A), background = Color(0xFFFAF8FF),
    onBackground = Color(0xFF191B25), surface = Color(0xFFFAF8FF),
    onSurface = Color(0xFF191B25), surfaceVariant = Color(0xFFE0E1F3),
    onSurfaceVariant = Color(0xFF444654), surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F2FF), surfaceContainer = Color(0xFFEDEDFB),
    surfaceContainerHigh = Color(0xFFE7E7F5), surfaceContainerHighest = Color(0xFFE1E1EF),
    outline = Color(0xFF727483), outlineVariant = Color(0xFFC4C5D6),
    inverseSurface = Color(0xFF2E303A), inverseOnSurface = Color(0xFFF0EFFE),
    surfaceBright = Color(0xFFFAF8FF), surfaceDim = Color(0xFFD9D9E7),
)

private val BrandDark = darkColorScheme(
    primary = Color(0xFFB7C4FF), onPrimary = Color(0xFF002682),
    primaryContainer = Color(0xFF0038B6), onPrimaryContainer = Color(0xFFDCE1FF),
    inversePrimary = Color(0xFF004CED), secondary = Color(0xFFBAC4F6),
    onSecondary = Color(0xFF232E56), secondaryContainer = Color(0xFF3A456E),
    onSecondaryContainer = Color(0xFFDCE1FF), tertiary = Color(0xFFFFB59F),
    onTertiary = Color(0xFF5E1700), tertiaryContainer = Color(0xFF852400),
    onTertiaryContainer = Color(0xFFFFDBD0), error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005), errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6), background = Color(0xFF11131C),
    onBackground = Color(0xFFE1E1EF), surface = Color(0xFF11131C),
    onSurface = Color(0xFFE1E1EF), surfaceVariant = Color(0xFF444654),
    onSurfaceVariant = Color(0xFFC4C5D6), surfaceContainerLowest = Color(0xFF0C0E17),
    surfaceContainerLow = Color(0xFF191B25), surfaceContainer = Color(0xFF1D1F29),
    surfaceContainerHigh = Color(0xFF282934), surfaceContainerHighest = Color(0xFF32343F),
    outline = Color(0xFF8E909F), outlineVariant = Color(0xFF444654),
    inverseSurface = Color(0xFFE1E1EF), inverseOnSurface = Color(0xFF2E303A),
    surfaceBright = Color(0xFF373943), surfaceDim = Color(0xFF11131C),
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
