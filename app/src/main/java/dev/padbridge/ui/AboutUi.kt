package dev.padbridge.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.padbridge.BuildConfig

const val UPI_ID = "spandan-jayanta@axisb"
private const val DEV_NAME = "Spandan"
private const val GITHUB_PROFILE = "https://github.com/spundone"

// ------------------------------------------------------------------ about the developer

/** "About the developer" card: who makes PadBridge, plus a UPI donation on the GitHub build. */
@Composable
fun AboutDeveloper() {
    val ctx = LocalContext.current
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(
                    Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(DEV_NAME.take(1), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimary)
                }
                Column {
                    Text("Made by $DEV_NAME", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("@spundone on GitHub", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                }
            }
            Text(
                "Hi! I build PadBridge in my spare time, to turn the phone in your pocket into the trackpad, keyboard " +
                    "and remote you forgot at home. It's free and open source, with no ads, no accounts and no tracking." +
                    if (BuildConfig.DONATE) " If it's useful to you, a small donation keeps it going." else "",
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (BuildConfig.DONATE) {
                    Button(onClick = { donate(ctx) }) {
                        Icon(Icons.Rounded.Favorite, null, Modifier.size(18.dp))
                        Text("  Donate with UPI")
                    }
                }
                OutlinedButton(onClick = { open(ctx, GITHUB_PROFILE) }) {
                    Icon(Icons.Rounded.Code, null, Modifier.size(18.dp))
                    Text("  GitHub")
                }
            }
            if (BuildConfig.DONATE) {
                TextButton(onClick = { copyUpi(ctx) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Icon(Icons.Rounded.ContentCopy, null, Modifier.size(16.dp))
                    Text("  UPI ID: $UPI_ID", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

private fun open(ctx: Context, url: String) = try {
    ctx.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url)))
} catch (_: Exception) {}

/** Opens the UPI app chooser with the payee filled in; the amount is left to the donor. */
private fun donate(ctx: Context) {
    val uri = android.net.Uri.Builder().scheme("upi").authority("pay")
        .appendQueryParameter("pa", UPI_ID)
        .appendQueryParameter("pn", DEV_NAME)
        .appendQueryParameter("tn", "Support PadBridge")
        .appendQueryParameter("cu", "INR")
        .build()
    val pay = Intent(Intent.ACTION_VIEW, uri)
    // Needs the <queries> entry for upi:// in the manifest to see UPI apps (Android 11+).
    if (ctx.packageManager.queryIntentActivities(pay, 0).isEmpty()) {
        copyUpi(ctx, "No UPI app found. UPI ID copied"); return
    }
    try {
        ctx.startActivity(Intent.createChooser(pay, "Donate with"))
    } catch (_: ActivityNotFoundException) {
        copyUpi(ctx, "No UPI app found. UPI ID copied")
    }
}

private fun copyUpi(ctx: Context, message: String = "UPI ID copied") {
    ctx.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("UPI ID", UPI_ID))
    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
}

// ------------------------------------------------------------------ changelog

private class Release(val version: String, val date: String?, val sections: List<Pair<String, List<String>>>)

/** Parses the bundled Keep a Changelog file: "## [1.3.1] – date", "### Added", "- item" (with wrapped lines). */
private fun parseChangelog(text: String): List<Release> {
    val out = ArrayList<Release>()
    var version: String? = null; var date: String? = null
    var sections = ArrayList<Pair<String, MutableList<String>>>()
    fun flush() { version?.let { out += Release(it, date, sections.map { (h, l) -> h to l.toList() }) } }
    for (raw in text.lines()) {
        val line = raw.trimEnd()
        when {
            line.startsWith("## ") -> {
                flush()
                val m = Regex("""##\s*\[?([^\]\s]+)]?\s*(?:[–-]\s*(.+))?""").find(line)
                version = m?.groupValues?.get(1) ?: line.removePrefix("## ")
                date = m?.groupValues?.get(2)?.takeIf { it.isNotBlank() }
                sections = ArrayList()
            }
            version == null -> {}
            line.startsWith("### ") -> sections += line.removePrefix("### ").trim() to mutableListOf()
            line.trimStart().startsWith("- ") -> {
                if (sections.isEmpty()) sections += "" to mutableListOf()
                sections.last().second += line.trimStart().removePrefix("- ")
            }
            line.isNotBlank() && sections.isNotEmpty() && sections.last().second.isNotEmpty() -> {
                val items = sections.last().second
                items[items.lastIndex] = items.last() + " " + line.trim()
            }
        }
    }
    flush()
    return out
}

/** `code`, **bold** and [text](link) → styled text without the markdown. */
@Composable
private fun inline(s: String): AnnotatedString {
    val codeBg = MaterialTheme.colorScheme.surfaceContainerHighest
    return remember(s, codeBg) {
        val noLinks = s.replace(Regex("""\[([^\]]+)]\([^)]*\)"""), "$1")
        buildAnnotatedString {
            val token = Regex("""`([^`]+)`|\*\*([^*]+)\*\*""")
            var at = 0
            for (m in token.findAll(noLinks)) {
                append(noLinks.substring(at, m.range.first))
                if (m.groupValues[1].isNotEmpty()) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) { append(m.groupValues[1]) }
                } else {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(m.groupValues[2]) }
                }
                at = m.range.last + 1
            }
            append(noLinks.substring(at))
        }
    }
}

/** "What's new": the changelog bundled with the app, newest first, with the installed version marked. */
@Composable
fun WhatsNewSheet(installed: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val releases = remember {
        runCatching { ctx.assets.open("CHANGELOG.md").bufferedReader().use { it.readText() } }
            .map(::parseChangelog).getOrDefault(emptyList())
            .filter { BuildConfig.DEBUG || !it.version.equals("Unreleased", ignoreCase = true) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            item { Text("What's new", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 8.dp)) }
            if (releases.isEmpty()) item { Text("The changelog isn't available in this build.") }
            items(releases, key = { it.version }) { r ->
                Column(Modifier.padding(top = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(r.version, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        if (r.version == installed) {
                            Text(
                                "Installed",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                    r.date?.let {
                        Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    for ((heading, list) in r.sections) {
                        if (heading.isNotEmpty()) Text(
                            heading, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                        for (item in list) Row(Modifier.padding(vertical = 2.dp)) {
                            Text("•  ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(inline(item), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
