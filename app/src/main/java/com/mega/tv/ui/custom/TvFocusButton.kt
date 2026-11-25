package com.mega.tv.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import androidx.appcompat.widget.AppCompatButton
import com.mega.tv.ui.theme.ThemeManager
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils

/**
 * Universal Premium Button supporting both:
 * - Mobile Touch: Single-tap response (isFocusableInTouchMode = false), soft press scale & ripple.
 * - Android TV D-Pad: Glowing focus border (theme_color / white), scale 1.06x, shadow elevation.
 */
class TvFocusButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.buttonStyle
) : AppCompatButton(context, attrs, defStyleAttr) {

    private var cornerRadiusDp = 10f
    var isSecondaryButton: Boolean = false
    private val isTvMode: Boolean = DeviceUtils.isTv(context)

    init {
        isFocusable = true
        isFocusableInTouchMode = false
        isClickable = true
        gravity = Gravity.CENTER
        includeFontPadding = false
        setTextColor(Color.WHITE)
        textSize = 15f
        isAllCaps = false
        elevation = 4f
        stateListAnimator = null
        minWidth = 0
        minimumWidth = 0
        minHeight = 0
        minimumHeight = 0
        setPadding(0, 0, 0, 0)

        updateBackground(isFocused)
    }

    fun setCornerRadiusDp(radius: Float) {
        cornerRadiusDp = radius
        updateBackground(isFocused)
    }

    fun setSecondary(secondary: Boolean) {
        isSecondaryButton = secondary
        updateBackground(isFocused)
    }

    fun updateBackground(focused: Boolean = isFocused) {
        val density = context.resources.displayMetrics.density
        val radiusPx = cornerRadiusDp * density
        val primaryColor = ThemeManager.getPrimaryColor(context)

        val darkerPrimary = Color.argb(
            Color.alpha(primaryColor),
            (Color.red(primaryColor) * 0.78f).toInt(),
            (Color.green(primaryColor) * 0.78f).toInt(),
            (Color.blue(primaryColor) * 0.78f).toInt()
        )

        val contentDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            if (isSecondaryButton) {
                setColor(if (focused) Color.parseColor("#3D32384B") else Color.parseColor("#2A222736"))
                if (focused) {
                    setStroke((2.5f * density).toInt(), primaryColor)
                } else {
                    setStroke((1.5f * density).toInt(), Color.parseColor("#55FFFFFF"))
                }
            } else {
                orientation = GradientDrawable.Orientation.TL_BR
                colors = intArrayOf(primaryColor, darkerPrimary)
                if (focused) {
                    setStroke((2.5f * density).toInt(), Color.WHITE)
                }
            }
        }

        val rippleColor = ColorStateList.valueOf(Color.parseColor("#44FFFFFF"))
        background = RippleDrawable(rippleColor, contentDrawable, null)
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        updateBackground(focused)
        if (isTvMode || focused) {
            AnimUtils.animateFocusChange(this, focused, scale = 1.06f, elevationFocused = 14f)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> animate().scaleX(0.96f).scaleY(0.96f).setDuration(90).start()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animate().scaleX(1.0f).scaleY(1.0f).setDuration(110).start()
        }
        return super.onTouchEvent(event)
    }
}
