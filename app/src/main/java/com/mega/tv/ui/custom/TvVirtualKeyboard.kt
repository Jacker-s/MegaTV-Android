package com.mega.tv.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.widget.EditText
import android.widget.LinearLayout

/**
 * Responsive Virtual on-screen keyboard designed for Android TV D-Pad navigation
 * across Login, Register, and Search screens.
 * Uses weighted layout distribution so all 10 keys per row fit with 100% precision
 * without clipping on any TV screen resolution.
 */
class TvVirtualKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var targetEditText: EditText? = null
    var onDoneClickListener: (() -> Unit)? = null
    private var isCaps: Boolean = false

    private val keyButtons = mutableListOf<TvFocusButton>()

    private val rawRows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "@"),
        listOf("z", "x", "c", "v", "b", "n", "m", ".", "_", "-"),
        listOf("CAPS", "ESPAÇO", "APAGAR", "CONCLUIR")
    )

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        clipChildren = false
        clipToPadding = false
        buildKeyboard()
    }

    fun attachEditText(editText: EditText) {
        this.targetEditText = editText
    }

    private fun buildKeyboard() {
        removeAllViews()
        keyButtons.clear()
        val density = context.resources.displayMetrics.density

        for (row in rawRows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                clipChildren = false
                clipToPadding = false
                val rowParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                rowParams.bottomMargin = (5 * density).toInt()
                layoutParams = rowParams
            }

            for (keyText in row) {
                val weight = when (keyText) {
                    "CAPS" -> 1.8f
                    "ESPAÇO" -> 3.6f
                    "APAGAR" -> 2.3f
                    "CONCLUIR" -> 2.3f
                    else -> 1.0f
                }

                val button = TvFocusButton(context).apply {
                    val display = getDisplayText(keyText)
                    text = display
                    isFocusable = true
                    isFocusableInTouchMode = false
                    setCornerRadiusDp(8f)

                    textSize = when {
                        keyText.length > 5 -> 11f
                        keyText.length > 3 -> 12f
                        else -> 14f
                    }
                    setSecondary(keyText != "CONCLUIR" && keyText != "CAPS")

                    val btnParams = LayoutParams(0, (38 * density).toInt(), weight).apply {
                        marginStart = (2 * density).toInt()
                        marginEnd = (2 * density).toInt()
                    }
                    layoutParams = btnParams

                    setOnClickListener { handleKeyPress(keyText) }

                    setOnKeyListener { _, keyCode, event ->
                        if (event.action == KeyEvent.ACTION_DOWN &&
                            (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                        ) {
                            handleKeyPress(keyText)
                            true
                        } else {
                            false
                        }
                    }
                }
                keyButtons.add(button)
                rowLayout.addView(button)
            }
            addView(rowLayout)
        }
    }

    private fun getDisplayText(key: String): String {
        return if (isCaps && key.length == 1 && key[0].isLetter()) {
            key.uppercase()
        } else {
            key
        }
    }

    private fun updateKeyLabels() {
        var idx = 0
        for (row in rawRows) {
            for (key in row) {
                if (idx < keyButtons.size) {
                    val btn = keyButtons[idx]
                    btn.text = getDisplayText(key)
                    if (key == "CAPS") {
                        btn.setSecondary(!isCaps)
                    }
                }
                idx++
            }
        }
    }

    private fun handleKeyPress(key: String) {
        val target = targetEditText ?: return
        when (key) {
            "CAPS" -> {
                isCaps = !isCaps
                updateKeyLabels()
            }
            "ESPAÇO" -> {
                val cursor = target.selectionStart.coerceAtLeast(0)
                target.text.insert(cursor, " ")
            }
            "APAGAR" -> {
                val cursor = target.selectionStart
                if (cursor > 0) {
                    target.text.delete(cursor - 1, cursor)
                } else if (target.text.isNotEmpty()) {
                    target.text.delete(target.text.length - 1, target.text.length)
                }
            }
            "CONCLUIR" -> {
                onDoneClickListener?.invoke()
            }
            else -> {
                val cursor = target.selectionStart.coerceAtLeast(0)
                val toInsert = if (isCaps && key.length == 1 && key[0].isLetter()) {
                    key.uppercase()
                } else {
                    key
                }
                target.text.insert(cursor, toInsert)
            }
        }
    }
}
