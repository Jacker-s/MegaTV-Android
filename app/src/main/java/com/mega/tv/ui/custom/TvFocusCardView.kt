package com.mega.tv.ui.custom

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import com.mega.tv.ui.theme.ThemeManager
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils

/**
 * Universal Premium CardView supporting both Mobile Touch and Android TV D-Pad:
 * - Crisp 12dp rounded clipping for child posters/images
 * - Subtle dark border when idle, smooth press animation & ripple on mobile touch
 * - Thick highlighted theme_color border + 1.07x scale + elevation lift on TV D-Pad focus
 */
class TvFocusCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cornerRadiusDp = 12f
    private val isTvMode: Boolean = DeviceUtils.isTv(context)

    init {
        isFocusable = true
        isFocusableInTouchMode = false
        isClickable = true
        clipChildren = true
        clipToPadding = true

        val density = context.resources.displayMetrics.density
        val radiusPx = cornerRadiusDp * density

        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radiusPx)
            }
        }
        clipToOutline = true

        updateCardAppearance(false)
    }

    private fun updateCardAppearance(focused: Boolean) {
        val density = context.resources.displayMetrics.density
        val radiusPx = cornerRadiusDp * density
        val primaryColor = ThemeManager.getPrimaryColor(context)

        val bgDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.parseColor("#161923"))
            setStroke((1f * density).toInt(), Color.parseColor("#2A3042"))
        }

        val rippleColor = ColorStateList.valueOf(Color.parseColor("#33FFFFFF"))
        val maskDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(Color.WHITE)
        }

        val focusOverlay = if (focused) {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radiusPx
                setColor(Color.TRANSPARENT)
                setStroke((3.5f * density).toInt(), primaryColor)
            }
        } else {
            null
        }

        background = bgDrawable
        foreground = if (focusOverlay != null) {
            focusOverlay
        } else {
            RippleDrawable(rippleColor, null, maskDrawable)
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        updateCardAppearance(focused)
        if (isTvMode || focused) {
            AnimUtils.animateFocusChange(this, focused, scale = 1.07f, elevationFocused = 18f)
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
