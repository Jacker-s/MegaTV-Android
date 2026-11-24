package com.mega.tv.ui.theme

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.mega.tv.data.local.PreferencesManager

object ThemeManager {

    fun getPrimaryColor(context: Context): Int {
        val colorHex = PreferencesManager.getInstance(context).getThemeColor()
        return try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#E50914") // Netflix Red fallback
        }
    }

    fun createFocusedBorderDrawable(
        context: Context,
        cornerRadiusDp: Float = 12f,
        strokeWidthDp: Int = 4,
        solidColor: Int = Color.parseColor("#20232B")
    ): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val strokePx = (strokeWidthDp * density).toInt()
        val radiusPx = cornerRadiusDp * density

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(solidColor)
            setStroke(strokePx, getPrimaryColor(context))
        }
    }

    fun createUnfocusedBorderDrawable(
        context: Context,
        cornerRadiusDp: Float = 12f,
        strokeWidthDp: Int = 1,
        solidColor: Int = Color.parseColor("#171922"),
        strokeColor: Int = Color.parseColor("#252A36")
    ): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val strokePx = (strokeWidthDp * density).toInt()
        val radiusPx = cornerRadiusDp * density

        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radiusPx
            setColor(solidColor)
            setStroke(strokePx, strokeColor)
        }
    }

    fun applyFocusAnimation(view: View, hasFocus: Boolean, scale: Float = 1.06f) {
        val targetScale = if (hasFocus) scale else 1.0f
        val targetElevation = if (hasFocus) 12f else 2f

        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, targetScale)
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, targetScale)
        val elevation = ObjectAnimator.ofFloat(view, View.TRANSLATION_Z, targetElevation)

        AnimatorSet().apply {
            playTogether(scaleX, scaleY, elevation)
            duration = 180
            interpolator = DecelerateInterpolator()
            start()
        }
    }
}
