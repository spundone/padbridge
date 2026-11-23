package dev.padbridge.ui

import androidx.compose.animation.togetherWith
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.padbridge.GAction
import dev.padbridge.GestureSlot
import dev.padbridge.HidDescriptor
import dev.padbridge.HidManager
import dev.padbridge.HidService
import dev.padbridge.MainActivity
import dev.padbridge.Settings

/** A toggle row inside a segmented group. */
@Composable
private fun SwitchRow(title: String, subtitle: String?, index: Int, count: Int, initial: Boolean, key: Any? = null, onChange: (Boolean) -> Unit) {
    var on by remember(key) { mutableStateOf(initial) }
    SegmentedListItem(
        onClick = { on = !on; onChange(on) },
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segColors(),
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = { Switch(checked = on, onCheckedChange = { on = it; onChange(it) }) },
    ) { Text(title) }
}

/**
 * Haptic feedback: on / off, and the style. Phone motors get crisp preset effects; basic motors (most tablets, like
 * a Galaxy Tab) can't play those, so they get plain pulses. Without any motor it offers a quiet click sound.
 */
@Composable
private fun HapticsRow() {
    var on by remember { mutableStateOf(Settings.haptics) }
    var style by remember { mutableIntStateOf(Settings.hapticStyle) }
    var sound by remember { mutableStateOf(Settings.hapticSound) }
    val motor = dev.padbridge.Haptics.available
    SegmentedListItem(
        onClick = { on = !on; Settings.haptics = on },
        shapes = ListItemDefaults.segmentedShapes(0, 3),
        colors = segColors(),
        trailingContent = { Switch(checked = on, onCheckedChange = { on = it; Settings.haptics = it }) },
        supportingContent = {
            Column {
                if (!motor) {
                    Text("This device has no vibration motor.")
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Click sound instead", Modifier.weight(1f))
                        Switch(checked = sound, onCheckedChange = { sound = it; Settings.hapticSound = it })
                    }
                } else if (on) {
                    Text(
                        when (style) {
                            1 -> "Rich: crisp ticks and clicks for phone haptic motors."
                            2 -> "Basic: plain pulses that simple motors (most tablets) can play."
                            else -> "Automatic: " + if (dev.padbridge.Haptics.rich) "rich, this motor supports it." else
                                "basic, this motor can't play rich effects."
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        val labels = listOf("Auto", "Rich", "Basic")
                        labels.forEachIndexed { i, label ->
                            androidx.compose.material3.ToggleButton(
                                checked = style == i,
                                onCheckedChange = {
                                    style = i; Settings.hapticStyle = i
                                    dev.padbridge.Haptics.click() // feel the new style
                                },
                                modifier = Modifier.weight(1f),
                                shapes = when (i) {
                                    0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    labels.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                            ) { Text(label, maxLines = 1) }
                        }
                    }
                }
            }
        },
    ) { Text("Haptic feedback") }
}

/** Do Not Disturb while full screen. Needs the user to grant Do Not Disturb access once. */
@Composable
private fun DndRow() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val activity = ctx as? dev.padbridge.MainActivity
    var on by remember { mutableStateOf(Settings.fullscreenDnd) }
    var access by remember { mutableStateOf(activity?.dndAccess() ?: false) }
    // Coming back from the system's access screen.
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(owner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, ev ->
            if (ev == androidx.lifecycle.Lifecycle.Event.ON_RESUME) access = activity?.dndAccess() ?: false
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }
    fun openAccess() = try {
        ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
    } catch (_: Exception) {}
    SegmentedListItem(
        onClick = { on = !on; Settings.fullscreenDnd = on; if (on && !access) openAccess() },
        shapes = ListItemDefaults.segmentedShapes(1, 2),
        colors = segColors(),
        supportingContent = {
            Text(
                if (on && !access) "Tap to allow Do Not Disturb access for PadBridge"
                else "Silences notifications while the app bars are hidden (full screen, full display, gamepad). " +
                    "Your own Do Not Disturb is left alone."
            )
        },
        trailingContent = {
            Switch(checked = on, onCheckedChange = {
                on = it; Settings.fullscreenDnd = it; if (it && !access) openAccess()
            })
        },
    ) { Text("Do Not Disturb in full screen") }
}

@Composable
private fun SliderRow(
    title: String, index: Int, count: Int, initial: Float,
    range: ClosedFloatingPointRange<Float> = 0.3f..3.3f, steps: Int = 29,
    format: (Float) -> String = { "${"%.1f".format(it)}×" },
    onChange: (Float) -> Unit,
) {
    var v by remember { mutableFloatStateOf(initial) }
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index, count),
        colors = segColors(),
        supportingContent = {
            Slider(
                value = v, onValueChange = { v = it; onChange(it) },
                valueRange = range, steps = steps,
            )
        },
    ) { Text("$title · ${format(v)}") }
}

