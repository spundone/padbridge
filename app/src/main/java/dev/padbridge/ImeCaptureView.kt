package dev.padbridge

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.BaseInputConnection
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager

/**
 * An invisible-ish "text field" that opens the phone's own keyboard (Gboard, Samsung, SwiftKey…)
 * and forwards everything typed as HID keystrokes instead of keeping it.
 *
 * Live mode disables suggestions so each key goes out immediately. Predictive mode lets the IME
 * compose words (glide typing, autocorrect); corrections are replayed with backspaces.
 */
class ImeCaptureView(ctx: Context) : View(ctx) {

    /** Called with recently typed text, for an on-screen echo. */
    var onEcho: ((String) -> Unit)? = null
    var onSkipped: ((Int) -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val imm = ctx.getSystemService(InputMethodManager::class.java)
    private val echo = StringBuilder()

    private val bg = Paint(Paint.ANTI_ALIAS_FLAG)
    private val txt = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 15 * density }
    private val caret = Paint().apply { color = C.ACCENT; strokeWidth = 2 * density }
    private val rect = RectF()

    init {
        isFocusable = true
        isFocusableInTouchMode = true
    }

    fun showKeyboard() {
        requestFocus()
        imm.restartInput(this)
        imm.showSoftInput(this, 0)
    }

    fun hideKeyboard() {
        imm.hideSoftInputFromWindow(windowToken, 0)
        clearFocus()
    }

    /** Re-reads the typing mode setting. */
    fun refreshMode() {
        if (hasFocus()) imm.restartInput(this)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (e.action == MotionEvent.ACTION_UP) showKeyboard()
        return true
    }

    override fun onCheckIsTextEditor() = true

    override fun onCreateInputConnection(out: EditorInfo): InputConnection {
        out.inputType = if (Settings.predictiveTyping) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_AUTO_CORRECT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        // NO_PERSONALIZED_LEARNING: text typed to the computer shouldn't train the phone keyboard.
        out.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI or EditorInfo.IME_FLAG_NO_FULLSCREEN or
            EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_ENTER_ACTION or
            EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        out.initialSelStart = 0
        out.initialSelEnd = 0
        return Conn()
    }

    private fun echoAppend(s: String) {
        echo.append(s)
        if (echo.length > 200) echo.delete(0, echo.length - 200)
        onEcho?.invoke(echo.toString()); invalidate()
    }

    private fun echoBackspace(n: Int) {
        repeat(n) { if (echo.isNotEmpty()) echo.setLength(echo.length - 1) }
        onEcho?.invoke(echo.toString()); invalidate()
    }

    private fun typeOut(s: String) {
        if (s.isEmpty()) return
        val skipped = HidManager.type(s)
        if (skipped > 0) onSkipped?.invoke(skipped)
        echoAppend(s)
    }

    private fun backspace(n: Int) {
        if (n <= 0) return
        HidManager.backspace(n)
        echoBackspace(n)
    }

    /** Hardware keyboards attached to the phone also end up here. */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean =
        if (handleKey(event)) true else super.onKeyDown(keyCode, event)

    private fun handleKey(e: KeyEvent): Boolean {
        val code = e.keyCode
        if (code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_VOLUME_UP ||
            code == KeyEvent.KEYCODE_VOLUME_DOWN
        ) return false
        val mods = Keys.modsFromAndroidMeta(e.metaState)
        when (code) {
            KeyEvent.KEYCODE_DEL -> { HidManager.tap(Keys.BACKSPACE, mods); echoBackspace(1); return true }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                HidManager.tap(Keys.ENTER, mods); echoAppend("\n"); return true
            }
            KeyEvent.KEYCODE_ESCAPE -> { HidManager.tap(Keys.ESC, mods); return true }
        }
        val ch = e.unicodeChar
        val ctrlLike = mods and (Keys.L_CTRL or Keys.L_ALT or Keys.L_GUI) != 0
        if (ch != 0 && !ctrlLike && code != KeyEvent.KEYCODE_TAB) {
            typeOut(String(Character.toChars(ch)))
            return true
        }
        val hid = Keys.fromAndroidKeyCode(code) ?: return false
        HidManager.tap(hid, mods)
        return true
    }

    private inner class Conn : BaseInputConnection(this, false) {
        private var composing = ""

        private fun replaceComposing(new: String) {
            val old = composing
            var p = 0
            while (p < old.length && p < new.length && old[p] == new[p]) p++
            backspace(old.length - p)
            typeOut(new.substring(p))
        }

        override fun commitText(text: CharSequence, newCursorPosition: Int): Boolean {
            replaceComposing(text.toString())
            composing = ""
            return true
        }

        override fun setComposingText(text: CharSequence, newCursorPosition: Int): Boolean {
            replaceComposing(text.toString())
            composing = text.toString()
            return true
        }

        override fun finishComposingText(): Boolean { composing = ""; return true }
        override fun setComposingRegion(start: Int, end: Int) = true

        override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
            if (composing.isNotEmpty() && beforeLength > 0) {
                val n = minOf(beforeLength, composing.length)
                composing = composing.dropLast(n)
            }
            backspace(beforeLength)
            repeat(afterLength) { HidManager.tap(Keys.DELETE) }
            return true
        }

        override fun deleteSurroundingTextInCodePoints(beforeLength: Int, afterLength: Int) =
            deleteSurroundingText(beforeLength, afterLength)

        override fun sendKeyEvent(event: KeyEvent): Boolean {
            if (event.action == KeyEvent.ACTION_DOWN) handleKey(event)
            return true
        }

        override fun performEditorAction(actionCode: Int): Boolean {
            HidManager.tap(Keys.ENTER); echoAppend("\n"); return true
        }

        // The IME sees an empty field (apart from the word being composed), so it never
        // tries to edit text we have already sent.
        override fun getTextBeforeCursor(n: Int, flags: Int): CharSequence = composing.takeLast(n)
        override fun getTextAfterCursor(n: Int, flags: Int): CharSequence = ""
        override fun getSelectedText(flags: Int): CharSequence? = null
        override fun getCursorCapsMode(reqModes: Int) = 0
    }

    override fun onDraw(c: Canvas) {
        val r = height / 2f
        caret.color = C.ACCENT
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        bg.color = if (hasFocus()) C.SURFACE3 else C.SURFACE2
        c.drawRoundRect(rect, r, r, bg)
        val pad = 14 * density
        val baseline = height / 2f + 5 * density
        val shown = echo.toString().substringAfterLast('\n')
        if (shown.isEmpty() && !hasFocus()) {
            txt.color = C.MUTED
            c.drawText("Type with phone keyboard", pad, baseline, txt)
            return
        }
        txt.color = C.TEXT
        val maxW = width - 2 * pad - 4 * density
        var s = shown
        while (s.isNotEmpty() && txt.measureText(s) > maxW) s = s.substring(1)
        c.drawText(s, pad, baseline, txt)
        if (hasFocus()) {
            val x = pad + txt.measureText(s) + 2 * density
            c.drawLine(x, height / 2f - 10 * density, x, height / 2f + 10 * density, caret)
        }
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: android.graphics.Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        invalidate()
    }
}
