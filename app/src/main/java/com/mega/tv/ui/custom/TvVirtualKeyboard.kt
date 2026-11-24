package com.mega.tv.ui.custom

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.KeyEvent
import android.widget.EditText
import android.widget.LinearLayout

/**
 * Virtual on-screen keyboard designed for Android TV D-Pad navigation
 * across Login, Register, and Search screens.
 */
class TvVirtualKeyboard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var targetEditText: EditText? = null
    var onDoneClickListener: (() -> Unit)? = null

    private val rows = listOf(
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l", "@"),
        listOf("z", "x", "c", "v", "b", "n", "m", ".", "_", "-"),
        listOf("ESPAÇO", "APAGAR", "CONCLUIR")
    )

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        buildKeyboard()
    }

    fun attachEditText(editText: EditText) {
        this.targetEditText = editText
    }

    private fun buildKeyboard() {
        removeAllViews()
        val density = context.resources.displayMetrics.density

        for (row in rows) {
            val rowLayout = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER
                val rowParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
                rowParams.bottomMargin = (5 * density).toInt()
                layoutParams = rowParams
            }

            for (keyText in row) {
                val button = TvFocusButton(context).apply {
                    text = keyText
                    isFocusable = true
                    isFocusableInTouchMode = false
                    textSize = if (keyText.length > 1) 12f else 15f
                    setSecondary(keyText != "CONCLUIR")

                    val btnParams = if (keyText == "ESPAÇO" || keyText == "APAGAR" || keyText == "CONCLUIR") {
                        LayoutParams((104 * density).toInt(), (42 * density).toInt())
                    } else {
                        LayoutParams((40 * density).toInt(), (42 * density).toInt())
                    }
                    btnParams.marginEnd = (3 * density).toInt()
                    btnParams.marginStart = (3 * density).toInt()
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
                rowLayout.addView(button)
            }
            addView(rowLayout)
        }
    }

    private fun handleKeyPress(key: String) {
        val target = targetEditText ?: return
        when (key) {
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
                target.text.insert(cursor, key)
            }
        }
    }
}
