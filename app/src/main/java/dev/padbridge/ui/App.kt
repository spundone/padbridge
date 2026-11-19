package dev.padbridge.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Mouse
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mouse
import androidx.compose.material.icons.rounded.SettingsRemote
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.padbridge.HidManager
import dev.padbridge.MainActivity
import dev.padbridge.Settings

/** Immutable snapshot of [HidManager] for Compose. [version] bumps on every change. */
data class HidUi(
    val version: Int,
    val state: HidManager.State,
    val detail: String,
    val hostName: String?,
    val hostAddress: String?,
    val connectingAddress: String?,
    val leds: Int,
    val needsBluetoothRestart: Boolean = false,
    val discovering: Boolean = false,
    val foundCount: Int = 0,
    val hostRefused: Boolean = false,
) {
    val connected get() = state == HidManager.State.CONNECTED

    companion object {
        fun capture(version: Int) = HidUi(
            version, HidManager.state, HidManager.detail,
            HidManager.host?.let { HidManager.name(it) }, HidManager.host?.address,
            HidManager.connectingTo?.address, HidManager.leds, HidManager.needsBluetoothRestart,
            HidManager.discovering, HidManager.found.size, HidManager.hostRefused,
        )
    }
}

/** Bottom bar / rail. Air mouse lives in the trackpad's menu, so the bar stays at five calm tabs. */
val NavDests get() = listOf(Dest.CONNECT, Dest.TRACKPAD, Dest.KEYBOARD, Dest.REMOTE, Dest.GAMEPAD)

enum class Dest(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    CONNECT("Connect", Icons.Rounded.Bluetooth, Icons.Rounded.BluetoothConnected),
    TRACKPAD("Trackpad", Icons.Outlined.TouchApp, Icons.Rounded.TouchApp),
    KEYBOARD("Keyboard", Icons.Outlined.Keyboard, Icons.Rounded.Keyboard),
    AIR_MOUSE("Air mouse", Icons.Outlined.Mouse, Icons.Rounded.Mouse),
    REMOTE("Remote", Icons.Outlined.SettingsRemote, Icons.Rounded.SettingsRemote),
    GAMEPAD("Gamepad", Icons.Outlined.SportsEsports, Icons.Rounded.SportsEsports),
}

