package dev.padbridge.ui

import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.Gamepad
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Swipe
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Laptop
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.BrightnessHigh
import androidx.compose.material.icons.rounded.BrightnessLow
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DesktopWindows
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import dev.padbridge.DpadView
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.KeyboardAlt
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.VerticalSplit
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.outlined.Bedtime as BedtimeOutlined
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.GpsFixed
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.KeyboardHide
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.StayCurrentLandscape
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import dev.padbridge.AirMouseView
import dev.padbridge.ClickBar
import dev.padbridge.GamepadView
import dev.padbridge.HidManager
import dev.padbridge.HostOs
import dev.padbridge.ImeCaptureView
import dev.padbridge.KeyboardView
import dev.padbridge.Keys
import dev.padbridge.Settings
import dev.padbridge.TrackpadView

private data class Chip(val label: String, val action: () -> Unit)

@Composable
private fun AodButton(aod: Boolean, onAod: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onAod) {
        Icon(if (aod) Icons.Rounded.Bedtime else Icons.Outlined.BedtimeOutlined, if (aod) "Exit AOD mode" else "AOD mode")
    }
}

/** Thin outline used in AOD mode, where every container is black. */
@Composable
private fun Modifier.aodOutline(aod: Boolean, shape: androidx.compose.ui.graphics.Shape): Modifier =
    if (aod) border(1.dp, MaterialTheme.colorScheme.outline, shape) else this

/**
 * The screen's title chip. In AOD mode it turns into a watch-style readout: time and connection.
 */
@Composable
private fun TitleChip(title: String, aod: Boolean, hid: HidUi?) {
    val shape = RoundedCornerShape(12.dp)
    Surface(shape = shape, color = MaterialTheme.colorScheme.secondaryContainer, modifier = Modifier.aodOutline(aod, shape)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (aod) {
                Text(rememberClock(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(6.dp).background(
                        if (hid?.connected == true) androidx.compose.ui.graphics.Color(0xFF4F7F5C)
                        else MaterialTheme.colorScheme.outline,
                        androidx.compose.foundation.shape.CircleShape,
                    )
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (hid?.connected == true) hid.hostName ?: "Connected" else "Not connected",
                    style = MaterialTheme.typography.labelMedium, maxLines = 1,
                )
            } else {
                Text(title, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun rememberClock(): String {
    val ctx = LocalContext.current
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) { kotlinx.coroutines.delay(60_000 - System.currentTimeMillis() % 60_000); now = System.currentTimeMillis() }
    }
    val pattern = if (android.text.format.DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
    return remember(now / 60_000) { java.text.SimpleDateFormat(pattern, java.util.Locale.getDefault()).format(java.util.Date(now)) }
}

/** Shifts AOD content a few dp each minute so the same OLED pixels never stay lit (burn-in protection). */
@Composable
fun Modifier.aodPixelShift(aod: Boolean): Modifier {
    if (!aod) return this
    var shift by remember { mutableStateOf(0 to 0) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60_000)
            shift = (-3..3).random() to (-3..3).random()
        }
    }
    return offset(shift.first.dp, shift.second.dp)
}

@Composable
private fun FullscreenButton(fullscreen: Boolean, onToggle: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onToggle) {
        Icon(
            if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
            if (fullscreen) "Show app bars" else "Hide app bars",
        )
    }
}

@Composable
private fun ChipRow(chips: List<Chip>, modifier: Modifier = Modifier) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(chips) { c ->
            AssistChip(
                onClick = { dev.padbridge.Haptics.tick(); c.action() },
                label = { Text(c.label) },
                shape = RoundedCornerShape(12.dp),
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                border = null,
            )
        }
    }
}

private fun quickKeys(os: HostOs): List<Chip> {
    val mod = when (os) { HostOs.MAC -> "⌘"; else -> "Ctrl+" }
    fun k(code: Int, mods: Int = 0): () -> Unit = { HidManager.tap(code, mods) }
    return listOf(
        Chip("Esc", k(Keys.ESC)), Chip("Tab", k(Keys.TAB)),
        Chip("←", k(Keys.LEFT)), Chip("↑", k(Keys.UP)), Chip("↓", k(Keys.DOWN)), Chip("→", k(Keys.RIGHT)),
        Chip("⌫", k(Keys.BACKSPACE)), Chip("Del", k(Keys.DELETE)), Chip("⏎", k(Keys.ENTER)),
        Chip("Home", k(Keys.HOME)), Chip("End", k(Keys.END)),
        Chip("${mod}A", k(Keys.letter('a'), os.primary)), Chip("${mod}C", k(Keys.letter('c'), os.primary)),
        Chip("${mod}V", k(Keys.letter('v'), os.primary)), Chip("${mod}Z", k(Keys.letter('z'), os.primary)),
    )
}

// ====================================================================== Trackpad

@Composable
fun TrackpadScreen(
    hid: HidUi, osVersion: Int, fullscreen: Boolean = false, onToggleFullscreen: () -> Unit = {},
    onAod: () -> Unit = {}, aod: Boolean = false, onFullDisplay: () -> Unit = {},
    onGoConnect: () -> Unit = {}, onAirMouse: () -> Unit = {}, onTablet: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val os = remember(osVersion) { Settings.hostOs }
    var kbOpen by remember { mutableStateOf(false) }
    var guide by remember { mutableStateOf(false) }
    var orientation by remember { mutableStateOf(Settings.trackpadOrientation) }
    val ime = remember { ImeCaptureView(ctx) }
    val activity = ctx as? dev.padbridge.MainActivity

    // Like DeX's rotate button: auto, or lock the pad portrait / landscape.
    androidx.compose.runtime.DisposableEffect(orientation) {
        activity?.setTrackpadOrientation(orientation)
        onDispose { activity?.setTrackpadOrientation(0) }
    }

    LaunchedEffect(kbOpen) {
        if (kbOpen) ime.post { ime.showKeyboard() } else ime.hideKeyboard()
    }

    val menu = listOf(
        MenuEntry("Trackpad only, edge to edge", Icons.Rounded.OpenInFull, action = onFullDisplay),
        MenuEntry(if (fullscreen) "Show app bars" else "Hide app bars",
            if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, action = onToggleFullscreen),
        MenuEntry("Air mouse", Icons.Rounded.Mouse, divider = true, action = onAirMouse),
        MenuEntry("Drawing tablet", Icons.Rounded.Draw, action = onTablet),
    ) + (if (dev.padbridge.BuildConfig.ROOT_FEATURES) listOf(
        MenuEntry("Floating trackpad over other apps", androidx.compose.material.icons.Icons.Rounded.Layers) { dev.padbridge.FloatingPadService.start(ctx) },
    ) else emptyList()) + listOf(
        MenuEntry("Gesture guide", Icons.Rounded.TipsAndUpdates, divider = true) { guide = true },
        MenuEntry("Trackpad settings", Icons.Rounded.Tune) { SettingsLink.open(SettingsPage.TRACKPAD) },
        MenuEntry("Auto-rotate", Icons.Rounded.ScreenRotation, checked = orientation == 0, divider = true) {
            orientation = 0; Settings.trackpadOrientation = 0
        },
        MenuEntry("Lock portrait", Icons.Rounded.StayCurrentPortrait, checked = orientation == 1) {
            orientation = 1; Settings.trackpadOrientation = 1
        },
        MenuEntry("Lock landscape", Icons.Rounded.StayCurrentLandscape, checked = orientation == 2) {
            orientation = 2; Settings.trackpadOrientation = 2
        },
    )
    val kbButton: @Composable () -> Unit = {
        FilledTonalIconToggleButton(
            checked = kbOpen, onCheckedChange = { kbOpen = it },
            modifier = Modifier.size(IconButtonDefaults.mediumContainerSize(IconButtonDefaults.IconButtonWidthOption.Wide))
                .aodOutline(aod, IconButtonDefaults.mediumRoundShape),
            shape = IconButtonDefaults.mediumRoundShape,
        ) { Icon(if (kbOpen) Icons.Rounded.KeyboardHide else Icons.Rounded.Keyboard, "Keyboard") }
    }
    // Phones in landscape: the toolbar becomes a slim column at the side, so the pad gets the whole height
    // (a top row took a third of it).
    val shortLandscape = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 520
    val padColumn: @Composable (Modifier) -> Unit = { m ->
        Column(m) {
            if (shortLandscape && aod) ConnectionChip(hid, true, Modifier.padding(start = 4.dp, bottom = 8.dp))
            NotConnectedBanner(hid, onGoConnect, Modifier.padding(bottom = 8.dp))
            ScreenshotGestureNotice(Modifier.padding(bottom = 8.dp))

            AnimatedVisibility(kbOpen, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(bottom = 8.dp)) {
                    AndroidView({ ime }, Modifier.fillMaxWidth().height(52.dp))
                    ChipRow(quickKeys(os), Modifier.padding(top = 8.dp))
                }
            }

            AndroidView(
                factory = { TrackpadView(it) },
                update = { v -> hid.version; osVersion; aod; v.invalidate() },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )

            if (Settings.showClickButtons) {
                AndroidView(
                    factory = { ClickBar(it) },
                    update = { v -> aod; v.invalidate() },
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(top = 8.dp),
                )
            }
        }
    }

    if (shortLandscape) {
        Row(Modifier.fillMaxSize().padding(start = 8.dp, end = 4.dp, bottom = 8.dp)) {
            padColumn(Modifier.weight(1f).fillMaxHeight())
            Column(
                Modifier.fillMaxHeight().padding(start = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                kbButton()
                Spacer(Modifier.weight(1f))
                AodButton(aod, onAod)
                OverflowMenu(menu)
            }
        }
    } else {
        Column(Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                if ((hid.connected && !LocalTopBarStatus.current) || aod) ConnectionChip(hid, aod)
                Spacer(Modifier.weight(1f))
                AodButton(aod, onAod)
                OverflowMenu(menu)
                Spacer(Modifier.width(4.dp))
                kbButton()
            }
            padColumn(Modifier.weight(1f).fillMaxWidth())
        }
    }

    if (guide) GestureGuide(os) { guide = false }
}

