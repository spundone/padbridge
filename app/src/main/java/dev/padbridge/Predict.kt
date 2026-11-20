package dev.padbridge

import android.content.Context
import android.view.textservice.SentenceSuggestionsInfo
import android.view.textservice.SpellCheckerSession
import android.view.textservice.SuggestionsInfo
import android.view.textservice.TextInfo
import android.view.textservice.TextServicesManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Word suggestions for PadBridge's own keyboard. It follows the word being typed (from the keys it sends) and
 * offers completions and corrections from:
 *
 * - the phone's spell checker (Gboard's on most phones; its dictionaries, your personal dictionary included), and
 * - optionally, words you've typed on this keyboard ([Settings.kbPredictLearn], off by default, stored only on the
 *   phone: anything typed here, passwords too, would otherwise be remembered).
 *
 * Picking one deletes the partial word on the computer and types the suggestion and a space.
 */
object Predict {
    /** The word being typed, as typed. */
    var word by mutableStateOf(""); private set
    val suggestions = mutableStateListOf<String>()

    private var session: SpellCheckerSession? = null
    private var asked = ""
    private lateinit var app: Context

    fun init(ctx: Context) {
        if (::app.isInitialized) return
        app = ctx.applicationContext
        Thread { loadWords() }.start()
    }

    // ---------------------------------------------------------------- bundled word list

    /** The 40,000 most common English words, most common first (wordfreq, CC BY-SA 4.0). */
    @Volatile private var words: Array<String> = emptyArray()
    /** Indexes into [words], sorted alphabetically, for prefix lookups. */
    @Volatile private var sorted: IntArray = IntArray(0)

    private fun loadWords() {
        try {
            val w = app.assets.open("words_en.txt").bufferedReader().readLines().filter { it.isNotBlank() }.toTypedArray()
            val idx = w.indices.sortedWith { a, b -> w[a].compareTo(w[b]) }.toIntArray()
            words = w; sorted = idx
        } catch (_: Exception) {}
    }

    /** The [n] most common words that start with [prefix] (lowercase) and are longer than it. */
    private fun completions(prefix: String, n: Int): List<String> {
        val w = words; val s = sorted
        if (w.isEmpty() || prefix.isEmpty()) return emptyList()
        // Binary search the first word >= prefix, then walk the run of words sharing the prefix.
        var lo = 0; var hi = s.size
        while (lo < hi) { val mid = (lo + hi) ushr 1; if (w[s[mid]] < prefix) lo = mid + 1 else hi = mid }
        val best = java.util.PriorityQueue<Int>(compareByDescending { it })
        var i = lo
        while (i < s.size && w[s[i]].startsWith(prefix)) {
            val r = s[i]
            if (w[r].length > prefix.length) { best.add(r); if (best.size > n) best.poll() }
            i++
        }
        return best.sorted().map { w[it] }
    }


    private fun session(): SpellCheckerSession? {
        session?.let { return it }
        if (!::app.isInitialized) return null
        val tsm = app.getSystemService(TextServicesManager::class.java) ?: return null
        session = try {
            @Suppress("DEPRECATION")
            tsm.newSpellCheckerSession(null, Locale.getDefault(), listener, true)
        } catch (_: Exception) { null }
        return session
    }

    private val listener = object : SpellCheckerSession.SpellCheckerSessionListener {
        override fun onGetSuggestions(results: Array<out SuggestionsInfo>?) {}
        override fun onGetSentenceSuggestions(results: Array<out SentenceSuggestionsInfo>?) {
            val found = ArrayList<String>()
            results?.forEach { s ->
                for (i in 0 until s.suggestionsCount) {
                    val info = s.getSuggestionsInfoAt(i) ?: continue
                    for (j in 0 until info.suggestionsCount) info.getSuggestionAt(j)?.let { found += it }
                }
            }
            if (asked == word) show(found)
        }
    }

    // ---------------------------------------------------------------- following the typing