@Composable
fun PadBridgeApp(hid: HidUi, activity: MainActivity) {
    var dest by rememberSaveable { mutableStateOf(if (Settings.lastHost != null) Dest.TRACKPAD else Dest.CONNECT) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    // Bumped when the computer type changes so OS-specific screens rebuild.
    var osVersion by remember { mutableIntStateOf(0) }

    var fullscreen by rememberSaveable { mutableStateOf(Settings.fullscreenInput) }
    val toggleFullscreen = { fullscreen = !fullscreen; Settings.fullscreenInput = fullscreen }
    val inputDest = dest == Dest.TRACKPAD || dest == Dest.KEYBOARD || dest == Dest.AIR_MOUSE || dest == Dest.REMOTE
    // AOD: the same screen as thin dim outlines on black, at minimum brightness; input keeps working.
    var aod by rememberSaveable { mutableStateOf(false) }
    val aodActive = aod && !settingsOpen && (dest == Dest.TRACKPAD || dest == Dest.AIR_MOUSE || dest == Dest.REMOTE)
    // Full display: trackpad on every pixel, under the cutout, no chrome.
    var fullDisplay by rememberSaveable { mutableStateOf(false) }
    val fullDisplayActive = fullDisplay && !settingsOpen && (dest == Dest.TRACKPAD || dest == Dest.KEYBOARD)
    LaunchedEffect(fullDisplayActive) { activity.setFullDisplay(fullDisplayActive) }
    LaunchedEffect(aodActive) { activity.setAod(aodActive) }
    // Dim to AOD after a while without touching the screen (Settings › Behavior), on the screens that have it,
    // while connected. A touch wakes it up again; AOD turned on by hand stays on.
    var autoAodAt by remember { mutableStateOf(0L) }
    val aodCapable = !settingsOpen && (dest == Dest.TRACKPAD || dest == Dest.AIR_MOUSE || dest == Dest.REMOTE)
    LaunchedEffect(aodCapable, aod, hid.connected, settingsOpen) {
        val secs = Settings.idleAodSeconds
        if (!aodCapable || aod || !hid.connected || secs <= 0) return@LaunchedEffect
        while (true) {
            val left = dev.padbridge.Idle.lastInput + secs * 1000L - android.os.SystemClock.uptimeMillis()
            if (left <= 0) { autoAodAt = android.os.SystemClock.uptimeMillis(); aod = true; break }
            kotlinx.coroutines.delay(left)
        }
    }
    LaunchedEffect(dev.padbridge.Idle.lastInput) {
        if (autoAodAt > 0 && aod && dev.padbridge.Idle.lastInput > autoAodAt) { aod = false; autoAodAt = 0 }
    }
    LaunchedEffect(aod) { if (!aod) autoAodAt = 0 }
    // The switch in Settings can change it too.
    LaunchedEffect(settingsOpen) { if (!settingsOpen) fullscreen = Settings.fullscreenInput }

    LaunchedEffect(dest, settingsOpen, fullscreen, aodActive, fullDisplayActive) {
        activity.setGamepadMode(dest == Dest.GAMEPAD && !settingsOpen)
        activity.setBarsHidden(
            !settingsOpen && (dest == Dest.GAMEPAD || (fullscreen && inputDest) || aodActive || fullDisplayActive)
        )
    }
    // Seamless: once a computer connects while you're on the Connect tab, go straight to the trackpad.
    var wasConnected by remember { mutableStateOf(hid.connected) }
    LaunchedEffect(hid.connected) {
        if (hid.connected && !wasConnected && dest == Dest.CONNECT && !settingsOpen) dest = Dest.TRACKPAD
        wasConnected = hid.connected
    }
    val requested = activity.requestedDest.value
    LaunchedEffect(requested) {
        Dest.entries.firstOrNull { it.name == requested }?.let { dest = it; settingsOpen = false }
        activity.requestedDest.value = null
    }

    var setupOpen by rememberSaveable { mutableStateOf(!Settings.setupDone) }
    if (setupOpen) {
        SetupScreen(hid, activity) { setupOpen = false; settingsOpen = false; osVersion++ }
        return
    }
    if (settingsOpen) {
        BackHandler { settingsOpen = false }
        SettingsScreen(
            hid = hid, onBack = { settingsOpen = false }, onOsChanged = { osVersion++ }, activity = activity,
            onRunSetup = { setupOpen = true },
        )
        return
    }
    if (dest == Dest.GAMEPAD) {
        BackHandler { dest = Dest.TRACKPAD }
        GamepadScreen(onExit = { dest = Dest.TRACKPAD })
        return
    }

    if (fullDisplayActive) {
        BackHandler { if (aodActive) aod = false else fullDisplay = false }
        val screen: @Composable () -> Unit = {
            androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surface) {
                Box(Modifier.fillMaxSize().aodPixelShift(aodActive)) {
                    if (dest == Dest.KEYBOARD) FullDisplayKeyboard(hid, osVersion, onExit = { fullDisplay = false })
                    else FullDisplayTrackpad(hid, osVersion, aodActive, onAod = { aod = !aod }, onExit = { fullDisplay = false })
                }
            }
        }
        if (aodActive) AodTheme(screen) else screen()
        return
    }

    // Fullscreen mode, and landscape trackpad / keyboard (as DeX does): the bars would leave only a strip.
    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // Tablets have room for the rail and top bar in landscape, so only phones drop them.
    val tablet = isTablet()
    if (inputDest && (fullscreen || (landscape && !tablet) || aodActive)) {
        BackHandler {
            when {
                aodActive -> aod = false
                dest == Dest.AIR_MOUSE -> dest = Dest.TRACKPAD
                fullscreen -> toggleFullscreen()
                else -> dest = Dest.CONNECT
            }
        }
        val screen: @Composable () -> Unit = {
            androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.surface) {
                Box(
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.safeDrawing)
                        .padding(top = 8.dp)
                        .aodPixelShift(aodActive)
                ) {
                    InputDest(
                        dest, hid, osVersion, fullscreen, toggleFullscreen, aodActive, { aod = !aod }, { fullDisplay = true },
                        { aod = false; fullscreen = false; Settings.fullscreenInput = false; dest = Dest.CONNECT },
                    ) { dest = Dest.AIR_MOUSE }
                }
            }
        }
        // AOD keeps the same screen, redrawn as dim outlines on black (like a watch face's ambient mode).
        if (aodActive) AodTheme(screen) else screen()
        return
    }

    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

    // Back from an input tab returns to Connect instead of closing the app mid-use.
    BackHandler(enabled = dest != Dest.CONNECT) { dest = if (dest == Dest.AIR_MOUSE) Dest.TRACKPAD else Dest.CONNECT }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = { Text("PadBridge", fontWeight = FontWeight.SemiBold) },
                actions = {
                    // The Connect tab's status card already says this, larger.
                    if (dest != Dest.CONNECT) StatusPill(hid) { dest = Dest.CONNECT }
                    IconButton(onClick = { settingsOpen = true }) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            if (!wide) ShortNavigationBar {
                NavDests.forEach { d ->
                    ShortNavigationBarItem(
                        selected = dest == d || (d == Dest.TRACKPAD && dest == Dest.AIR_MOUSE),
                        onClick = { dest = d },
                        icon = { Icon(if (dest == d) d.selectedIcon else d.icon, contentDescription = null) },
                        label = { Text(d.label) },
                    )
                }
            }
        },
    ) { padding ->
        val enterSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
        val exitSpec = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
        val slideSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
        androidx.compose.foundation.layout.Row(Modifier.padding(padding).fillMaxSize()) {
        if (wide) {
            // Tablets / landscape: a rail instead of a bottom bar, so content keeps its height.
            androidx.compose.material3.NavigationRail(
                containerColor = MaterialTheme.colorScheme.surface,
                windowInsets = androidx.compose.foundation.layout.WindowInsets(0),
            ) {
                // Scrolls when six destinations don't fit (phones in landscape).
                androidx.compose.foundation.layout.Column(
                    Modifier.fillMaxHeight().verticalScroll(androidx.compose.foundation.rememberScrollState()),
                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    NavDests.forEach { d ->
                        androidx.compose.material3.NavigationRailItem(
                            selected = dest == d,
                            onClick = { dest = d },
                            icon = { Icon(if (dest == d) d.selectedIcon else d.icon, contentDescription = null) },
                            label = { Text(d.label) },
                        )
                    }
                }
            }
        }
        AnimatedContent(
            targetState = dest,
            // Expressive shared-axis: a short slide toward the tab you picked, on the motion scheme's spring.
            transitionSpec = {
                val dir = if (targetState.ordinal >= initialState.ordinal) 1 else -1
                (androidx.compose.animation.slideInHorizontally(slideSpec) { w -> dir * w / 8 } + fadeIn(enterSpec)) togetherWith
                    (androidx.compose.animation.slideOutHorizontally(slideSpec) { w -> -dir * w / 8 } + fadeOut(exitSpec))
            },
            modifier = Modifier.weight(1f).fillMaxSize(),
            label = "dest",
        ) { d ->
            androidx.compose.runtime.CompositionLocalProvider(LocalTopBarStatus provides true) {
            when (d) {
                Dest.CONNECT -> ConnectScreen(
                    hid, osVersion, onOsChanged = { osVersion++ }, onOpenSettings = { settingsOpen = true },
                    onOpen = { dest = it },
                )
                Dest.TRACKPAD, Dest.KEYBOARD, Dest.AIR_MOUSE, Dest.REMOTE ->
                    InputDest(d, hid, osVersion, fullscreen, toggleFullscreen, false, { aod = true }, { fullDisplay = true }, { dest = Dest.CONNECT }) { dest = Dest.AIR_MOUSE }
                Dest.GAMEPAD -> Box(Modifier)
            }
            }
        }
        }
    }
}