/** Cheat sheet of what every gesture does right now (DeX shows a similar hint screen). */
@Composable
private fun GestureGuide(os: HostOs, onDismiss: () -> Unit) {
    val builtIn = listOf(
        "Tap" to "Left click",
        "Double tap" to "Double click",
        "Tap, then drag" to "Drag / select",
        "Two-finger move" to if (Settings.twoFingerNavigate) "Scroll (sideways = back / forward)" else "Scroll",
        "Pinch" to "Zoom",
        "Right / bottom edge slide" to if (Settings.edgeScroll) "Scroll" else "Off",
    )
    androidx.compose.material3.ModalBottomSheet(onDismissRequest = onDismiss, containerColor = glassSheetColor(), scrimColor = glassScrim()) {
        GlassBehind()
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("Gestures on ${os.label}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Change them in Settings → Gestures.", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(builtIn) { (g, a) -> GuideRow(g, a, null) }
            items(dev.padbridge.GestureSlot.entries.toList()) { slot ->
                val a = Settings.gesture(slot, os)
                if (a != dev.padbridge.GAction.NONE &&
                    !(Settings.threeFingerDrag && slot.group == "Three fingers" && slot.name.startsWith("THREE_") && slot != dev.padbridge.GestureSlot.THREE_TAP)
                ) GuideRow(slot.title, a.label, shortcutText(a, os))
            }
            if (Settings.threeFingerDrag) item { GuideRow("Three-finger move", "Drag", null) }
        }
    }
}

