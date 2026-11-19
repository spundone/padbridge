package dev.padbridge.ui

import android.os.Build
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import dev.padbridge.Settings

/**
 * Material 3 Expressive-style glass: sheets, dialogs and menus are translucent, and while one is open the app
 * behind it is blurred (the blur itself needs Android 12+; older versions keep the translucency).
 */
object Glass {
    /** Open sheets and dialogs that want the app blurred behind them. */
    var layers by mutableIntStateOf(0)
    val on get() = Settings.glass
    val blur get() = on && Build.VERSION.SDK_INT >= 31
}

/** Call inside a sheet's or dialog's content: blurs the app behind it while it's shown. */
@Composable
fun GlassBehind() {
    DisposableEffect(Unit) {
        Glass.layers++
        onDispose { Glass.layers-- }
    }
}

@Composable
fun glassSheetColor(): Color =
    if (Glass.on) MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.84f) else BottomSheetDefaults.ContainerColor

@Composable
fun glassScrim(): Color = if (Glass.blur) Color.Black.copy(alpha = 0.16f) else BottomSheetDefaults.ScrimColor

@Composable
fun glassDialogColor(): Color =
    if (Glass.on) MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.86f) else MaterialTheme.colorScheme.surfaceContainerHigh

@Composable
fun glassMenuColor(): Color =
    if (Glass.on) MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainer
