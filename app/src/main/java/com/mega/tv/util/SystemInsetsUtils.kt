package com.mega.tv.util

import android.app.Activity
import android.graphics.Color
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Utility to ensure the entire application cleanly respects system bars on Mobile
 * (status bar / punch-hole & bottom navigation bar) while running cleanly fullscreen on Android TV.
 */
object SystemInsetsUtils {

    fun setupEdgeToEdge(
        activity: Activity,
        rootView: View,
        topView: View? = null,
        bottomView: View? = null,
        lightStatusBar: Boolean = false
    ) {
        val window = activity.window

        // On TV devices, hide system bars completely for clean big-screen experience
        if (DeviceUtils.isTv(activity)) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            return
        }

        // Mobile: Draw behind system bars and apply dynamic insets cleanly
        WindowCompat.setDecorFitsSystemWindows(window, false)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.parseColor("#0B0C0E")

        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = lightStatusBar
            isAppearanceLightNavigationBars = false
        }

        val initialTopPadding = topView?.paddingTop ?: 0
        val initialBottomPadding = bottomView?.paddingBottom ?: 0
        val initialRootTopPadding = rootView.paddingTop
        val initialRootBottomPadding = rootView.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            val statusInsets = insets.getInsets(
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
            )
            val navInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            topView?.let { top ->
                top.setPadding(
                    top.paddingLeft,
                    initialTopPadding + statusInsets.top,
                    top.paddingRight,
                    top.paddingBottom
                )
            }

            bottomView?.let { bottom ->
                bottom.setPadding(
                    bottom.paddingLeft,
                    bottom.paddingTop,
                    bottom.paddingRight,
                    initialBottomPadding + navInsets.bottom
                )
            }

            if (topView == null && bottomView == null) {
                rootView.setPadding(
                    rootView.paddingLeft,
                    initialRootTopPadding + statusInsets.top,
                    rootView.paddingRight,
                    initialRootBottomPadding + navInsets.bottom
                )
            } else if (topView == null) {
                rootView.setPadding(
                    rootView.paddingLeft,
                    initialRootTopPadding + statusInsets.top,
                    rootView.paddingRight,
                    rootView.paddingBottom
                )
            } else if (bottomView == null) {
                rootView.setPadding(
                    rootView.paddingLeft,
                    rootView.paddingTop,
                    rootView.paddingRight,
                    initialRootBottomPadding + navInsets.bottom
                )
            }

            insets
        }

        ViewCompat.requestApplyInsets(rootView)
    }
}