@Composable
private fun GuideRow(gesture: String, action: String, detail: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(gesture, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(action, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium)
            if (detail != null) Text(detail, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ====================================================================== Keyboard

@Composable
fun KeyboardScreen(
    hid: HidUi, osVersion: Int, fullscreen: Boolean = false, onToggleFullscreen: () -> Unit = {},
    onFullDisplay: () -> Unit = {}, onGoConnect: () -> Unit = {}, aod: Boolean = false, onAod: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val os = remember(osVersion) { Settings.hostOs }
    var skipped by remember { mutableStateOf(0) }
    var cfg by remember { mutableStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    val landscapeNow = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val showRows = remember(cfg, landscapeNow) { Settings.kbShowRows(landscapeNow) }
    val ime = remember {
        ImeCaptureView(ctx).apply { onSkipped = { n -> skipped = n } }
    }
    val imeUp = WindowInsets.isImeVisible

    LaunchedEffect(skipped) {
        if (skipped > 0) { kotlinx.coroutines.delay(3500); skipped = 0 }
    }

    Column(Modifier.fillMaxSize().padding(bottom = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
        ) {
            // AOD: a watch-style clock and connection readout instead of the text field.
            if (aod) { ConnectionChip(hid, true); Spacer(Modifier.weight(1f)) }
            else AndroidView({ ime }, Modifier.weight(1f).height(52.dp))
            Spacer(Modifier.width(4.dp))
            AodButton(aod, onAod)
            if (!aod) androidx.compose.material3.IconButton(onClick = { sheet = true }) {
                Icon(Icons.Rounded.Dashboard, "Keyboard layout and size")
            }
            if (!aod) OverflowMenu(
                listOf(
                    MenuEntry("Keyboard only, edge to edge", Icons.Rounded.OpenInFull, action = onFullDisplay),
                    MenuEntry(if (fullscreen) "Show app bars" else "Hide app bars",
                        if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, action = onToggleFullscreen),
                    MenuEntry("Layout and size", Icons.Rounded.Dashboard, divider = true) { sheet = true },
                    MenuEntry("Keyboard settings", Icons.Rounded.Tune) { SettingsLink.open(SettingsPage.KEYBOARD) },
                )
            )
        }
        NotConnectedBanner(hid, onGoConnect, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        ScreenshotGestureNotice(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        AnimatedVisibility(skipped > 0) {
            Text(
                "$skipped character(s) can't be typed with a US layout and were skipped.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        val laptopNow = rememberLaptopDeck(cfg)
        if (showRows && !imeUp && !laptopNow && !aod) {
            ChipRow(os.shortcuts.map { s -> Chip(s.label) { HidManager.combo(s.combo) } }, Modifier.padding(top = 10.dp))
            MediaRow(Modifier.padding(top = 8.dp))
        }

        if (!imeUp) {
            KeyboardArea(hid, os, cfg, resizable = true, modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 4.dp), aod = aod)
        }
    }

    if (sheet) KeyboardLayoutSheet(onChanged = { cfg++; ime.refreshMode() }) { sheet = false }
}

/**
 * The on-screen keyboard plus a drag handle on top to resize it (double-tap the handle to fill again).
 * Width and alignment come from the layout sheet. Everything is remembered per orientation.
 */
@Composable
private fun KeyboardArea(hid: HidUi, os: HostOs, cfg: Int, resizable: Boolean, modifier: Modifier = Modifier, aod: Boolean = false) {
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    var heightDp by remember(landscape, cfg) { mutableStateOf(Settings.kbHeight(landscape)) }
    val layout = remember(landscape, cfg) { Settings.kbLayout(landscape) }
    val widthFrac = remember(landscape, cfg) { Settings.kbWidth(landscape) }
    val align = remember(landscape, cfg) { Settings.kbAlign(landscape) }
    val gap = remember(cfg) { Settings.kbSplitGap }
    val preview = remember(cfg) { Settings.kbPreview }
    val flick = remember(cfg) { Settings.kbFlick }
    val spaceCursor = remember(cfg) { Settings.kbSpaceCursor }
    val trackPoint = remember(cfg) { Settings.kbTrackPoint }
    val predictOn = remember(cfg) { Settings.kbPredict }

    val freePad = remember(cfg) { resizable && Settings.kbFreePad }
    val laptop = rememberLaptopDeck(cfg)
    val tablet = isTablet()

    @Composable
    fun KeysView(mod: Modifier) = AndroidView(
        factory = { KeyboardView(it) },
        update = { v ->
            hid.version; aod
            v.keyPreview = preview
            v.flick = flick
            v.spaceCursor = spaceCursor
            v.trackPoint = trackPoint
            v.configure(os, layout, gap)
            v.invalidate()
        },
        modifier = mod,
    )

    @Composable
    fun Keys(mod: Modifier) = Column(mod) {
        if (predictOn) SuggestionStrip()
        KeysView(Modifier.weight(1f).fillMaxWidth())
    }

    if (laptop) {
        LaptopDeck(hid, modifier, cfg) { m -> Keys(m) }
        return
    }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val handle = if (resizable) 22f else 0f
        val maxH = maxHeight.value - handle
        val minH = 110f.coerceAtMost(maxH)
        // On tablets "fill" stops at about half the width tall, so keys stay key-shaped; the rest is trackpad.
        // On phones in portrait, "fill" stops when keys are about 1.2x as tall as wide (a short full keyboard
        // with the trackpad above it, rather than tall skinny keys).
        val fill = when {
            tablet && resizable -> minOf(maxH, maxWidth.value * 0.5f)
            !landscape && resizable -> minOf(maxH, maxWidth.value / layout.cols * 1.2f * layout.rows + 8f)
            else -> maxH
        }
        val h = if (!resizable || heightDp <= 0f) fill else heightDp.coerceIn(minH, maxH)
        val freeH = maxH - h
        // Narrow keyboard pushed to one side: the rest of the row becomes a trackpad (e.g. numpad + pad).
        val sidePad = freePad && widthFrac <= 0.8f && align != 1
        Column(Modifier.fillMaxSize()) {
            // Space freed by shrinking the keyboard becomes a trackpad.
            if (freePad && freeH >= 72f) {
                FreeSpacePad(hid, Modifier.weight(1f).fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp))
            } else {
                Spacer(Modifier.weight(1f))
            }
            if (resizable) {
                Box(
                    Modifier.fillMaxWidth().height(handle.dp)
                        .pointerInput(landscape) {
                            detectVerticalDragGestures(onDragEnd = { Settings.setKbHeight(landscape, heightDp) }) { change, dy ->
                                change.consume()
                                val cur = if (heightDp <= 0f) maxH else heightDp
                                heightDp = (cur - dy / density).coerceIn(minH, maxH)
                            }
                        }
                        .pointerInput(landscape) {
                            detectTapGestures(onDoubleTap = { heightDp = 0f; Settings.setKbHeight(landscape, 0f) })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(width = 40.dp, height = 4.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            if (sidePad) {
                Row(
                    Modifier.fillMaxWidth().height(h.dp).padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (align == 2) FreeSpacePad(hid, Modifier.weight(1f - widthFrac).fillMaxHeight())
                    Keys(Modifier.weight(widthFrac).fillMaxHeight())
                    if (align == 0) FreeSpacePad(hid, Modifier.weight(1f - widthFrac).fillMaxHeight())
                }
            } else {
                Box(
                    Modifier.fillMaxWidth().height(h.dp).padding(horizontal = 4.dp),
                    contentAlignment = when (align) { 0 -> Alignment.CenterStart; 2 -> Alignment.CenterEnd; else -> Alignment.Center },
                ) {
                    Keys(Modifier.fillMaxWidth(widthFrac).fillMaxHeight())
                }
            }
        }
    }
}

/** True on tablets (smallest width ≥ 600 dp). */
@Composable
fun isTablet(): Boolean = androidx.compose.ui.platform.LocalConfiguration.current.smallestScreenWidthDp >= 600

@Composable
private fun rememberLaptopDeck(cfg: Int): Boolean {
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val tablet = isTablet()
    return remember(cfg, landscape, tablet) { Settings.kbLaptop(landscape, tablet && landscape) }
}

/**
 * Laptop deck: the keyboard across the top and a trackpad under it, like a laptop's palm rest.
 *
 * Adjustable (per orientation, unless locked in the layout sheet):
 * - the bar under the keys: drag up/down to resize the keyboard, double-tap to reset;
 * - the grip under the trackpad: drag sideways to move it, double-tap to center it again;
 * - the corner grips under the trackpad: drag to make it wider or narrower.
 * Keyboard width and a full-width trackpad are set in the layout sheet.
 */
@Composable
private fun LaptopDeck(hid: HidUi, modifier: Modifier, cfg: Int, keys: @Composable (Modifier) -> Unit) {
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    var keysFrac by remember(landscape, cfg) { mutableStateOf(Settings.deckKeys(landscape)) }
    var padW by remember(landscape, cfg) { mutableStateOf(Settings.deckPadWidth(landscape)) }
    var padX by remember(landscape, cfg) { mutableStateOf(Settings.deckPadPos(landscape)) }
    var kbW by remember(landscape, cfg) { mutableStateOf(Settings.deckKbWidth(landscape)) }
    val full = remember(landscape, cfg) { Settings.deckPadFull(landscape) }
    val locked = remember(cfg) { Settings.deckLocked }
    val grip = MaterialTheme.colorScheme.outlineVariant
    val gripActive = MaterialTheme.colorScheme.primary
    var active by remember { mutableStateOf("") }
    // How far the screen's center is from this area's center (dp). The navigation rail on tablets pushes the
    // area right, which would leave a "centered" keyboard off-center on the device itself.
    val rootView = androidx.compose.ui.platform.LocalView.current.rootView
    var shift by remember { mutableStateOf(0f) }

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.padding(horizontal = 8.dp).onGloballyPositioned { c ->
            val left = c.positionInWindow().x
            val s = (rootView.width / 2f - (left + c.size.width / 2f)) / density
            if (kotlin.math.abs(s - shift) > 0.5f) shift = s
        }
    ) {
        val totalW = maxWidth.value
        // The widest strip of this area that is symmetric around the screen's center. Keys and the trackpad are
        // laid out in it; a full-width trackpad still uses the whole area. Never narrower than 75% of the area.
        val frameW = (totalW - 2f * kotlin.math.abs(shift)).coerceIn(totalW * 0.75f, totalW)
        val frameX = if (shift < 0f) 0f else totalW - frameW
        val handleH = if (locked) 12f else 20f
        val gripH = if (locked || full) 4f else 22f
        val avail = maxHeight.value - handleH - gripH
        val deckW = (if (kbW > 0f) totalW * kbW else minOf(totalW, 1100f)).coerceAtMost(frameW)
        val autoKeys = minOf(avail * 0.62f, deckW * 0.36f)
        val minKeys = 120f.coerceAtMost(avail * 0.5f)
        val maxKeys = (avail - 90f).coerceAtLeast(minKeys)
        val keysH = (if (keysFrac > 0f) avail * keysFrac else autoKeys).coerceIn(minKeys, maxKeys)
        val pw = if (full) totalW else (frameW * padW).coerceIn(minOf(160f, frameW), frameW)
        val px = if (full) 0f else frameX + (frameW - pw) * padX.coerceIn(0f, 1f)

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.offset(x = frameX.dp).width(frameW.dp).height(keysH.dp), contentAlignment = Alignment.Center) {
                keys(Modifier.width(deckW.dp).fillMaxHeight())
            }
            // Bar under the keys: drag up/down in the middle to resize the keys' height, drag the end grips
            // (under the keyboard's corners) sideways to make it wider or narrower. Double-tap resets both.
            val geo by androidx.compose.runtime.rememberUpdatedState(
                floatArrayOf(avail, autoKeys, minKeys, maxKeys, deckW, frameX, frameW, totalW)
            )
            val kx0 = frameX + (frameW - deckW) / 2f
            Box(
                Modifier.fillMaxWidth().height(handleH.dp).then(
                    if (locked) Modifier else Modifier
                        .pointerInput(landscape) {
                            detectDragGestures(
                                onDragStart = { o ->
                                    val g = geo
                                    val x = o.x / density
                                    val l = g[5] + (g[6] - g[4]) / 2f; val r = l + g[4]
                                    active = when {
                                        kotlin.math.abs(x - l) < 56f -> "kbLeft"
                                        kotlin.math.abs(x - r) < 56f -> "kbRight"
                                        else -> "keys"
                                    }
                                },
                                onDragEnd = {
                                    active = ""
                                    Settings.setDeckKeys(landscape, keysFrac); Settings.setDeckKbWidth(landscape, kbW)
                                },
                                onDragCancel = { active = "" },
                            ) { change, drag ->
                                change.consume()
                                val g = geo
                                when (active) {
                                    "keys" -> {
                                        val cur = if (keysFrac > 0f) g[0] * keysFrac else g[1]
                                        keysFrac = ((cur + drag.y / density).coerceIn(g[2], g[3])) / g[0]
                                    }
                                    // Symmetric: the keyboard stays centered, both edges follow the finger.
                                    "kbLeft", "kbRight" -> {
                                        val dx = drag.x / density * (if (active == "kbLeft") -2f else 2f)
                                        val nw = (g[4] + dx).coerceIn(g[7] * 0.4f, g[6])
                                        kbW = nw / g[7]
                                    }
                                }
                            }
                        }
                        .pointerInput(landscape) {
                            detectTapGestures(onDoubleTap = {
                                keysFrac = 0f; kbW = 0f
                                Settings.setDeckKeys(landscape, 0f); Settings.setDeckKbWidth(landscape, 0f)
                            })
                        }
                ),
            ) {
                if (!locked) {
                    Box(
                        Modifier.align(Alignment.Center).offset(x = (frameX + frameW / 2f - totalW / 2f).dp).size(width = 48.dp, height = 4.dp)
                            .background(if (active == "keys") gripActive else grip, androidx.compose.foundation.shape.CircleShape)
                    )
                    // Width grips under the keyboard's bottom corners.
                    Box(
                        Modifier.align(Alignment.CenterStart).offset(x = (kx0 + 6f).dp).size(width = 16.dp, height = 4.dp)
                            .background(if (active == "kbLeft") gripActive else grip, androidx.compose.foundation.shape.CircleShape)
                    )
                    Box(
                        Modifier.align(Alignment.CenterStart).offset(x = (kx0 + deckW - 22f).dp).size(width = 16.dp, height = 4.dp)
                            .background(if (active == "kbRight") gripActive else grip, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                FreeSpacePad(
                    hid,
                    Modifier.offset(x = px.dp).width(pw.dp).fillMaxHeight().padding(bottom = gripH.dp),
                )
                if (!locked && !full) {
                    // Pills drawn under the pad: resize corners at both ends, move grip in the middle.
                    Row(
                        Modifier.align(Alignment.BottomStart).offset(x = px.dp).width(pw.dp).height(gripH.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        GripPill(16, active == "left", grip, gripActive)
                        GripPill(40, active == "move", grip, gripActive)
                        GripPill(16, active == "right", grip, gripActive)
                    }
                    // Touch handling on a strip that doesn't move, so drags stay smooth while the pad moves.
                    Box(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth().height(gripH.dp)
                            .pointerInput(landscape, frameW, frameX) {
                                fun curW() = (frameW * padW).coerceIn(minOf(160f, frameW), frameW)
                                fun curX() = frameX + (frameW - curW()) * padX.coerceIn(0f, 1f)
                                detectHorizontalDragGestures(
                                    onDragStart = { o ->
                                        val x = o.x / density; val l = curX(); val r = l + curW()
                                        active = when {
                                            x < l - 24f || x > r + 24f -> ""
                                            x < l + 44f -> "left"
                                            x > r - 44f -> "right"
                                            else -> "move"
                                        }
                                    },
                                    onDragEnd = {
                                        active = ""
                                        Settings.setDeckPadWidth(landscape, padW); Settings.setDeckPadPos(landscape, padX)
                                    },
                                ) { change, dxPx ->
                                    change.consume()
                                    val dx = dxPx / density
                                    val w = curW(); val l = curX(); val r = l + w
                                    when (active) {
                                        "move" -> if (frameW - w > 0f) padX = ((l + dx - frameX) / (frameW - w)).coerceIn(0f, 1f)
                                        "left" -> {
                                            val nw = (w - dx).coerceIn(160f, r - frameX)
                                            padW = nw / frameW
                                            padX = if (frameW - nw > 0f) (r - nw - frameX) / (frameW - nw) else 0.5f
                                        }
                                        "right" -> {
                                            val nw = (w + dx).coerceIn(160f, frameX + frameW - l)
                                            padW = nw / frameW
                                            padX = if (frameW - nw > 0f) (l - frameX) / (frameW - nw) else 0.5f
                                        }
                                    }
                                }
                            }
                            .pointerInput(landscape) {
                                detectTapGestures(onDoubleTap = {
                                    padX = 0.5f; padW = 0.42f
                                    Settings.setDeckPadPos(landscape, 0.5f); Settings.setDeckPadWidth(landscape, 0.42f)
                                })
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun GripPill(widthDp: Int, active: Boolean, color: androidx.compose.ui.graphics.Color, activeColor: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.padding(horizontal = 12.dp).size(width = widthDp.dp, height = 4.dp)
            .background(if (active) activeColor else color, androidx.compose.foundation.shape.CircleShape)
    )
}

/** A compact trackpad for space the keyboard doesn't use: same gestures, no hint text. */
@Composable
private fun FreeSpacePad(hid: HidUi, modifier: Modifier) {
    AndroidView(
        factory = { TrackpadView(it).apply { compact = true } },
        update = { v -> hid.version; v.invalidate() },
        modifier = modifier,
    )
}

/** Layout, size and typing options for the keyboard in the current orientation. */
@Composable
private fun KeyboardLayoutSheet(onChanged: () -> Unit, onDismiss: () -> Unit) {
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    var layout by remember { mutableStateOf(Settings.kbLayout(landscape)) }
    var width by remember { mutableStateOf(Settings.kbWidth(landscape)) }
    var align by remember { mutableStateOf(Settings.kbAlign(landscape)) }
    var gap by remember { mutableStateOf(Settings.kbSplitGap) }
    var rows by remember { mutableStateOf(Settings.kbShowRows(landscape)) }
    var preview by remember { mutableStateOf(Settings.kbPreview) }
    var flick by remember { mutableStateOf(Settings.kbFlick) }
    var spaceCursor by remember { mutableStateOf(Settings.kbSpaceCursor) }
    var typing by remember { mutableStateOf(Settings.typingMode) }
    var learning by remember { mutableStateOf(Settings.imeLearning) }
    var trackPoint by remember { mutableStateOf(Settings.kbTrackPoint) }
    var nubSpeed by remember { mutableStateOf(Settings.trackPointSpeed) }
    var predict by remember { mutableStateOf(Settings.kbPredict) }
    var predictLearn by remember { mutableStateOf(Settings.kbPredictLearn) }
    var freePadOn by remember { mutableStateOf(Settings.kbFreePad) }
    val tabletLand = isTablet() && landscape
    var laptop by remember { mutableStateOf(Settings.kbLaptop(landscape, tabletLand)) }
    var deckKbW by remember { mutableStateOf(Settings.deckKbWidth(landscape)) }
    var deckKeys by remember { mutableStateOf(Settings.deckKeys(landscape)) }
    var padFull by remember { mutableStateOf(Settings.deckPadFull(landscape)) }
    var deckLocked by remember { mutableStateOf(Settings.deckLocked) }
    var stylus by remember { mutableStateOf(Settings.stylus) }
    var palm by remember { mutableStateOf(Settings.palmRejection) }
    val icons = mapOf(
        dev.padbridge.KbLayout.LAPTOP to Icons.Rounded.Laptop,
        dev.padbridge.KbLayout.FULL to Icons.Rounded.Keyboard,
        dev.padbridge.KbLayout.THINKPAD to Icons.Rounded.Mouse,
        dev.padbridge.KbLayout.FULL_NUMPAD to Icons.Rounded.Calculate,
        dev.padbridge.KbLayout.COMPACT_65 to Icons.Rounded.KeyboardAlt,
        dev.padbridge.KbLayout.SPLIT to Icons.Rounded.VerticalSplit,
        dev.padbridge.KbLayout.GAMING to Icons.Rounded.SportsEsports,
        dev.padbridge.KbLayout.NUMPAD to Icons.Rounded.Dialpad,
        dev.padbridge.KbLayout.PHONE to Icons.Rounded.Smartphone,
    )

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = glassSheetColor(), scrimColor = glassScrim(),
    ) {
        GlassBehind()
        androidx.compose.foundation.lazy.LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(androidx.compose.material3.ListItemDefaults.SegmentedGap),
        ) {
            item {
                Text("Keyboard · ${if (landscape) "landscape" else "portrait"}",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Layout and size are remembered separately for each orientation.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp))
            }
            item {
                // Keys, labels and the shortcut row follow the computer type; switching here changes it app-wide.
                Text("Keyboard for", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp))
                OsSelector(OsBus.version) { OsBus.version++; onChanged() }
                Spacer(Modifier.height(16.dp))
            }
            item {
                SheetSwitch("Laptop deck: keyboard with a trackpad below", laptop, 0, 1) {
                    laptop = it; Settings.setKbLaptop(landscape, it); onChanged()
                }
                Spacer(Modifier.height(12.dp))
            }
            val all = dev.padbridge.KbLayout.entries
            items(all.size) { i ->
                val l = all[i]
                val selected = l == layout
                androidx.compose.material3.SegmentedListItem(
                    onClick = { layout = l; Settings.setKbLayout(landscape, l); onChanged() },
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(i, all.size),
                    colors = if (selected) androidx.compose.material3.ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        supportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        leadingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) else segColors(),
                    leadingContent = { Icon(icons.getValue(l), null) },
                    supportingContent = { Text(l.blurb) },
                    trailingContent = if (selected) ({ Icon(Icons.Rounded.Check, null) }) else null,
                ) { Text(l.label) }
            }

            if (laptop) item {
                Column(Modifier.padding(top = 16.dp)) {
                    Text("Laptop deck", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 4.dp))
                    Text(
                        "Keyboard width · " + if (deckKbW <= 0f) "auto" else "${(deckKbW * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    androidx.compose.material3.Slider(
                        value = if (deckKbW <= 0f) 1f else deckKbW, valueRange = 0.4f..1f, steps = 11,
                        onValueChange = { deckKbW = it; Settings.setDeckKbWidth(landscape, it); onChanged() },
                    )
                    Text(
                        "Keyboard height · " + if (deckKeys <= 0f) "auto" else "${(deckKeys * 100).toInt()}% of the space",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    androidx.compose.material3.Slider(
                        value = if (deckKeys <= 0f) 0.62f else deckKeys, valueRange = 0.3f..0.85f, steps = 10,
                        onValueChange = { deckKeys = it; Settings.setDeckKeys(landscape, it); onChanged() },
                    )
                    Text(
                        "Or on screen: drag the middle of the bar under the keys to change their height, and the small " +
                            "grips under the keyboard's corners to change its width. Under the trackpad, the middle grip " +
                            "moves it and the end grips resize it. Double-tap a grip to reset.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    SheetSwitch("Full-width trackpad", padFull, 0, 4) {
                        padFull = it; Settings.setDeckPadFull(landscape, it); onChanged()
                    }
                    SheetSwitch("Lock layout (hide the handles)", deckLocked, 1, 4) {
                        deckLocked = it; Settings.deckLocked = it; onChanged()
                    }
                    SheetSwitch("Palm rejection: ignore the trackpad while typing", palm, 2, 4) {
                        palm = it; Settings.palmRejection = it; onChanged()
                    }
                    SheetSwitch("S Pen: hover moves, touch clicks, palm rejection", stylus, 3, 4) {
                        stylus = it; Settings.stylus = it; onChanged()
                    }
                    androidx.compose.material3.TextButton(onClick = {
                        Settings.resetDeck(landscape); deckKbW = 0f; deckKeys = 0f; padFull = false; onChanged()
                    }) { Text("Reset deck") }
                }
            }
            if (!laptop) item { Text("Size", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)) }
            if (!laptop) item {
                Column {
                    Text("Width · ${(width * 100).toInt()}%", style = MaterialTheme.typography.bodyLarge)
                    androidx.compose.material3.Slider(
                        value = width, valueRange = 0.4f..1f, steps = 11,
                        onValueChange = { width = it; Settings.setKbWidth(landscape, it); onChanged() },
                    )
                    Text("Height: drag the bar above the keys; the space above becomes a trackpad. Double-tap the bar to fill the space again. " +
                        "A narrower keyboard aligned left or right gets a trackpad beside it.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!laptop) item {
                Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    listOf("Left", "Center", "Right").forEachIndexed { i, label ->
                        ToggleButton(
                            checked = align == i,
                            onCheckedChange = { align = i; Settings.setKbAlign(landscape, i); onChanged() },
                            enabled = width < 0.99f,
                            modifier = Modifier.weight(1f),
                            shapes = when (i) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            },
                        ) { Text(label) }
                    }
                }
            }
            if (layout == dev.padbridge.KbLayout.SPLIT) item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("Split gap · ${"%.1f".format(gap)} keys", style = MaterialTheme.typography.bodyLarge)
                    androidx.compose.material3.Slider(
                        value = gap, valueRange = 0f..8f, steps = 15,
                        onValueChange = { gap = it; Settings.kbSplitGap = it; onChanged() },
                    )
                }
            }
            if (!laptop) item {
                androidx.compose.material3.TextButton(onClick = {
                    Settings.setKbHeight(landscape, 0f); Settings.setKbWidth(landscape, 1f); Settings.setKbAlign(landscape, 1)
                    width = 1f; align = 1; onChanged()
                }) { Text("Reset size") }
            }

            item { Text("Options", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)) }
            item {
                SheetSwitch("Trackpad in free space", freePadOn, 0, 10) { freePadOn = it; Settings.kbFreePad = it; onChanged() }
            }
            item {
                SheetSwitch("Shortcut and media rows", rows, 1, 10) { rows = it; Settings.setKbShowRows(landscape, it); onChanged() }
            }
            item {
                SheetSwitch("Key preview bubbles", preview, 2, 10) { preview = it; Settings.kbPreview = it; onChanged() }
            }
            item {
                SheetSwitch("Flick up on a key for its shifted character (! for 1, A for a)", flick, 3, 10) {
                    flick = it; Settings.kbFlick = it; onChanged()
                }
            }
            item {
                SheetSwitch("Drag the space bar to move the cursor", spaceCursor, 4, 10) {
                    spaceCursor = it; Settings.kbSpaceCursor = it; onChanged()
                }
            }
            item {
                androidx.compose.material3.SegmentedListItem(
                    onClick = {},
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(5, 10),
                    colors = segColors(),
                    supportingContent = {
                        Column {
                            Text(when (typing) {
                                0 -> "Direct: every key goes to the computer as you type. No suggestions."
                                1 -> "Direct with suggestions: glide and autocorrect; corrections are fixed with backspaces."
                                else -> "Indirect: write with suggestions, glide and voice typing, edit it, then Enter types it " +
                                    "all and presses Enter on the computer."
                            })
                            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                listOf("Direct", "Suggest", "Indirect").forEachIndexed { i, label ->
                                    ToggleButton(
                                        checked = typing == i,
                                        onCheckedChange = { typing = i; Settings.typingMode = i; onChanged() },
                                        modifier = Modifier.weight(1f),
                                        shapes = when (i) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            2 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        },
                                    ) { Text(label) }
                                }
                            }
                        }
                    },
                ) { Text("Phone keyboard typing") }
            }
            item {
                SheetSwitch("Phone keyboard learns from what you type (off: incognito)", learning, 6, 10) {
                    learning = it; Settings.imeLearning = it; onChanged()
                }
            }
            item {
                androidx.compose.material3.SegmentedListItem(
                    onClick = { trackPoint = !trackPoint; Settings.kbTrackPoint = trackPoint; onChanged() },
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(7, 10),
                    colors = segColors(),
                    trailingContent = {
                        androidx.compose.material3.Switch(checked = trackPoint, onCheckedChange = {
                            trackPoint = it; Settings.kbTrackPoint = it; onChanged()
                        })
                    },
                    supportingContent = {
                        Column {
                            Text("Sensitivity · ${"%.1f".format(nubSpeed)}×  (ThinkPad layout too)")
                            androidx.compose.material3.Slider(
                                value = nubSpeed, valueRange = 0.3f..3f,
                                onValueChange = { nubSpeed = (it * 10).roundToInt() / 10f; Settings.trackPointSpeed = nubSpeed },
                            )
                        }
                    },
                ) { Text("TrackPoint on Laptop and Full layouts (tap to click, hold Fn to scroll)") }
            }
            item {
                SheetSwitch("Word suggestions above the keys (from your phone's spell checker)", predict, 8, 10) {
                    predict = it; Settings.kbPredict = it; if (!it) dev.padbridge.Predict.reset(); onChanged()
                }
            }
            item {
                androidx.compose.material3.SegmentedListItem(
                    onClick = { predictLearn = !predictLearn; Settings.kbPredictLearn = predictLearn },
                    shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(9, 10),
                    colors = segColors(),
                    trailingContent = {
                        androidx.compose.material3.Switch(checked = predictLearn, onCheckedChange = {
                            predictLearn = it; Settings.kbPredictLearn = it
                        })
                    },
                    supportingContent = {
                        Column {
                            Text("Kept only on this phone. Anything typed on this keyboard counts, passwords too.")
                            androidx.compose.material3.TextButton(
                                onClick = { dev.padbridge.Predict.forget() },
                                contentPadding = PaddingValues(0.dp),
                            ) { Text("Forget learned words") }
                        }
                    },
                ) { Text("Suggestions learn the words you type") }
            }
        }
    }
}

/** Word suggestions for the built-in keyboard: up to three, tap one to replace the word being typed. */
@Composable
private fun SuggestionStrip() {
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { dev.padbridge.Predict.init(ctx) }
    val list = dev.padbridge.Predict.suggestions
    val word = dev.padbridge.Predict.word
    Row(
        Modifier.fillMaxWidth().height(40.dp).padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (list.isEmpty()) {
            Text(
                if (word.isEmpty()) "Suggestions appear as you type" else word,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
        list.forEachIndexed { i, s ->
            Surface(
                onClick = { dev.padbridge.Predict.pick(s) },
                shape = RoundedCornerShape(12.dp),
                color = if (i == 0) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 3.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(s, style = MaterialTheme.typography.bodyLarge, maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SheetSwitch(title: String, value: Boolean, index: Int, count: Int, onChange: (Boolean) -> Unit) {
    androidx.compose.material3.SegmentedListItem(
        onClick = { onChange(!value) },
        shapes = androidx.compose.material3.ListItemDefaults.segmentedShapes(index, count),
        colors = segColors(),
        trailingContent = { androidx.compose.material3.Switch(checked = value, onCheckedChange = onChange) },
    ) { Text(title) }
}

/**
 * Keyboard on the whole display: no bars or rows, keys edge to edge (clear of the camera cutout).
 * A small handle at the top opens Exit and the layout sheet.
 */
@Composable
fun FullDisplayKeyboard(hid: HidUi, osVersion: Int, onExit: () -> Unit) {
    val os = remember(osVersion) { Settings.hostOs }
    var cfg by remember { mutableStateOf(0) }
    var sheet by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    LaunchedEffect(controls, sheet) { if (controls && !sheet) { kotlinx.coroutines.delay(4000); controls = false } }

    Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
        Box(
            Modifier.fillMaxWidth().height(22.dp).clickable(onClickLabel = "Show controls") { controls = true },
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(width = 40.dp, height = 4.dp)
                .background(MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape))
        }
        // Same layout as the Keyboard tab (saved height, trackpad in the free space, laptop deck), just edge to edge.
        KeyboardArea(hid, os, cfg, resizable = true,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(start = 6.dp, end = 6.dp, bottom = 6.dp))
    }
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(controls, modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
            enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            HorizontalFloatingToolbar(expanded = true, colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors()) {
                androidx.compose.material3.IconButton(onClick = onExit) { Icon(Icons.Rounded.CloseFullscreen, "Exit full display") }
                androidx.compose.material3.IconButton(onClick = { sheet = true }) { Icon(Icons.Rounded.Dashboard, "Keyboard layout") }
                androidx.compose.material3.IconButton(onClick = { controls = false }) { Icon(Icons.Rounded.ExpandLess, "Hide controls") }
            }
        }
    }
    if (sheet) KeyboardLayoutSheet(onChanged = { cfg++ }) { sheet = false; controls = false }
}

@Composable
private fun MediaRow(modifier: Modifier = Modifier) {
    val items: List<Pair<ImageVector, Int>> = listOf(
        Icons.Rounded.SkipPrevious to Keys.C_PREV,
        Icons.Rounded.PlayArrow to Keys.C_PLAY_PAUSE,
        Icons.Rounded.SkipNext to Keys.C_NEXT,
        Icons.AutoMirrored.Rounded.VolumeDown to Keys.C_VOL_DOWN,
        Icons.AutoMirrored.Rounded.VolumeUp to Keys.C_VOL_UP,
        Icons.AutoMirrored.Rounded.VolumeOff to Keys.C_MUTE,
        Icons.Rounded.BrightnessLow to Keys.C_BRIGHT_DOWN,
        Icons.Rounded.BrightnessHigh to Keys.C_BRIGHT_UP,
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        items(items) { (icon, usage) ->
            if (usage == Keys.C_PLAY_PAUSE) {
                FilledIconButton(
                    onClick = { dev.padbridge.Haptics.tick(); HidManager.consumer(usage) },
                    shapes = IconButtonDefaults.shapes(),
                ) { Icon(icon, null) }
            } else {
                FilledTonalIconButton(
                    onClick = { dev.padbridge.Haptics.tick(); HidManager.consumer(usage) },
                    shapes = IconButtonDefaults.shapes(),
                ) { Icon(icon, null) }
            }
        }
    }
}

// ====================================================================== Gamepad

@Composable
fun GamepadScreen(onExit: () -> Unit) {
    var ps by remember { mutableStateOf(Settings.gamepadStyle == "ps") }
    var haptics by remember { mutableStateOf(Settings.haptics) }
    var gyro by remember { mutableStateOf(Settings.gamepadGyro) }
    var preset by remember { mutableStateOf(Settings.gamepadPreset) }
    var presetMenu by remember { mutableStateOf(false) }
    val ctx = LocalContext.current
    val hasGyro = remember { dev.padbridge.Motion.hasGyro(ctx.applicationContext) }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { GamepadView(it) },
            update = {
                it.style = if (ps) GamepadView.Style.PLAYSTATION else GamepadView.Style.XBOX
                it.preset = preset
                it.gyroMode = gyro
            },
            onRelease = { it.releaseAll() },
            modifier = Modifier.fillMaxSize(),
        )
        HorizontalFloatingToolbar(
            expanded = true,
            colors = FloatingToolbarDefaults.standardFloatingToolbarColors(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
        ) {
            FilledTonalIconButton(onClick = onExit) { Icon(Icons.Rounded.Close, "Exit gamepad") }
            Spacer(Modifier.width(8.dp))
            // Preset (layout for the kind of game) + button labels, in one menu to keep the toolbar short.
            Box {
                androidx.compose.material3.FilledTonalButton(onClick = { presetMenu = true }) {
                    Icon(Icons.Rounded.SportsEsports, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(preset.label, fontWeight = FontWeight.Medium)
                }
                androidx.compose.material3.DropdownMenu(
                    expanded = presetMenu, onDismissRequest = { presetMenu = false }, shape = RoundedCornerShape(16.dp),
                    containerColor = glassMenuColor(),
                ) {
                    dev.padbridge.GamePreset.entries.forEach { p ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = {
                                Column {
                                    Text(p.label, fontWeight = FontWeight.Medium)
                                    Text(p.blurb, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            trailingIcon = if (p == preset) ({ Icon(Icons.Rounded.Check, null) }) else null,
                            onClick = {
                                presetMenu = false
                                preset = p; Settings.gamepadPreset = p
                                // Each preset has a sensible gyro default (racing steers, first person aims).
                                if (hasGyro) { gyro = p.defaultGyro; Settings.gamepadGyro = gyro }
                            },
                        )
                    }
                    androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    listOf("Xbox labels", "PlayStation labels").forEachIndexed { i, label ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(label) },
                            trailingIcon = if (ps == (i == 1)) ({ Icon(Icons.Rounded.Check, null) }) else null,
                            onClick = { presetMenu = false; ps = i == 1; Settings.gamepadStyle = if (ps) "ps" else "xbox" },
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            if (hasGyro) {
                // Off -> aim (right stick follows the phone's turn) -> steer (tilt drives the left stick).
                ToggleButton(
                    checked = gyro != 0,
                    onCheckedChange = { gyro = (gyro + 1) % 3; Settings.gamepadGyro = gyro },
                ) {
                    Text(
                        when (gyro) { 1 -> "Gyro aim"; 2 -> "Gyro steer"; else -> "Gyro off" },
                        fontWeight = FontWeight.Medium,
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            FilledTonalIconToggleButton(checked = haptics, onCheckedChange = { haptics = it; Settings.haptics = it }) {
                Icon(Icons.Rounded.Vibration, "Haptics")
            }
        }
    }
}

// ====================================================================== Full display trackpad

/**
 * The trackpad on every pixel of the display: no margins, bars or toolbar, under the camera cutout.
 * A floating toolbar shows on entry, then folds into a small handle at the top; tap it for controls.
 */
@Composable
fun FullDisplayTrackpad(hid: HidUi, osVersion: Int, aod: Boolean, onAod: () -> Unit, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val os = remember(osVersion) { Settings.hostOs }
    var controls by remember { mutableStateOf(true) }
    var kbOpen by remember { mutableStateOf(false) }
    var poke by remember { mutableStateOf(0) }
    val ime = remember { ImeCaptureView(ctx) }

    // Controls fold away after a few seconds unless the keyboard is open.
    LaunchedEffect(controls, poke, kbOpen) {
        if (controls && !kbOpen) { kotlinx.coroutines.delay(4000); controls = false }
    }
    LaunchedEffect(kbOpen) { if (kbOpen) ime.post { ime.showKeyboard() } else ime.hideKeyboard() }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { TrackpadView(it).apply { fullBleed = true } },
            update = { v -> hid.version; osVersion; aod; v.invalidate() },
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(top = 6.dp),
        ) {
            AnimatedVisibility(!controls, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier.size(width = 72.dp, height = 28.dp)
                        .clickable(onClickLabel = "Show controls") { controls = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(width = 40.dp, height = 5.dp)
                            .background(MaterialTheme.colorScheme.outline, androidx.compose.foundation.shape.CircleShape)
                    )
                }
            }
            AnimatedVisibility(controls, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                HorizontalFloatingToolbar(
                    expanded = true,
                    // Vibrant so it stands out from the pad, which fills the screen in the same surface color.
                    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
                    modifier = Modifier.aodOutline(aod, FloatingToolbarDefaults.ContainerShape),
                ) {
                    androidx.compose.material3.IconButton(onClick = onExit) {
                        Icon(Icons.Rounded.CloseFullscreen, "Exit full display")
                    }
                    FilledTonalIconToggleButton(checked = kbOpen, onCheckedChange = { kbOpen = it; poke++ }) {
                        Icon(if (kbOpen) Icons.Rounded.KeyboardHide else Icons.Rounded.Keyboard, "Keyboard")
                    }
                    AodButton(aod) { onAod(); poke++ }
                    androidx.compose.material3.IconButton(onClick = { controls = false }) {
                        Icon(Icons.Rounded.ExpandLess, "Hide controls")
                    }
                }
            }
            AnimatedVisibility(kbOpen) {
                Column(Modifier.padding(top = 8.dp).width(360.dp)) {
                    AndroidView({ ime }, Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 12.dp))
                    ChipRow(quickKeys(os), Modifier.padding(top = 6.dp))
                }
            }
        }
    }
}

// ====================================================================== Air mouse

@Composable
fun AirMouseScreen(
    fullscreen: Boolean = false, onToggleFullscreen: () -> Unit = {}, onAod: () -> Unit = {}, aod: Boolean = false,
    onBack: () -> Unit = {},
) {
    var always by remember { mutableStateOf(Settings.airAlwaysAim) }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            // Air mouse lives inside the trackpad: one tap back to it.
            if (!aod) androidx.compose.material3.IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back to trackpad")
            }
            TitleChip("Air mouse", aod, null)
            Spacer(Modifier.weight(1f))
            AodButton(aod, onAod)
            if (!aod) OverflowMenu(listOf(
                MenuEntry(if (fullscreen) "Show app bars" else "Hide app bars",
                    if (fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen, action = onToggleFullscreen),
                MenuEntry("Air mouse settings", Icons.Rounded.Tune) { SettingsLink.open(SettingsPage.MOTION) },
            ))
            Spacer(Modifier.width(4.dp))
            ToggleButton(
                checked = always,
                onCheckedChange = { always = it; Settings.airAlwaysAim = it },
                colors = if (aod) androidx.compose.material3.ToggleButtonDefaults.colors(
                    containerColor = androidx.compose.ui.graphics.Color.Black,
                    checkedContainerColor = androidx.compose.ui.graphics.Color.Black,
                    checkedContentColor = MaterialTheme.colorScheme.primary,
                ) else androidx.compose.material3.ToggleButtonDefaults.colors(),
                border = if (aod) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
            ) {
                Icon(Icons.Rounded.GpsFixed, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Always aim")
            }
        }
        AndroidView(
            factory = { AirMouseView(it) },
            update = { it.alwaysAim = always; aod; it.invalidate() },
            onRelease = { it.releaseAll() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        )
    }
}


// ====================================================================== Remote (D-pad)

/**
 * TV-remote style D-pad for Android TV, Apple TV, a Mac, a PC or a slideshow. The profile decides what
 * Back, Home and the extra buttons send; arrows repeat while held. Volume buttons can drive it too.
 */
@Composable
fun RemoteScreen(hid: HidUi, osVersion: Int, aod: Boolean = false, onAod: () -> Unit = {}, onGoConnect: () -> Unit = {}) {
    val ctx = LocalContext.current
    val activity = ctx as? dev.padbridge.MainActivity
    val os = remember(osVersion) { Settings.hostOs }
    var profile by remember { mutableStateOf(Settings.remoteProfile) }
    var volKeys by remember { mutableStateOf(Settings.remoteVolumeKeys) }
    var kbOpen by remember { mutableStateOf(false) }
    var incognito by remember { mutableStateOf(!Settings.imeLearning) }
    val ime = remember { ImeCaptureView(ctx) }
    LaunchedEffect(kbOpen) { if (kbOpen) ime.post { ime.showKeyboard() } else ime.hideKeyboard() }

    // Phones hold the remote like a remote: portrait only. Tablets keep rotating.
    val phone = !isTablet()
    androidx.compose.runtime.DisposableEffect(phone) {
        if (phone) activity?.setTrackpadOrientation(1)
        onDispose { if (phone) activity?.setTrackpadOrientation(0) }
    }

    // Phone volume buttons: Slides = previous / next slide, otherwise the host's volume.
    androidx.compose.runtime.DisposableEffect(profile, volKeys) {
        activity?.volumeKeyHandler = if (!volKeys) null else { up, pressed ->
            if (pressed) {
                if (profile == dev.padbridge.RemoteProfile.SLIDES)
                    dev.padbridge.RemoteSender.tap(dev.padbridge.RemoteOut.Key(if (up) Keys.RIGHT else Keys.LEFT))
                else HidManager.consumer(if (up) Keys.C_VOL_UP else Keys.C_VOL_DOWN)
                dev.padbridge.Haptics.tick()
            }
            true
        }
        onDispose { activity?.volumeKeyHandler = null }
    }

    val (prev, next) = profile.prevNext
    val back = profile.back
    val home = profile.home(os)

    // "Short" screens (phones in landscape) get the side-by-side layout; tablets have room for the tall one.
    val landscapeTop = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp < 520
    // Tablets: bigger keys and clickpad; sideways, the clickpad and the buttons sit side by side like a remote's
    // body laid flat, instead of a phone-width column in the middle of the screen.
    val big = isTablet()
    val sideBySide = landscapeTop || (big && androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE)
    // Profile picker. In landscape it shares the toolbar row, leaving the height to the clickpad.
    fun profileIcon(p: dev.padbridge.RemoteProfile) = when (p) {
        dev.padbridge.RemoteProfile.ANDROID_TV, dev.padbridge.RemoteProfile.APPLE_TV -> Icons.Rounded.Tv
        dev.padbridge.RemoteProfile.MAC -> Icons.Rounded.LaptopMac
        dev.padbridge.RemoteProfile.PC -> Icons.Rounded.DesktopWindows
        dev.padbridge.RemoteProfile.SLIDES -> Icons.Rounded.Slideshow
    }
    // One chip that opens a menu, instead of a row of every profile.
    val profilePicker: @Composable () -> Unit = {
        var open by remember { mutableStateOf(false) }
        Box {
            androidx.compose.material3.FilterChip(
                selected = true,
                onClick = { open = true },
                label = { Text(profile.label) },
                leadingIcon = { Icon(profileIcon(profile), null, Modifier.size(18.dp)) },
                trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, "Change remote", Modifier.size(18.dp)) },
            )
            androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false },
                shape = RoundedCornerShape(16.dp), containerColor = glassMenuColor()) {
                dev.padbridge.RemoteProfile.entries.forEach { p ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(p.label) },
                        leadingIcon = { Icon(profileIcon(p), null) },
                        trailingIcon = if (p == profile) ({ Icon(Icons.Rounded.Check, null) }) else null,
                        onClick = { open = false; profile = p; Settings.remoteProfile = p },
                    )
                }
            }
        }
    }
    val tv = profile == dev.padbridge.RemoteProfile.ANDROID_TV
    var swipe by remember { mutableStateOf(Settings.remoteSwipe) }
    val extras = (if (profile == dev.padbridge.RemoteProfile.SLIDES) emptyList() else listOf(prev, next)) + profile.extras(os)
    // On tablets the remote stays a remote-sized column in the middle instead of spreading edge to edge.
    val remoteWidth = when {
        landscapeTop -> Modifier.fillMaxWidth()
        sideBySide -> Modifier.wrapContentWidth().widthIn(max = 1100.dp).fillMaxWidth()
        big -> Modifier.wrapContentWidth().widthIn(max = 720.dp).fillMaxWidth()
        else -> Modifier.wrapContentWidth().widthIn(max = 600.dp).fillMaxWidth()
    }
    Column(Modifier.fillMaxSize().then(remoteWidth).padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            if ((hid.connected && !LocalTopBarStatus.current) || aod) ConnectionChip(hid, aod)
            Box(Modifier.padding(horizontal = 8.dp)) { profilePicker() }
            Spacer(Modifier.weight(1f))
            if (tv) {
                // Android TV keeps its extras in a menu so the remote itself stays clean.
                androidx.compose.material3.IconToggleButton(checked = swipe, onCheckedChange = {
                    swipe = it; Settings.remoteSwipe = it
                }) { Icon(if (swipe) Icons.Rounded.Swipe else Icons.Rounded.Gamepad, if (swipe) "Swipe pad (tap for D-pad)" else "D-pad (tap for swipe pad)") }
                OverflowMenu(extras.map { b ->
                    MenuEntry(b.label, when (b.label) {
                        "Previous" -> Icons.Rounded.SkipPrevious; "Next" -> Icons.Rounded.SkipNext
                        "Rewind" -> Icons.Rounded.FastRewind; "Fast fwd" -> Icons.Rounded.FastForward
                        "Menu" -> Icons.Rounded.Menu; else -> Icons.Rounded.Tv
                    }) { dev.padbridge.RemoteSender.tap(b.out); dev.padbridge.Haptics.tick() }
                } + MenuEntry("Phone volume keys control the TV", Icons.Rounded.Vibration, checked = volKeys, divider = true) {
                    volKeys = !volKeys; Settings.remoteVolumeKeys = volKeys
                })
            }
            AodButton(aod, onAod)
            FilledTonalIconToggleButton(
                checked = kbOpen, onCheckedChange = { kbOpen = it },
                modifier = Modifier.aodOutline(aod, IconButtonDefaults.filledShape),
            ) { Icon(if (kbOpen) Icons.Rounded.KeyboardHide else Icons.Rounded.Keyboard, "Type text") }
        }

        NotConnectedBanner(hid, onGoConnect, Modifier.padding(bottom = 8.dp))

        AnimatedVisibility(kbOpen) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                AndroidView({ ime }, Modifier.weight(1f).height(48.dp))
                // Incognito on by default (what you type on the TV doesn't train the phone keyboard). Off: the
                // keyboard learns and suggests, so a direct-typing mode moves to "Suggest" (still typed live).
                FilledTonalIconToggleButton(
                    checked = incognito,
                    onCheckedChange = {
                        incognito = it
                        Settings.imeLearning = !it
                        if (!it && Settings.typingMode == 0) Settings.typingMode = 1
                        ime.refreshMode()
                    },
                    modifier = Modifier.padding(start = 8.dp),
                ) {
                    Icon(
                        if (incognito) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        if (incognito) "Incognito on: the phone keyboard doesn't learn. Tap to let it learn"
                        else "Incognito off: the phone keyboard learns. Tap for incognito",
                    )
                }
                VoiceTypeButton()
            }
        }

        if (profile == dev.padbridge.RemoteProfile.ANDROID_TV) {
            GoogleTvRemote(aod, sideBySide, swipe, back, home, Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), big)
        } else {
        // Siri Remote-style: a large clickpad, then a quiet two-column grid of identical round keys
        // (Back | Home, Play | volume rocker, Mute). No panel, no colored keys: the controls float on the page.
        // Landscape puts the keys beside the clickpad instead of under it.
        val slides = profile == dev.padbridge.RemoteProfile.SLIDES
        val landscapeNow = sideBySide
        val key = if (landscapeTop) 52.dp else if (big) 76.dp else 64.dp
        val gap = if (landscapeTop) 12.dp else if (big) 22.dp else 16.dp
        val clickpad: @Composable (Modifier) -> Unit = { m ->
            androidx.compose.foundation.layout.BoxWithConstraints(m, contentAlignment = Alignment.Center) {
                // Square, as large as fits (capped for tablets).
                val side = minOf(maxWidth - 16.dp, maxHeight, if (big) 560.dp else 400.dp)
                AndroidView(
                    factory = { DpadView(it) },
                    update = { v -> aod; v.invalidate() },
                    modifier = Modifier.size(side),
                )
            }
        }
        val playKey = dev.padbridge.RemoteButton("Play / pause", dev.padbridge.RemoteOut.Consumer(Keys.C_PLAY_PAUSE))
        val muteKey = dev.padbridge.RemoteButton("Mute", dev.padbridge.RemoteOut.Consumer(Keys.C_MUTE))
        // Landscape has little height: two short rows, with the rocker lying sideways.
        val wideKeys: @Composable () -> Unit = {
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    RemoteKey(back, Icons.Rounded.ChevronLeft, aod, key)
                    RemoteKey(home, if (slides) Icons.Rounded.Slideshow else Icons.Rounded.Tv, aod, key)
                    RemoteKey(playKey, Icons.Rounded.PlayArrow, aod, key, secondIcon = Icons.Rounded.Pause)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    RemoteKey(muteKey, Icons.AutoMirrored.Rounded.VolumeOff, aod, key)
                    VolumeRocker(aod, key * 2 + gap, key, horizontal = true)
                }
            }
        }
        val keys: @Composable () -> Unit = {
            Row(horizontalArrangement = Arrangement.spacedBy(56.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    RemoteKey(back, Icons.Rounded.ChevronLeft, aod, key)
                    RemoteKey(
                        dev.padbridge.RemoteButton("Play / pause", dev.padbridge.RemoteOut.Consumer(Keys.C_PLAY_PAUSE)),
                        Icons.Rounded.PlayArrow, aod, key, secondIcon = Icons.Rounded.Pause,
                    )
                    RemoteKey(
                        dev.padbridge.RemoteButton("Mute", dev.padbridge.RemoteOut.Consumer(Keys.C_MUTE)),
                        Icons.AutoMirrored.Rounded.VolumeOff, aod, key,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    RemoteKey(home, if (slides) Icons.Rounded.Slideshow else Icons.Rounded.Tv, aod, key)
                    VolumeRocker(aod, key, key * 2 + gap)
                }
            }
        }
        if (landscapeNow) {
            Row(
                Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                clickpad(Modifier.weight(1f).fillMaxHeight())
                // Phones sideways: two short rows; a tablet has the height for the tall two-column grid.
                if (landscapeTop) wideKeys() else keys()
                Spacer(Modifier.width(24.dp))
            }
        } else {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                clickpad(Modifier.weight(1f).fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                keys()
            }
        }

        }

        // Profile extras. Previous / next live here (slides already step with the clickpad's left / right).
        if (!tv) LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            items(extras) { b ->
                Surface(
                    onClick = { dev.padbridge.RemoteSender.tap(b.out); dev.padbridge.Haptics.tick() },
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.aodOutline(aod, androidx.compose.foundation.shape.CircleShape),
                ) {
                    Text(b.label, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp))
                }
            }
            item {
                androidx.compose.material3.FilterChip(
                    selected = volKeys,
                    onClick = { volKeys = !volKeys; Settings.remoteVolumeKeys = volKeys },
                    label = { Text(if (profile == dev.padbridge.RemoteProfile.SLIDES) "Volume keys = slides" else "Volume keys") },
                    leadingIcon = { Icon(Icons.Rounded.Vibration, null, Modifier.size(18.dp)) },
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
            }
        }
    }
}