@Composable
fun SettingsScreen(
    hid: HidUi, onBack: () -> Unit, onOsChanged: () -> Unit, activity: MainActivity,
    onRunSetup: () -> Unit = {}, startPage: SettingsPage? = null,
) {
    var rootOpen by remember { mutableStateOf(false) }
    if (rootOpen) {
        androidx.activity.compose.BackHandler { rootOpen = false }
        RootScreen(onBack = { rootOpen = false })
        return
    }
    val ctx = LocalContext.current
    var page by rememberSaveable { mutableStateOf(startPage) }
    var query by rememberSaveable { mutableStateOf("") }
    // Opened from a mode's menu straight at a page: Back returns to that mode, not to the list.
    androidx.activity.compose.BackHandler(enabled = page != null && androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp < 840) {
        if (startPage != null) onBack() else page = null
    }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var osVersion by remember { mutableIntStateOf(0) }
    var gestureVersion by remember { mutableIntStateOf(0) }
    var picking by remember { mutableStateOf<GestureSlot?>(null) }
    val os = remember(osVersion) { Settings.hostOs }
    // Tablets: list-detail, categories on the left and the open page on the right.
    val wide = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 840
    val shown = if (wide) (page ?: SettingsPage.GENERAL) else page

    val pageList: @Composable (SettingsPage?, Modifier) -> Unit = { shown, modifier ->
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            modifier = modifier,
        ) {
            // Two-pane: the top bar says "Settings", so the page names itself here.
            if (wide && shown != null) item {
                Text(shown.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 8.dp, top = 8.dp))
            }
            when (shown) {
                null -> {
                    item { SettingsSearch(query) { query = it } }
                    if (query.isBlank()) item { SettingsCategories(os, onOpen = { page = it }) }
                    else item { SearchResults(query) { page = it; query = "" } }
                }
                SettingsPage.TRACKPAD -> {
                    item { SectionHeader("Pointer & scrolling") }
                    item { SliderRow("Pointer speed", 0, 7, Settings.pointerSpeed) { Settings.pointerSpeed = it } }
                    item { SliderRow("Scroll speed", 1, 7, Settings.scrollSpeed) { Settings.scrollSpeed = it } }
                    item {
                        SwitchRow("Natural scrolling",
                            if (os == dev.padbridge.HostOs.MAC) "macOS also applies its own setting; off here matches a Mac trackpad"
                            else "Content follows your fingers",
                            2, 7, Settings.naturalScroll, key = osVersion) { Settings.naturalScroll = it }
                    }
                    item { SwitchRow("Scroll momentum", "Keep scrolling after a flick", 3, 7, Settings.momentum) { Settings.momentum = it } }
                    item { SwitchRow("Scroll rails", "Lock to vertical or horizontal when the swipe starts that way", 4, 7, Settings.scrollRails) { Settings.scrollRails = it } }
                    item { SwitchRow("Edge scroll strips", "Slide one finger along the right or bottom edge to scroll", 5, 7, Settings.edgeScroll) { Settings.edgeScroll = it } }
                    item {
                        SwitchRow("Two-finger swipe for back / forward", "Instead of horizontal scrolling (per computer type)",
                            6, 7, Settings.twoFingerNavigate, key = osVersion) { Settings.twoFingerNavigate = it }
                    }
                    item { SectionHeader("Touch") }
                    item { SwitchRow("Tap to click", null, 0, 6, Settings.tapToClick) { Settings.tapToClick = it } }
                    item {
                        SliderRow("Double-tap window", 1, 6, Settings.doubleTapMs, range = 150f..600f, steps = 8,
                            format = { "${it.toInt()} ms" }) { Settings.doubleTapMs = it }
                    }
                    item {
                        SwitchRow("Ignore resting fingers", "A finger left still on the pad doesn't count, like Samsung DeX",
                            2, 6, Settings.restRejection) { Settings.restRejection = it }
                    }
                    item {
                        SwitchRow("Palm rejection", "Ignore the trackpad for a moment while you type on the keyboard, " +
                            "and ignore palm-sized touches, like a laptop", 3, 6, Settings.palmRejection) { Settings.palmRejection = it }
                    }
                    item {
                        SwitchRow("Show click buttons", "Left / right buttons under the trackpad", 4, 6, Settings.showClickButtons) {
                            Settings.showClickButtons = it
                        }
                    }
                    item {
                        SwitchRow("S Pen and stylus", "Hover to move the pointer, touch to click or drag, pen button for right " +
                            "click. Your palm is ignored while the pen is near", 5, 6, Settings.stylus) { Settings.stylus = it }
                    }

                    item { SectionHeader("Native touchpad (beta)") }
                    item { NativeTouchpadSetting(hid) }

                    item { SectionHeader("Three-finger drag") }
                    item {
                        SwitchRow("Drag with three fingers", "Move windows and select text without clicking. Turns off three-finger swipes.",
                            0, 1, Settings.threeFingerDrag) { Settings.threeFingerDrag = it }
                    }
                }
                SettingsPage.GESTURES -> {
                    item { Gap(8) }
                    item {
                        Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            androidx.compose.material3.FilledTonalButton(onClick = { Settings.resetGestures(); gestureVersion++ }) {
                                Text("${os.label} defaults")
                            }
                            androidx.compose.material3.FilledTonalButton(onClick = { Settings.applyDexPreset(); gestureVersion++ }) {
                                Text("Samsung DeX style")
                            }
                        }
                    }
                    item {
                        Text(
                            "Three-finger gestures not working? Many phones (Samsung, Xiaomi, OnePlus, Oppo, Vivo, Motorola) " +
                                "take three-finger swipes for screenshots first. Turn that off in the phone's own Settings, " +
                                "under Gestures or Advanced features.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    item {
                        Text(
                            "Each gesture sends a click, media key or the keyboard shortcut ${os.label} uses for that action. " +
                                "DeX style: three-finger tap = Back, four-finger tap = launcher, three-finger swipes switch apps.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                        )
                    }
                    val groups = GestureSlot.entries.groupBy { it.group }
                    groups.forEach { (group, slots) ->
                        item(key = "g_$group") {
                            Text(group, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp))
                        }
                        items(slots.size, key = { "s_${slots[it].name}" }) { i ->
                            val slot = slots[i]
                            val action = remember(gestureVersion, osVersion) { Settings.gesture(slot) }
                            SegmentedListItem(
                                onClick = { picking = slot },
                                shapes = ListItemDefaults.segmentedShapes(i, slots.size),
                                colors = segColors(),
                                supportingContent = {
                                    Text(
                                        action.label + if (!action.availableOn(os)) " (not available on ${os.label})" else "",
                                        color = if (action == GAction.NONE) MaterialTheme.colorScheme.onSurfaceVariant
                                        else MaterialTheme.colorScheme.primary,
                                    )
                                },
                            ) { Text(slot.title) }
                        }
                    }
                }
                SettingsPage.MOTION -> {
                    item { Gap(8) }
                    item { SliderRow("Air mouse speed", 0, 3, Settings.airSpeed) { Settings.airSpeed = it } }
                    item {
                        SwitchRow("Air mouse always aims", "Move without holding the pad. Tap the pad to click.",
                            1, 3, Settings.airAlwaysAim) { Settings.airAlwaysAim = it }
                    }
                    item {
                        SliderRow("Gamepad gyro sensitivity", 2, 3, Settings.gamepadGyroSens, range = 0.4f..2.5f, steps = 20) {
                            Settings.gamepadGyroSens = it
                        }
                    }
                }
                SettingsPage.KEYBOARD -> {
                    item { SectionHeader("Typing") }
                    item {
                        SwitchRow("Word suggestions", "Above PadBridge's own keyboard; tap one to finish the word",
                            0, 3, Settings.kbPredict) { Settings.kbPredict = it; if (!it) dev.padbridge.Predict.reset() }
                    }
                    item {
                        var learn by remember { mutableStateOf(Settings.kbPredictLearn) }
                        SegmentedListItem(
                            onClick = { learn = !learn; Settings.kbPredictLearn = learn },
                            shapes = ListItemDefaults.segmentedShapes(1, 3),
                            colors = segColors(),
                            trailingContent = { Switch(checked = learn, onCheckedChange = { learn = it; Settings.kbPredictLearn = it }) },
                            supportingContent = {
                                Column {
                                    Text("Kept only on this phone. Anything typed on this keyboard counts, passwords too.")
                                    TextButton(onClick = { dev.padbridge.Predict.forget() }, contentPadding = PaddingValues(0.dp)) {
                                        Text("Forget learned words")
                                    }
                                }
                            },
                        ) { Text("Suggestions learn your words") }
                    }
                    item {
                        SwitchRow("Phone keyboard learns from typing", "Off: Gboard and others type in incognito mode",
                            2, 3, Settings.imeLearning) { Settings.imeLearning = it }
                    }
                    item { SectionHeader("TrackPoint") }
                    item {
                        SwitchRow("TrackPoint on Laptop and Full layouts", "Tap to click, hold Fn to scroll",
                            0, 2, Settings.kbTrackPoint) { Settings.kbTrackPoint = it }
                    }
                    item {
                        SliderRow("TrackPoint sensitivity", 1, 2, Settings.trackPointSpeed, range = 0.3f..3f, steps = 26) {
                            Settings.trackPointSpeed = it
                        }
                    }
                    item { SectionHeader("Keyboard rollover") }
                    item { NkroSetting() }
                    item {
                        Text(
                            "Layout, size, the laptop deck and key options are on the Keyboard tab, under its layout button.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                        )
                    }
                }
                SettingsPage.SCREEN -> {
                    item { SectionHeader("While you use PadBridge") }
                    item {
                        SwitchRow("Keep screen on while connected", null, 0, 2, Settings.keepScreenOn) {
                            Settings.keepScreenOn = it; activity.applyKeepScreenOn()
                        }
                    }
                    item {
                        val steps = listOf(0, 15, 30, 60, 120, 300)
                        SliderRow(
                            "Dim to outline mode when idle", 1, 2,
                            steps.indexOf(Settings.idleAodSeconds).coerceAtLeast(0).toFloat(),
                            range = 0f..(steps.size - 1).toFloat(), steps = steps.size - 2,
                            format = { i ->
                                when (val s = steps[i.toInt().coerceIn(0, steps.lastIndex)]) {
                                    0 -> "Never"; in 1..59 -> "$s s"; else -> "${s / 60} min"
                                }
                            },
                        ) { Settings.idleAodSeconds = steps[it.toInt().coerceIn(0, steps.lastIndex)] }
                    }
                    item { SectionHeader("Full screen") }
                    item {
                        SwitchRow("Hide app bars on input screens", "Trackpad, keyboard and air mouse use the whole height. Back shows them again.",
                            0, 2, Settings.fullscreenInput) { Settings.fullscreenInput = it }
                    }
                    item { DndRow() }
                }
                SettingsPage.APPEARANCE -> {
                    item { Gap(8) }
                    item { AppearanceSettings() }
                }
                SettingsPage.GENERAL -> {
                    item { SectionHeader("Computer type") }
                    item { OsSelector(osVersion) { osVersion++; gestureVersion++; onOsChanged() } }
                    item { SectionHeader("Behavior") }
                    item { HapticsRow() }
                    item { SwitchRow("Reconnect to last computer", "When PadBridge starts", 1, 3, Settings.autoReconnect) { Settings.autoReconnect = it } }
                    item { SwitchRow("Show all paired devices", "Phones, speakers and others too, not only computers and TVs", 2, 3, Settings.showAllDevices) { Settings.showAllDevices = it } }

                    item { SectionHeader("More") }
                    val rootBuild = dev.padbridge.BuildConfig.ROOT_FEATURES
                    item {
                        SegmentedListItem(
                            onClick = onRunSetup,
                            shapes = ListItemDefaults.segmentedShapes(0, if (rootBuild) 2 else 1),
                            colors = segColors(),
                            leadingContent = { Icon(Icons.Rounded.RestartAlt, null) },
                            supportingContent = { Text("Permissions, computer type and pairing") },
                        ) { Text("Run setup again") }
                    }
                    if (rootBuild) item {
                        SegmentedListItem(
                            onClick = { rootOpen = true },
                            shapes = ListItemDefaults.segmentedShapes(1, 2),
                            colors = segColors(),
                            leadingContent = { Icon(Icons.Rounded.Science, null) },
                            supportingContent = { Text("Bluetooth speaker, device class, controller identity (needs root)") },
                        ) { Text("Root mode (experimental)") }
                    }
                }
                SettingsPage.ABOUT -> {
                    item { Gap(8) }
                    item { AboutSection(activity) }
                    item {
                        Column(Modifier.padding(top = 16.dp)) {
                            SwitchRow(
                                "Check for updates automatically",
                                if (dev.padbridge.Updater.SOURCE == "Google Play") "Ask Google Play when the app opens"
                                else "Once a day, from GitHub releases. The only time PadBridge uses the internet",
                                0, 1, Settings.autoUpdateCheck,
                            ) { Settings.autoUpdateCheck = it }
                        }
                    }
                    item { Column(Modifier.padding(top = 16.dp)) { AboutDeveloper() } }
                    item { Column(Modifier.padding(top = 12.dp)) { OtherApps() } }

                    item {
                        Button(
                            onClick = {
                                ctx.startService(Intent(ctx, HidService::class.java).setAction(HidService.ACTION_STOP))
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                            ),
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                        ) {
                            Icon(Icons.Rounded.PowerSettingsNew, null)
                            Text("  Stop PadBridge")
                        }
                    }
                }
            }
            if (shown == null) item {
                Text(
                    "PadBridge ${activity.versionName()} · HID descriptor v${HidDescriptor.VERSION} · ${hid.state.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }

    // Pages slide in from the side (deeper) or back (to the list), on the expressive spring.
    val slide = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val fadeIn = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val fadeOut = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val animatedPages: @Composable (SettingsPage?, Modifier) -> Unit = { target, m ->
        androidx.compose.animation.AnimatedContent(
            targetState = target,
            transitionSpec = {
                val dir = when {
                    initialState == null -> 1
                    targetState == null -> -1
                    else -> if (targetState!!.ordinal >= initialState!!.ordinal) 1 else -1
                }
                (androidx.compose.animation.slideInHorizontally(slide) { w -> dir * w / 6 } + androidx.compose.animation.fadeIn(fadeIn)) togetherWith
                    (androidx.compose.animation.slideOutHorizontally(slide) { w -> -dir * w / 6 } + androidx.compose.animation.fadeOut(fadeOut))
            },
            modifier = m,
            label = "settingsPage",
        ) { pg -> pageList(pg, Modifier.fillMaxSize()) }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(if (wide) "Settings" else page?.title ?: "Settings") },
                subtitle = { Text(if (wide) "PadBridge for ${os.label}" else page?.subtitle?.invoke(os) ?: "PadBridge for ${os.label}") },
                navigationIcon = {
                    IconButton(onClick = { if (page != null && !wide && startPage == null) page = null else onBack() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                scrollBehavior = scroll,
            )
        },
    ) { padding ->
        if (wide) Row(Modifier.padding(padding).fillMaxSize().padding(horizontal = 8.dp)) {
            Column(
                Modifier.width(360.dp).fillMaxHeight()
                    .verticalScroll(androidx.compose.foundation.rememberScrollState())
                    .padding(ScreenPadding),
            ) {
                SettingsSearch(query) { query = it }
                if (query.isBlank()) SettingsCategories(os, selected = shown, onOpen = { page = it })
                else SearchResults(query) { page = it; query = "" }
                Text(
                    "PadBridge ${activity.versionName()} · HID descriptor v${HidDescriptor.VERSION} · ${hid.state.name.lowercase()}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
            Box(Modifier.weight(1f).fillMaxHeight()) { animatedPages(shown, Modifier.fillMaxSize()) }
        } else animatedPages(shown, Modifier.padding(padding).fillMaxWidth())
    }

    picking?.let { slot ->
        ActionPicker(slot, os = os, onDismiss = { picking = null }) { a ->
            Settings.setGesture(slot, a); gestureVersion++; picking = null
        }
    }
}

@Composable
private fun ActionPicker(slot: GestureSlot, os: dev.padbridge.HostOs, onDismiss: () -> Unit, onPick: (GAction) -> Unit) {
    val current = remember { Settings.gesture(slot) }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        containerColor = glassSheetColor(), scrimColor = glassScrim()) {
        GlassBehind()
        Column(Modifier.padding(horizontal = 16.dp)) {
            Text(slot.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("Default: ${GAction.default(os, slot).label}", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
        }
        val actions = GAction.entries
        LazyColumn(
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            items(actions.size) { i ->
                val a = actions[i]
                val available = a.availableOn(os)
                val selected = a == current
                SegmentedListItem(
                    onClick = { onPick(a) },
                    enabled = available,
                    shapes = ListItemDefaults.segmentedShapes(i, actions.size),
                    colors = if (selected) ListItemDefaults.segmentedColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ) else segColors(),
                    supportingContent = shortcutText(a, os)?.let { { Text(it) } },
                    trailingContent = if (selected) ({ Icon(Icons.Rounded.Check, null) }) else null,
                ) { Text(a.label) }
            }
        }
    }
}

/** Human-readable shortcut, e.g. "Ctrl + Win + →". */
fun shortcutText(a: GAction, os: dev.padbridge.HostOs): String? {
    if (!a.availableOn(os)) return "No ${os.label} shortcut"
    val c = a.combo(os) ?: return when (a) {
        GAction.NONE -> null
        GAction.LEFT_CLICK, GAction.RIGHT_CLICK, GAction.MIDDLE_CLICK, GAction.DOUBLE_CLICK -> "Mouse button"
        else -> "Media key"
    }
    val mac = os == dev.padbridge.HostOs.MAC
    val parts = mutableListOf<String>()
    if (c.mods and 0x11 != 0) parts += if (mac) "⌃" else "Ctrl"
    if (c.mods and 0x44 != 0) parts += if (mac) "⌥" else "Alt"
    if (c.mods and 0x22 != 0) parts += if (mac) "⇧" else "Shift"
    if (c.mods and 0x88 != 0) parts += when (os) { dev.padbridge.HostOs.MAC -> "⌘"; dev.padbridge.HostOs.WINDOWS -> "Win"; else -> "Super" }
    if (c.code != 0) parts += keyName(c.code)
    return parts.joinToString(if (mac) "" else " + ")
}

private fun keyName(code: Int): String = when (code) {
    in 0x04..0x1D -> ('A' + (code - 0x04)).toString()
    in 0x1E..0x26 -> ('1' + (code - 0x1E)).toString()
    0x27 -> "0"
    in 0x3A..0x45 -> "F${code - 0x39}"
    dev.padbridge.Keys.TAB -> "Tab"
    dev.padbridge.Keys.SPACE -> "Space"
    dev.padbridge.Keys.ESC -> "Esc"
    dev.padbridge.Keys.ENTER -> "Enter"
    dev.padbridge.Keys.LEFT -> "←"
    dev.padbridge.Keys.RIGHT -> "→"
    dev.padbridge.Keys.UP -> "↑"
    dev.padbridge.Keys.DOWN -> "↓"
    dev.padbridge.Keys.LBRACKET -> "["
    dev.padbridge.Keys.RBRACKET -> "]"
    dev.padbridge.Keys.EQUAL -> "="
    dev.padbridge.Keys.MINUS -> "−"
    dev.padbridge.Keys.PERIOD -> "."
    dev.padbridge.Keys.PRINT_SCREEN -> "PrtSc"
    dev.padbridge.Keys.BACKSPACE -> "Backspace"
    dev.padbridge.Keys.DELETE -> "Delete"
    dev.padbridge.Keys.HOME -> "Home"
    dev.padbridge.Keys.END -> "End"
    dev.padbridge.Keys.PAGE_UP -> "PgUp"
    dev.padbridge.Keys.PAGE_DOWN -> "PgDn"
    dev.padbridge.Keys.SLASH -> "/"
    dev.padbridge.Keys.COMMA -> ","
    dev.padbridge.Keys.GRAVE -> "`"
    else -> "Key $code"
}


@Composable
private fun AppearanceSettings() {
    var mode by remember { mutableIntStateOf(Settings.themeMode) }
    var palette by remember { mutableIntStateOf(Settings.palette) }
    var black by remember { mutableStateOf(Settings.pureBlack) }
    var seed by remember { mutableIntStateOf(Settings.seedColor) }
    var bg by remember { mutableIntStateOf(Settings.twoToneBg) }
    var fg by remember { mutableIntStateOf(Settings.twoToneFg) }
    var editing by remember { mutableIntStateOf(0) } // two-tone: 0 background, 1 foreground
    val canDynamic = android.os.Build.VERSION.SDK_INT >= 31
    fun changed() { ThemeState.version++ }

    @Composable
    fun Choice(labels: List<String>, selected: Int, enabled: (Int) -> Boolean = { true }, onPick: (Int) -> Unit) =
        Row(horizontalArrangement = Arrangement.spacedBy(androidx.compose.material3.ButtonGroupDefaults.ConnectedSpaceBetween),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            labels.forEachIndexed { i, label ->
                androidx.compose.material3.ToggleButton(
                    checked = selected == i,
                    onCheckedChange = { onPick(i) },
                    enabled = enabled(i),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    shapes = when (i) {
                        0 -> androidx.compose.material3.ButtonGroupDefaults.connectedLeadingButtonShapes()
                        labels.lastIndex -> androidx.compose.material3.ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else -> androidx.compose.material3.ButtonGroupDefaults.connectedMiddleButtonShapes()
                    },
                ) { Text(label, maxLines = 1) }
            }
        }

    val twoTone = palette == 3
    val items = if (twoTone) 3 else 4
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        if (!twoTone) SegmentedListItem(
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(0, items),
            colors = segColors(),
            supportingContent = {
                Column {
                    Choice(listOf("System", "Light", "Dark", "Realistic"), mode) { mode = it; Settings.themeMode = it; changed() }
                    if (mode == 3) Text(
                        "Drawn like real hardware: a silver aluminum keyboard and glass trackpad in light, space black in dark " +
                            "(follows the system).",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            },
        ) { Text("Theme") }
        SegmentedListItem(
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(if (twoTone) 0 else 1, items),
            colors = segColors(),
            supportingContent = {
                Column {
                    Text(when (palette) {
                        1 -> "Colors from your wallpaper."
                        2 -> "Pick one color; the whole palette grows from it, light and dark."
                        3 -> "Just two colors: one for backgrounds, one for text, keys and accents. Light or dark follows the background."
                        else -> "PadBridge's own blue palette."
                    })
                    Choice(listOf("PadBridge", "Wallpaper", "Custom", "Two-tone"), palette, enabled = { it != 1 || canDynamic }) {
                        palette = it; Settings.palette = it; Settings.dynamicColor = it == 1; changed()
                    }
                }
            },
        ) { Text("Colors") }
        if (palette == 2) SegmentedListItem(
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(2, items),
            colors = segColors(),
            supportingContent = {
                ColorPicker(seed, onChange = {}, onDone = { seed = it; Settings.seedColor = it; changed() })
            },
        ) { Text("Seed color") }
        if (twoTone) SegmentedListItem(
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(1, items),
            colors = segColors(),
            supportingContent = {
                Column {
                    Choice(listOf("Background", "Foreground"), editing) { editing = it }
                    val ratio = contrast(bg, fg)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Text(
                            "Contrast ${"%.1f".format(ratio)}:1" + if (ratio < 4.5f) " · low, text may be hard to read" else "",
                            color = if (ratio < 4.5f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            val t = bg; bg = fg; fg = t; Settings.twoToneBg = bg; Settings.twoToneFg = fg; changed()
                        }) { Text("Swap") }
                    }
                    if (editing == 0) ColorPicker(bg, onChange = {}, onDone = { bg = it; Settings.twoToneBg = it; changed() })
                    else ColorPicker(fg, onChange = {}, onDone = { fg = it; Settings.twoToneFg = it; changed() })
                }
            },
        ) { Text("Two-tone colors") }
        if (!twoTone) SegmentedListItem(
            onClick = { black = !black; Settings.pureBlack = black; changed() },
            shapes = ListItemDefaults.segmentedShapes(items - 1, items),
            colors = segColors(),
            supportingContent = { Text("True black in dark mode, easier on OLED screens and battery") },
            trailingContent = { Switch(checked = black, onCheckedChange = { black = it; Settings.pureBlack = it; changed() }) },
        ) { Text("Pure black") }
        else SegmentedListItem(
            onClick = {},
            shapes = ListItemDefaults.segmentedShapes(2, items),
            colors = segColors(),
            supportingContent = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    for ((b, f) in TwoTonePresets) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = { bg = b; fg = f; Settings.twoToneBg = b; Settings.twoToneFg = f; changed() },
                            colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                containerColor = androidx.compose.ui.graphics.Color(b),
                                contentColor = androidx.compose.ui.graphics.Color(f)),
                        ) { Text("Aa") }
                    }
                }
            },
        ) { Text("Presets") }
        var glass by remember { mutableStateOf(Settings.glass) }
        SegmentedListItem(
            onClick = { glass = !glass; Settings.glass = glass; changed() },
            shapes = ListItemDefaults.segmentedShapes(0, 1),
            colors = segColors(),
            modifier = Modifier.padding(top = 12.dp),
            supportingContent = {
                Text("See-through sheets, dialogs and menus" +
                    if (android.os.Build.VERSION.SDK_INT >= 31) ", with the app blurred behind them" else "")
            },
            trailingContent = { Switch(checked = glass, onCheckedChange = { glass = it; Settings.glass = it; changed() }) },
        ) { Text("Blur and transparency") }
    }
}

