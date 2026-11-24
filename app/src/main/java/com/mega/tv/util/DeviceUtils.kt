package com.mega.tv.util

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration

/**
 * Helper to detect whether the app is running on Android TV / Google TV / TV Box
 * or on a Mobile phone/tablet, and adapt orientation and input behavior dynamically.
 */
object DeviceUtils {

    private var cachedIsTv: Boolean? = null

    fun isTv(context: Context): Boolean {
        cachedIsTv?.let { return it }

        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTvMode = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        val pm = context.packageManager
        val hasLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        val hasTouch = pm.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)

        // Note: Some TV devices run Leanback without reporting UI_MODE_TYPE_TELEVISION,
        // or have no touchscreen at all.
        val result = isTvMode || (hasLeanback && !hasTouch) || !hasTouch
        cachedIsTv = result
        return result
    }

    /**
     * Configures optimal screen orientation:
     * - Player or TV devices -> Sensor Landscape
     * - Mobile non-player screens -> Portrait
     */
    fun applyOrientation(activity: Activity, isPlayer: Boolean = false) {
        activity.requestedOrientation = if (isPlayer || isTv(activity)) {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }
}