/** Press handling shared by remote keys: held while the finger is down (keys repeat), with a soft press state. */
private fun Modifier.remotePress(b: dev.padbridge.RemoteButton, onPressed: (Boolean) -> Unit): Modifier =
    semantics { contentDescription = b.label; role = androidx.compose.ui.semantics.Role.Button }
        .pointerInput(b) {
            detectTapGestures(onPress = {
                onPressed(true)
                dev.padbridge.Haptics.click()
                dev.padbridge.RemoteSender.down(b.out)
                tryAwaitRelease()
                dev.padbridge.RemoteSender.up(b.out)
                onPressed(false)
            })
        }

@Composable
private fun pressColor(pressed: Boolean): androidx.compose.ui.graphics.Color {
    val cs = MaterialTheme.colorScheme
    // Realistic light: white buttons like a silver remote's; dark keeps the graphite containers.
    val real = dev.padbridge.Real.active && !dev.padbridge.Real.dark
    return androidx.compose.animation.animateColorAsState(
        when {
            real -> if (pressed) cs.surfaceContainerHighest else cs.surfaceContainer
            pressed -> cs.surfaceContainerHighest
            else -> cs.surfaceContainerHigh
        }, label = "keyColor",
    ).value
}

/** A round, icon-only remote key, all the same neutral tone like the Siri Remote's buttons. */
@Composable
private fun RemoteKey(
    b: dev.padbridge.RemoteButton, icon: ImageVector, aod: Boolean, size: androidx.compose.ui.unit.Dp,
    secondIcon: ImageVector? = null,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.9f else 1f, label = "keyScale")
    val shape = androidx.compose.foundation.shape.CircleShape
    Surface(
        shape = shape,
        color = pressColor(pressed),
        contentColor = MaterialTheme.colorScheme.onSurface,
        // Realistic: raised buttons with a soft shadow (flatter while pressed).
        shadowElevation = if (dev.padbridge.Real.active) (if (pressed) 1.dp else 3.dp) else 0.dp,
        modifier = Modifier.size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aodOutline(aod, shape)
            .remotePress(b) { pressed = it },
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (secondIcon == null) Icon(icon, null, Modifier.size(26.dp))
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(20.dp))
                Icon(secondIcon, null, Modifier.size(18.dp))
            }
        }
    }
}