private val TwoTonePresets = listOf(
    0xFF0B1F33 to 0xFFFFC857, 0xFF000000 to 0xFFFFFFFF, 0xFF000000 to 0xFF39FF14, 0xFFF4EFE6 to 0xFF1F3A5F,
    0xFF1B0B2E to 0xFFFF6AD5, 0xFFFFF4E0 to 0xFFC0392B, 0xFF0F2A1D to 0xFFB8F2C9, 0xFF101010 to 0xFFFF7A50,
).map { (a, b) -> a.toInt() to b.toInt() }

const val REPO_URL = "https://github.com/spundone/padbridge"

@Composable
private fun AboutSection(activity: MainActivity) {
    val ctx = LocalContext.current
    var licenses by remember { mutableStateOf(false) }
    var whatsNew by remember { mutableStateOf(false) }
    fun open(url: String) = try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
    } catch (_: Exception) {}
    val rows = listOf(
        Triple("PadBridge ${activity.versionName()}", "${dev.padbridge.BuildConfig.DIST} build · HID descriptor v${HidDescriptor.VERSION}") { },
        Triple("What's new", "Changes in each version") { whatsNew = true },
        Triple("Check for updates", updateSubtitle(dev.padbridge.Updates.state)) { onUpdateRowClick(activity) },
        Triple("Source code", "github.com/spundone/padbridge") { open(REPO_URL) },
        Triple("Report a problem", "Open an issue on GitHub") { open("$REPO_URL/issues/new") },
        Triple("License", "Apache License 2.0") { open("$REPO_URL/blob/main/LICENSE") },
        Triple("Open-source licenses", "Libraries PadBridge is built with") { licenses = true },
    )
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        rows.forEachIndexed { i, (title, sub, action) ->
            SegmentedListItem(
                onClick = action,
                shapes = ListItemDefaults.segmentedShapes(i, rows.size),
                colors = segColors(),
                supportingContent = { Text(sub) },
            ) { Text(title) }
        }
    }
    if (whatsNew) WhatsNewSheet(activity.versionName().substringBefore("-")) { whatsNew = false }
    if (licenses) androidx.compose.material3.AlertDialog(
        onDismissRequest = { licenses = false },
        containerColor = glassDialogColor(),
        confirmButton = { androidx.compose.material3.TextButton(onClick = { licenses = false }) { Text("Close") } },
        title = { Text("Open-source licenses") },
        text = {
            Text(
                "PadBridge is licensed under the Apache License 2.0 and uses:\n\n" +
                    "• Jetpack Compose, Material 3, AndroidX Activity and Core (Google) — Apache License 2.0\n" +
                    "• Material Icons (Google) — Apache License 2.0\n" +
                    "• Kotlin standard library and coroutines (JetBrains) — Apache License 2.0\n" +
                    "• English word list for keyboard suggestions: from wordfreq by Robyn Speer, whose data is " +
                    "licensed CC BY-SA 4.0 (creativecommons.org/licenses/by-sa/4.0); the list in PadBridge is " +
                    "shared under the same license\n\n" +
                    "Full license texts: apache.org/licenses/LICENSE-2.0"
            )
        },
    )
}


