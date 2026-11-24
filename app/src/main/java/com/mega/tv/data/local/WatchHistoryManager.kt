package com.mega.tv.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mega.tv.data.model.WatchHistoryItem

class WatchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mega_tv_watch_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_HISTORY_LIST = "key_watch_history_list"

        @Volatile
        private var instance: WatchHistoryManager? = null

        fun getInstance(context: Context): WatchHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: WatchHistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }

    @Synchronized
    fun getHistoryList(): List<WatchHistoryItem> {
        val json = prefs.getString(KEY_HISTORY_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<WatchHistoryItem>>() {}.type
            val list: List<WatchHistoryItem> = gson.fromJson(json, type)
            list.sortedByDescending { it.lastWatchedTimestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun saveProgress(
        streamId: String,
        mediaType: String,
        title: String,
        subtitle: String? = null,
        posterUrl: String? = null,
        backdropUrl: String? = null,
        directSource: String,
        positionMs: Long,
        durationMs: Long
    ) {
        // If content is > 95% watched, consider finished and remove from "Continuar Assistindo"
        if (durationMs > 0 && (positionMs.toDouble() / durationMs.toDouble()) >= 0.95) {
            removeItem(streamId)
            return
        }

        val currentList = getHistoryList().toMutableList()
        // Remove existing item with same streamId if present
        currentList.removeAll { it.streamId == streamId }

        val newItem = WatchHistoryItem(
            streamId = streamId,
            mediaType = mediaType,
            title = title,
            subtitle = subtitle,
            posterUrl = posterUrl,
            backdropUrl = backdropUrl,
            directSource = directSource,
            positionMs = positionMs,
            durationMs = durationMs,
            lastWatchedTimestamp = System.currentTimeMillis()
        )

        // Insert at beginning
        currentList.add(0, newItem)

        // Keep maximum 30 items
        val trimmedList = if (currentList.size > 30) currentList.subList(0, 30) else currentList

        prefs.edit().putString(KEY_HISTORY_LIST, gson.toJson(trimmedList)).apply()
    }

    @Synchronized
    fun removeItem(streamId: String) {
        val currentList = getHistoryList().toMutableList()
        val removed = currentList.removeAll { it.streamId == streamId }
        if (removed) {
            prefs.edit().putString(KEY_HISTORY_LIST, gson.toJson(currentList)).apply()
        }
    }

    @Synchronized
    fun getSavedPosition(streamId: String): Long {
        val item = getHistoryList().find { it.streamId == streamId }
        return item?.positionMs ?: 0L
    }
}