/**
 * Google TV app-style remote for Android TV: the D-pad (or a swipe pad, switched from the toolbar), then two rows:
 * power, Back, Home and Assistant; mute, volume and play / pause. Everything else is in the toolbar's menu.
 */
@Composable
private fun GoogleTvRemote(
    aod: Boolean, landscape: Boolean, swipe: Boolean, back: dev.padbridge.RemoteButton, home: dev.padbridge.RemoteButton,
    modifier: Modifier, big: Boolean = false,
) {
    val power = remember { dev.padbridge.RemoteButton("Power", dev.padbridge.RemoteOut.Consumer(Keys.C_POWER)) }
    val assistant = remember {
        dev.padbridge.RemoteButton("Assistant", dev.padbridge.RemoteOut.Consumer(dev.padbridge.RemoteProfile.AC_SEARCH))
    }
    val mute = remember { dev.padbridge.RemoteButton("Mute", dev.padbridge.RemoteOut.Consumer(Keys.C_MUTE)) }
    val play = remember { dev.padbridge.RemoteButton("Play / pause", dev.padbridge.RemoteOut.Consumer(Keys.C_PLAY_PAUSE)) }
    val key = if (big) 76.dp else if (landscape) 52.dp else 60.dp
    val gap = if (big) 22.dp else if (landscape) 12.dp else 18.dp

    val pad: @Composable (Modifier) -> Unit = { m ->
        androidx.compose.foundation.layout.BoxWithConstraints(m, contentAlignment = Alignment.Center) {
            if (swipe) AndroidView(
                factory = { dev.padbridge.SwipePadView(it) },
                update = { v -> aod; v.invalidate() },
                modifier = Modifier.size(minOf(maxWidth, if (big) 620.dp else 460.dp), minOf(maxHeight, if (big) 620.dp else 460.dp)),
            ) else {
                val side = minOf(maxWidth - 16.dp, maxHeight, if (big) 560.dp else 400.dp)
                AndroidView(factory = { DpadView(it) }, update = { v -> aod; v.invalidate() }, modifier = Modifier.size(side))
            }
        }
    }
    val buttons: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(gap), horizontalAlignment = Alignment.CenterHorizontally) {
            // Two even rows, four keys wide each (the rocker counts as two).
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                RemoteKey(power, Icons.Rounded.PowerSettingsNew, aod, key)
                RemoteKey(back, Icons.AutoMirrored.Rounded.ArrowBack, aod, key)
                RemoteKey(home, Icons.Rounded.Home, aod, key)
                RemoteKey(assistant, Icons.Rounded.Mic, aod, key)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(gap), verticalAlignment = Alignment.CenterVertically) {
                RemoteKey(mute, Icons.AutoMirrored.Rounded.VolumeOff, aod, key)
                VolumeRocker(aod, key * 2 + gap, key, horizontal = true)
                RemoteKey(play, Icons.Rounded.PlayArrow, aod, key, secondIcon = Icons.Rounded.Pause)
            }
        }
    }

    Column(modifier) {
        if (landscape) {
            Row(Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                pad(Modifier.weight(1f).fillMaxHeight())
                Spacer(Modifier.width(24.dp))
                buttons()
            }
        } else {
            pad(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { buttons() }
        }
    }
}