/**
 * Opt-in Windows Precision Touchpad / Linux multitouch mode. Changing it re-registers the HID descriptor,
 * so computers must forget and re-pair the phone once.
 */
@Composable
private fun NativeTouchpadSetting(hid: HidUi) {
    var on by remember { mutableStateOf(Settings.nativeTouchpad) }
    var changed by remember { mutableStateOf(false) }
    val active = HidManager.ptpActive
    SegmentedListItem(
        onClick = {
            on = !on; Settings.nativeTouchpad = on; changed = true
            HidManager.reRegister()
        },
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = segColors(),
        supportingContent = {
            Column {
                Text(
                    "Reports raw touches as a Precision Touchpad, so Windows and Linux (GNOME, KDE) do taps, scrolling, " +
                        "pinch and 3/4-finger gestures themselves, with their own touchpad settings. macOS ignores it and " +
                        "keeps PadBridge's gestures."
                )
                Text(
                    when {
                        active -> "Active: ${hid.hostName ?: "the computer"} is handling gestures."
                        changed -> "Remove this phone on the computer and pair it again so it sees the change."
                        on -> "On. Waiting for a computer to switch it into touchpad mode."
                        else -> "Off. Needs a one-time re-pair after turning on."
                    },
                    color = if (active) MaterialTheme.colorScheme.primary
                    else if (changed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        trailingContent = {
            Switch(checked = on, onCheckedChange = {
                on = it; Settings.nativeTouchpad = it; changed = true; HidManager.reRegister()
            })
        },
    ) { Text("Native touchpad for Windows and Linux") }
}


/** Opt-in N-key rollover: any number of keys at once instead of six. Re-registers the descriptor. */
@Composable
private fun NkroSetting() {
    var on by remember { mutableStateOf(Settings.nkro) }
    var changed by remember { mutableStateOf(false) }
    fun set(v: Boolean) { on = v; Settings.nkro = v; changed = true; HidManager.reRegister() }
    SegmentedListItem(
        onClick = { set(!on) },
        shapes = ListItemDefaults.segmentedShapes(0, 1),
        colors = segColors(),
        supportingContent = {
            Column {
                Text(
                    "A normal Bluetooth keyboard can report 6 keys held at once. N-key rollover reports every key, " +
                        "so big chords and games never drop one. Works on Windows, macOS, Linux, Android and iPad; " +
                        "some TVs only understand 6-key keyboards."
                )
                Text(
                    if (changed) "Remove this phone on the computer and pair it again so it sees the change."
                    else if (on) "On." else "Off. Needs a one-time re-pair after turning on.",
                    color = if (changed) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        trailingContent = { Switch(checked = on, onCheckedChange = { set(it) }) },
    ) { Text("N-key rollover (any number of keys)") }
}

/** Settings are grouped into short pages instead of one long scroll. */
enum class SettingsPage(
    val title: String,
    val subtitle: (dev.padbridge.HostOs) -> String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    GENERAL("General", { "Computer type, haptics, reconnecting, setup again" }, Icons.Rounded.Tune),
    TRACKPAD("Trackpad", { "Speed, scrolling, taps and the native touchpad" }, Icons.Rounded.TouchApp),
    GESTURES("Gestures", { "What each multi-finger gesture does on ${it.label}" }, Icons.Rounded.Gesture),
    KEYBOARD("Keyboard", { "Word suggestions, TrackPoint and key rollover" }, Icons.Rounded.Keyboard),
    MOTION("Air mouse & gamepad", { "Phone motion for the air mouse and gamepad gyro" }, Icons.Rounded.ScreenRotation),
    SCREEN("Screen", { "Keep awake, dim when idle, full screen and Do Not Disturb" }, Icons.Rounded.Fullscreen),
    APPEARANCE("Appearance", { "Theme, colors, realistic look and blur" }, Icons.Rounded.Palette),
    ABOUT("About", { "What's new, updates and the developer" }, Icons.Rounded.Info),
}

@Composable
private fun SettingsCategories(os: dev.padbridge.HostOs, selected: SettingsPage? = null, onOpen: (SettingsPage) -> Unit) {
    val pages = SettingsPage.entries
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap), modifier = Modifier.padding(top = 8.dp)) {
        pages.forEachIndexed { i, p ->
            SegmentedListItem(
                onClick = { onOpen(p) },
                shapes = ListItemDefaults.segmentedShapes(i, pages.size),
                colors = if (p == selected) ListItemDefaults.segmentedColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    supportingContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) else segColors(),
                leadingContent = {
                    androidx.compose.material3.Surface(
                        shape = androidx.compose.foundation.shape.CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(40.dp),
                    ) { androidx.compose.foundation.layout.Box(contentAlignment = androidx.compose.ui.Alignment.Center) { Icon(p.icon, null) } }
                },
                supportingContent = { Text(p.subtitle(os)) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
            ) { Text(p.title, fontWeight = FontWeight.Medium) }
        }
    }
}

/** Every setting by name, with words people might search for, and the page it's on. */
private val SearchIndex: List<Triple<String, SettingsPage, String>> = listOf(
    Triple("Computer type", SettingsPage.GENERAL, "windows mac macos linux os shortcuts"),
    Triple("Haptic feedback", SettingsPage.GENERAL, "vibration vibrate buzz click sound motor"),
    Triple("Reconnect to last computer", SettingsPage.GENERAL, "auto connect startup"),
    Triple("Show all paired devices", SettingsPage.GENERAL, "phones speakers list"),
    Triple("Run setup again", SettingsPage.GENERAL, "wizard onboarding permissions pair"),
    Triple("Root mode", SettingsPage.GENERAL, "experimental speaker device class controller identity xbox"),
    Triple("Pointer speed", SettingsPage.TRACKPAD, "cursor mouse sensitivity fast slow"),
    Triple("Scroll speed", SettingsPage.TRACKPAD, "scrolling wheel"),
    Triple("Natural scrolling", SettingsPage.TRACKPAD, "reverse invert direction"),
    Triple("Scroll momentum", SettingsPage.TRACKPAD, "inertia flick"),
    Triple("Scroll rails", SettingsPage.TRACKPAD, "lock axis"),
    Triple("Edge scroll strips", SettingsPage.TRACKPAD, "side edge"),
    Triple("Two-finger swipe for back / forward", SettingsPage.TRACKPAD, "navigate history"),
    Triple("Tap to click", SettingsPage.TRACKPAD, "tap click"),
    Triple("Double-tap window", SettingsPage.TRACKPAD, "double click timing"),
    Triple("Ignore resting fingers", SettingsPage.TRACKPAD, "dex thumb rest"),
    Triple("Palm rejection", SettingsPage.TRACKPAD, "palm typing accidental"),
    Triple("Show click buttons", SettingsPage.TRACKPAD, "left right buttons"),
    Triple("S Pen and stylus", SettingsPage.TRACKPAD, "pen hover spen"),
    Triple("Native touchpad for Windows and Linux", SettingsPage.TRACKPAD, "precision touchpad ptp raw"),
    Triple("Three-finger drag", SettingsPage.TRACKPAD, "drag windows select"),
    Triple("Gestures", SettingsPage.GESTURES, "three four finger swipe tap pinch dex shortcuts"),
    Triple("Word suggestions", SettingsPage.KEYBOARD, "predict prediction autocomplete spell"),
    Triple("Suggestions learn your words", SettingsPage.KEYBOARD, "learn forget dictionary"),
    Triple("Phone keyboard learns from typing", SettingsPage.KEYBOARD, "gboard incognito"),
    Triple("TrackPoint", SettingsPage.KEYBOARD, "nub nipple thinkpad sensitivity"),
    Triple("N-key rollover", SettingsPage.KEYBOARD, "nkro keys games chords"),
    Triple("Air mouse speed", SettingsPage.MOTION, "gyro motion pointer"),
    Triple("Air mouse always aims", SettingsPage.MOTION, "gyro"),
    Triple("Gamepad gyro sensitivity", SettingsPage.MOTION, "controller motion aim"),
    Triple("Keep screen on while connected", SettingsPage.SCREEN, "awake sleep timeout"),
    Triple("Dim to outline mode when idle", SettingsPage.SCREEN, "aod always on display dim battery"),
    Triple("Hide app bars on input screens", SettingsPage.SCREEN, "full screen fullscreen immersive"),
    Triple("Do Not Disturb in full screen", SettingsPage.SCREEN, "dnd notifications silence"),
    Triple("Theme", SettingsPage.APPEARANCE, "dark light realistic skeuomorphic"),
    Triple("Colors", SettingsPage.APPEARANCE, "palette wallpaper dynamic custom two-tone accent"),
    Triple("Pure black", SettingsPage.APPEARANCE, "oled amoled dark mode"),
    Triple("Blur and transparency", SettingsPage.APPEARANCE, "glass"),
    Triple("Check for updates", SettingsPage.ABOUT, "update version github"),
    Triple("What's new", SettingsPage.ABOUT, "changelog"),
    Triple("Report a problem", SettingsPage.ABOUT, "bug issue help"),
    Triple("Licenses", SettingsPage.ABOUT, "open source"),
    Triple("Stop PadBridge", SettingsPage.ABOUT, "quit exit stop service"),
)

@Composable
private fun SettingsSearch(query: String, onChange: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query, onValueChange = onChange,
        placeholder = { Text("Search settings") },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = if (query.isNotEmpty()) ({
            IconButton(onClick = { onChange("") }) { Icon(Icons.Rounded.Close, "Clear") }
        }) else null,
        singleLine = true,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}

@Composable
private fun SearchResults(query: String, onOpen: (SettingsPage) -> Unit) {
    val words = query.lowercase().split(' ').filter { it.isNotBlank() }
    val hits = SearchIndex.filter { (title, page, keys) ->
        val hay = "$title ${page.title} $keys".lowercase()
        words.all { it in hay }
    }
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap), modifier = Modifier.padding(top = 8.dp)) {
        if (hits.isEmpty()) Text(
            "Nothing matches “$query”.", color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
        hits.forEachIndexed { i, (title, page, _) ->
            SegmentedListItem(
                onClick = { onOpen(page) },
                shapes = ListItemDefaults.segmentedShapes(i, hits.size),
                colors = segColors(),
                leadingContent = { Icon(page.icon, null) },
                supportingContent = { Text(page.title) },
                trailingContent = { Icon(Icons.Rounded.ChevronRight, null) },
            ) { Text(title) }
        }
    }
}