@Composable
private fun InputDest(
    d: Dest, hid: HidUi, osVersion: Int, fullscreen: Boolean, onToggleFullscreen: () -> Unit,
    aod: Boolean, onAod: () -> Unit, onFullDisplay: () -> Unit = {}, onGoConnect: () -> Unit = {},
    onAirMouse: () -> Unit = {},
) {
    when (d) {
        Dest.TRACKPAD -> TrackpadScreen(hid, osVersion, fullscreen, onToggleFullscreen, onAod, aod, onFullDisplay, onGoConnect, onAirMouse)
        Dest.KEYBOARD -> KeyboardScreen(hid, osVersion, fullscreen, onToggleFullscreen, onFullDisplay, onGoConnect)
        Dest.AIR_MOUSE -> AirMouseScreen(fullscreen, onToggleFullscreen, onAod, aod)
        Dest.REMOTE -> RemoteScreen(hid, osVersion, aod, onAod, onGoConnect)
        else -> {}
    }
}

/**
 * True inside the main layout, where the top bar's [StatusPill] already shows the connection. Input screens
 * then leave out their own connection chip and plain "Not connected" banner, so the status appears once.
 */
val LocalTopBarStatus = androidx.compose.runtime.staticCompositionLocalOf { false }

@Composable
fun StatusPill(hid: HidUi, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val (bg, fg) = when (hid.state) {
        HidManager.State.CONNECTED -> cs.primaryContainer to cs.onPrimaryContainer
        HidManager.State.CONNECTING, HidManager.State.REGISTERING -> cs.tertiaryContainer to cs.onTertiaryContainer
        HidManager.State.READY -> cs.surfaceContainerHigh to cs.onSurfaceVariant
        else -> cs.errorContainer to cs.onErrorContainer
    }
    val text = when (hid.state) {
        HidManager.State.CONNECTED -> hid.hostName ?: "Connected"
        HidManager.State.CONNECTING -> "Connecting"
        HidManager.State.REGISTERING -> "Starting"
        HidManager.State.READY -> "Not connected"
        else -> "Needs attention"
    }
    val colorSpec = MaterialTheme.motionScheme.defaultEffectsSpec<androidx.compose.ui.graphics.Color>()
    val bgA by androidx.compose.animation.animateColorAsState(bg, colorSpec, label = "pillBg")
    val fgA by androidx.compose.animation.animateColorAsState(fg, colorSpec, label = "pillFg")
    Surface(
        onClick = onClick, shape = CircleShape, color = bgA, contentColor = fgA,
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
    ) {
        Row(
            Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (hid.state == HidManager.State.CONNECTING || hid.state == HidManager.State.REGISTERING) {
                LoadingIndicator(Modifier.size(20.dp), color = fg)
            } else {
                Box(Modifier.size(8.dp).background(fg, CircleShape))
            }
            Spacer(Modifier.width(8.dp))
            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    (androidx.compose.animation.slideInVertically { it / 2 } + fadeIn()) togetherWith
                        (androidx.compose.animation.slideOutVertically { -it / 2 } + fadeOut())
                },
                label = "pillText",
            ) { t ->
                Text(
                    t, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 160.dp),
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(start = 8.dp, top = 20.dp, bottom = 8.dp),
    )
}

val ScreenPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)

@Composable
fun Gap(h: Int = 8) = Spacer(Modifier.size(h.dp))

@Composable
fun segColors() = androidx.compose.material3.ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
)