/**
 * Voice typing: the phone's speech recognizer (Google's voice input) listens, and what it heard is typed on the
 * computer or TV as keystrokes. Handy for TV search boxes. No microphone permission: the recognizer app holds it.
 */
@Composable
private fun VoiceTypeButton() {
    val ctx = LocalContext.current
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
    ) { r ->
        val text = r.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (r.resultCode == android.app.Activity.RESULT_OK && !text.isNullOrBlank()) {
            val skipped = HidManager.type(text)
            if (skipped > 0) android.widget.Toast.makeText(ctx, "$skipped characters can't be typed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    FilledTonalIconButton(onClick = {
        val i = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak to type")
        try { launcher.launch(i) } catch (_: android.content.ActivityNotFoundException) {
            android.widget.Toast.makeText(ctx, "No voice input on this phone", android.widget.Toast.LENGTH_SHORT).show()
        }
    }, modifier = Modifier.padding(start = 8.dp)) { Icon(Icons.Rounded.KeyboardVoice, "Voice typing") }
}

/** Siri Remote-style volume rocker: one tall pill, + on top and − below. */
@Composable
private fun VolumeRocker(
    aod: Boolean, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, horizontal: Boolean = false,
) {
    val up = remember { dev.padbridge.RemoteButton("Volume up", dev.padbridge.RemoteOut.Consumer(Keys.C_VOL_UP)) }
    val down = remember { dev.padbridge.RemoteButton("Volume down", dev.padbridge.RemoteOut.Consumer(Keys.C_VOL_DOWN)) }
    var upPressed by remember { mutableStateOf(false) }
    var downPressed by remember { mutableStateOf(false) }
    val shape = androidx.compose.foundation.shape.CircleShape
    Surface(
        shape = shape,
        color = if (dev.padbridge.Real.active && !dev.padbridge.Real.dark) MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = if (dev.padbridge.Real.active) 3.dp else 0.dp,
        modifier = Modifier.size(width, height).aodOutline(aod, shape),
    ) {
        val halves = listOf(Triple(up, Icons.Rounded.Add, upPressed), Triple(down, Icons.Rounded.Remove, downPressed))
            .let { if (horizontal) it.reversed() else it } // sideways: − on the left, + on the right
        @Composable
        fun Half(m: Modifier, t: Triple<dev.padbridge.RemoteButton, ImageVector, Boolean>) = Box(
            m.background(if (t.third) MaterialTheme.colorScheme.surfaceContainerHighest else androidx.compose.ui.graphics.Color.Transparent)
                .remotePress(t.first) { if (t.first === up) upPressed = it else downPressed = it },
            contentAlignment = Alignment.Center,
        ) { Icon(t.second, null, Modifier.size(26.dp)) }
        if (horizontal) Row { halves.forEach { Half(Modifier.weight(1f).fillMaxHeight(), it) } }
        else Column { halves.forEach { Half(Modifier.weight(1f).fillMaxWidth(), it) } }
    }
}