    /** A key the built-in keyboard sent (HID usage, modifiers, Caps Lock on). */
    fun key(usage: Int, mods: Int, caps: Boolean) {
        if (!Settings.kbPredict) return
        val shortcut = mods and (Keys.L_CTRL or Keys.L_ALT or Keys.L_GUI or 0x10 or 0x40 or 0x80) != 0
        when {
            shortcut -> reset()
            usage == Keys.BACKSPACE -> if (word.isNotEmpty()) { word = word.dropLast(1); lookup() } else reset()
            else -> {
                val c = Local.charFor(usage, mods)
                when {
                    c == null -> { learn(); reset() } // Enter, Tab, arrows, Esc: the word ends or the cursor moved
                    c.isLetter() || c == '\'' -> {
                        val ch = if (caps && c.isLetter()) (if (mods and 0x22 != 0) c.lowercaseChar() else c.uppercaseChar()) else c
                        word += ch; lookup()
                    }
                    else -> { learn(); reset() } // space, punctuation, digits end the word
                }
            }
        }
    }

    /** Clicking or moving the cursor elsewhere: forget the word (we no longer know what's before the cursor). */
    fun reset() {
        word = ""; asked = ""; suggestions.clear()
    }

    private fun lookup() {
        if (word.isEmpty()) { suggestions.clear(); return }
        asked = word
        // Learned words first (instant), then the spell checker's answer replaces / extends them.
        show(emptyList())
        if (word.length >= 2) try {
            session()?.getSentenceSuggestions(arrayOf(TextInfo(word)), 5)
        } catch (_: Exception) {}
    }

    private fun show(fromChecker: List<String>) {
        val prefix = word.lowercase()
        val learned = learnedWords().filter { it.length > prefix.length && it.startsWith(prefix) }.take(3)
        // Typo (no word starts like this): the spell checker's corrections lead; otherwise completions do.
        val complete = completions(prefix, 3)
        val ordered = if (complete.isEmpty()) learned + fromChecker else learned + complete + fromChecker
        val all = LinkedHashSet<String>()
        ordered.forEach { s ->
            if (s.isNotBlank() && !s.equals(word, ignoreCase = true) && ' ' !in s) all += matchCase(s)
        }
        suggestions.clear()
        suggestions.addAll(all.take(3))
    }

    /** Keep the user's capitalization: "Hel" -> "Hello", "HEL" -> "HELLO". */
    private fun matchCase(s: String): String = when {
        s == "i" || s.startsWith("i'") -> s.replaceFirstChar { 'I' }
        word.length > 1 && word.all { !it.isLetter() || it.isUpperCase() } -> s.uppercase()
        word.firstOrNull()?.isUpperCase() == true -> s.replaceFirstChar { it.uppercaseChar() }
        else -> s
    }

    /** Replaces the partial word on the computer with [s] and a space. */
    fun pick(s: String) {
        val n = word.length
        if (n > 0) HidManager.backspace(n)
        HidManager.type("$s ")
        bump(s)
        reset()
        Haptics.soft(0.4f)
    }

    // ---------------------------------------------------------------- learned words (opt-in)

    private const val MAX_WORDS = 3000
    private var cache: MutableMap<String, Int>? = null

    private fun counts(): MutableMap<String, Int> {
        cache?.let { return it }
        val m = HashMap<String, Int>()
        Settings.kbLearnedWords.split('\n').forEach { line ->
            val i = line.lastIndexOf('\t')
            if (i > 0) m[line.substring(0, i)] = line.substring(i + 1).toIntOrNull() ?: 1
        }
        cache = m
        return m
    }

    private fun learnedWords(): List<String> =
        if (!Settings.kbPredictLearn) emptyList() else counts().entries.sortedByDescending { it.value }.map { it.key }

    private fun learn() { if (word.length >= 3) bump(word) }

    private fun bump(w: String) {
        if (!Settings.kbPredictLearn) return
        val m = counts()
        val k = w.lowercase()
        m[k] = (m[k] ?: 0) + 1
        if (m.size > MAX_WORDS) m.entries.minByOrNull { it.value }?.let { m.remove(it.key) }
        Settings.kbLearnedWords = m.entries.joinToString("\n") { "${it.key}\t${it.value}" }
    }

    /** Forget every learned word. */
    fun forget() { cache = HashMap(); Settings.kbLearnedWords = "" }
}
