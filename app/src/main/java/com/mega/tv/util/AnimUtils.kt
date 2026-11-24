package com.mega.tv.util

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator

/**
 * Lightweight UI animation helpers for smooth, lag-free mobile & TV transitions.
 */
object AnimUtils {

    fun fadeInSlideUp(view: View, delayMs: Long = 0L, durationMs: Long = 320L) {
        view.alpha = 0f
        view.translationY = 24f

        val alphaAnim = ObjectAnimator.ofFloat(view, View.ALPHA, 0f, 1f)
        val slideAnim = ObjectAnimator.ofFloat(view, View.TRANSLATION_Y, 24f, 0f)

        AnimatorSet().apply {
            playTogether(alphaAnim, slideAnim)
            startDelay = delayMs
            duration = durationMs
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    fun scalePulse(view: View, targetScale: Float = 1.06f, durationMs: Long = 160L) {
        val sx = ObjectAnimator.ofFloat(view, View.SCALE_X, 1.0f, targetScale, 1.0f)
        val sy = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.0f, targetScale, 1.0f)
        AnimatorSet().apply {
            playTogether(sx, sy)
            duration = durationMs
            interpolator = OvershootInterpolator(1.2f)
            start()
        }
    }

    fun crossfadeVisible(view: View, durationMs: Long = 220L) {
        if (view.visibility != View.VISIBLE) {
            view.alpha = 0f
            view.visibility = View.VISIBLE
        }
        view.animate()
            .alpha(1f)
            .setDuration(durationMs)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun animateFocusChange(view: View, hasFocus: Boolean, scale: Float = 1.07f, elevationFocused: Float = 16f) {
        val targetScale = if (hasFocus) scale else 1.0f
        val targetZ = if (hasFocus) elevationFocused else 2f

        view.animate()
            .scaleX(targetScale)
            .scaleY(targetScale)
            .translationZ(targetZ)
            .setDuration(150L)
            .setInterpolator(if (hasFocus) OvershootInterpolator(1.1f) else DecelerateInterpolator())
            .start()
    }
}