// ====================================================================== shared bits

data class MenuEntry(
    val label: String, val icon: ImageVector, val checked: Boolean? = null, val divider: Boolean = false,
    val action: () -> Unit,
)

/** ⋮ menu for secondary toolbar actions, so input screens keep a calm top row. */
@Composable
fun OverflowMenu(entries: List<MenuEntry>) {
    var open by remember { mutableStateOf(false) }
    Box {
        androidx.compose.material3.IconButton(onClick = { open = true }) { Icon(Icons.Rounded.MoreVert, "More options") }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }, shape = RoundedCornerShape(16.dp),
            containerColor = glassMenuColor()) {
            entries.forEach { e ->
                if (e.divider) androidx.compose.material3.HorizontalDivider(Modifier.padding(vertical = 4.dp))
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(e.label) },
                    leadingIcon = { Icon(e.icon, null) },
                    trailingIcon = if (e.checked == true) ({ Icon(Icons.Rounded.Check, null) }) else null,
                    onClick = { open = false; e.action() },
                )
            }
        }
    }
}

/** Connected computer at a glance (dot + name), for screens where the title would repeat the tab. */
@Composable
fun ConnectionChip(hid: HidUi, aod: Boolean, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    val cs = MaterialTheme.colorScheme
    Surface(
        shape = shape,
        color = if (hid.connected) cs.secondaryContainer else cs.surfaceContainerHigh,
        modifier = modifier.aodOutline(aod, shape),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (aod) {
                Text(rememberClock(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(8.dp))
            }
            Box(Modifier.size(8.dp).background(if (hid.connected) cs.tertiary else cs.outline, androidx.compose.foundation.shape.CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                if (hid.connected) hid.hostName ?: "Connected" else "Not connected",
                style = MaterialTheme.typography.labelLarge, maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp),
            )
        }
    }
}

