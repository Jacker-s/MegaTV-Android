package com.mega.tv.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.mega.tv.data.api.AppConstants
import com.mega.tv.data.model.ConfigResponse
import com.mega.tv.data.model.UserModel

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_CONFIG = "key_app_config"
        private const val KEY_USER = "key_logged_user"
        private const val KEY_THEME_COLOR = "key_theme_color"
        private const val KEY_XTREAM_URL = "key_xtream_url"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    // Config
    fun saveConfig(config: ConfigResponse) {
        val json = gson.toJson(config)
        prefs.edit()
            .putString(KEY_CONFIG, json)
            .putString(KEY_THEME_COLOR, config.themeColor)
            .putString(KEY_XTREAM_URL, config.xtreamUrl)
            .apply()
    }

    fun getConfig(): ConfigResponse? {
        val json = prefs.getString(KEY_CONFIG, null) ?: return null
        return try {
            gson.fromJson(json, ConfigResponse::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getThemeColor(): String {
        return prefs.getString(KEY_THEME_COLOR, null) ?: "#E50914"
    }

    fun getXtreamUrl(): String? {
        return prefs.getString(KEY_XTREAM_URL, null) ?: getConfig()?.xtreamUrl
    }

    // User
    fun saveUser(user: UserModel) {
        val json = gson.toJson(user)
        prefs.edit().putString(KEY_USER, json).apply()
    }

    fun getUser(): UserModel? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, UserModel::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun updateSubscription(hasActiveSubscription: Boolean, expDate: Long) {
        val currentUser = getUser() ?: UserModel()
        val updatedUser = currentUser.copy(
            hasActiveSubscription = hasActiveSubscription,
            expDate = expDate
        )
        saveUser(updatedUser)
    }

    fun isUserSubscribed(): Boolean {
        val user = getUser() ?: return false
        return user.isSubscriptionValid()
    }

    fun hasSavedUser(): Boolean {
        return getUser() != null
    }

    fun clearSession() {
        prefs.edit().remove(KEY_USER).apply()
    }
}