/**
 * Shown on input screens while nothing is connected: one tap reconnects to the last computer,
 * or jumps to the Connect tab. Hidden while connected.
 */
/**
 * Shown once the phone's own three-finger gesture (usually a screenshot swipe) cancelled a trackpad gesture: the
 * phone takes three-finger swipes first, so PadBridge's three-finger gestures can't work until it's turned off.
 */
@Composable
fun ScreenshotGestureNotice(modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val show by dev.padbridge.SystemGestureNotice.show
    AnimatedVisibility(show, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.tertiaryContainer, modifier = modifier.fillMaxWidth()) {
            Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
                Text("Your phone took that three-finger swipe", style = MaterialTheme.typography.titleSmall)
                Text(
                    "Its own three-finger screenshot gesture is on, so three-finger swipes and taps on the trackpad get " +
                        "cancelled. Turn it off in the phone's Settings (often under Gestures, Motions and gestures or " +
                        "Advanced features) to use them here.",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    androidx.compose.material3.TextButton(onClick = {
                        try { ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_SETTINGS)) } catch (_: Exception) {}
                    }) { Text("Open Settings") }
                    androidx.compose.material3.TextButton(onClick = {
                        dev.padbridge.SystemGestureNotice.show.value = false; Settings.screenshotNoticeDismissed = true
                    }) { Text("Got it") }
                }
            }
        }
    }
}

@Composable
fun NotConnectedBanner(hid: HidUi, onGoConnect: () -> Unit, modifier: Modifier = Modifier) {
    val last = remember(hid.version) { HidManager.lastHostDevice() }
    // Registering is a normal second or two at startup: show progress, not a "Fix" button.
    val connecting = hid.state == HidManager.State.CONNECTING || hid.state == HidManager.State.REGISTERING
    val ready = hid.state == HidManager.State.READY
    val fix = rememberBluetoothFix(hid)
    // With the top bar's status pill on screen, "Not connected" / "Connecting…" would repeat it: keep the
    // banner only for a one-tap reconnect or a problem that needs fixing.
    val topBar = LocalTopBarStatus.current
    val visible = !hid.connected && (!topBar || (!connecting && (!ready || last != null)))
    AnimatedVisibility(visible, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (!ready && !connecting) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = if (!ready && !connecting) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface,
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (connecting) androidx.compose.material3.LoadingIndicator(Modifier.size(28.dp))
                else Icon(if (ready) Icons.Rounded.LinkOff else Icons.Rounded.ErrorOutline, null)
                Spacer(Modifier.width(12.dp))
                Text(
                    when {
                        hid.state == HidManager.State.REGISTERING -> "Starting Bluetooth…"
                        connecting -> "Connecting…"
                        ready && topBar && last != null -> "Last used"
                        ready -> "Not connected"
                        else -> fix.problem.ifEmpty { "Bluetooth isn't ready" }
                    },
                    style = MaterialTheme.typography.bodyMedium, maxLines = 3,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!connecting) {
                    when {
                        ready && last != null -> androidx.compose.material3.FilledTonalButton(onClick = { HidManager.connect(last) }) {
                            Text("Connect to ${HidManager.name(last)}", maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 160.dp))
                        }
                        ready -> androidx.compose.material3.TextButton(onClick = onGoConnect) { Text("Choose computer") }
                        fix.action != null -> androidx.compose.material3.Button(onClick = fix.run) { Text(fix.action) }
                        else -> androidx.compose.material3.TextButton(onClick = onGoConnect) { Text("Details") }
                    }
                }
            }
        }
    }
}
