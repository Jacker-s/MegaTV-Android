package com.mega.tv.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mega.tv.data.model.LiveCategory
import com.mega.tv.data.model.LiveStream
import com.mega.tv.data.model.SeriesCategory
import com.mega.tv.data.model.SeriesItem
import com.mega.tv.data.model.VodCategory
import com.mega.tv.data.model.VodStream
import java.io.File

/**
 * Robust Disk-based Catalog Cache for instant loading, full offline/cached capability,
 * and lightning-fast local search for Movies and Series.
 */
class CatalogCacheManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("mega_tv_catalog_cache", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val FILE_VOD_CATEGORIES = "catalog_vod_categories.json"
        private const val FILE_VOD_STREAMS = "catalog_vod_streams.json"
        private const val FILE_SERIES_CATEGORIES = "catalog_series_categories.json"
        private const val FILE_SERIES_STREAMS = "catalog_series_streams.json"
        private const val FILE_LIVE_CATEGORIES = "catalog_live_categories.json"
        private const val FILE_LIVE_STREAMS = "catalog_live_streams.json"

        private const val KEY_LAST_SYNC = "key_last_sync_timestamp"

        @Volatile
        private var instance: CatalogCacheManager? = null

        fun getInstance(context: Context): CatalogCacheManager {
            return instance ?: synchronized(this) {
                instance ?: CatalogCacheManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun <T> saveToFile(filename: String, data: T) {
        try {
            val file = File(context.filesDir, filename)
            file.bufferedWriter().use { writer ->
                gson.toJson(data, writer)
            }
            prefs.edit().putLong(KEY_LAST_SYNC, System.currentTimeMillis()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- VOD (Movies) Categories & Streams ---

    fun saveVodCategories(categories: List<VodCategory>) {
        saveToFile(FILE_VOD_CATEGORIES, categories)
    }

    fun getVodCategories(): List<VodCategory> {
        return try {
            val file = File(context.filesDir, FILE_VOD_CATEGORIES)
            if (!file.exists()) return emptyList()
            file.bufferedReader().use { reader ->
                val type = object : TypeToken<List<VodCategory>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveVodStreams(streams: List<VodStream>) {
        saveToFile(FILE_VOD_STREAMS, streams)
    }

    fun getVodStreams(): List<VodStream> {
        return try {
            val file = File(context.filesDir, FILE_VOD_STREAMS)
            if (!file.exists()) return emptyList()
            file.bufferedReader().use { reader ->
                val type = object : TypeToken<List<VodStream>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Series Categories & Items ---

    fun saveSeriesCategories(categories: List<SeriesCategory>) {
        saveToFile(FILE_SERIES_CATEGORIES, categories)
    }

    fun getSeriesCategories(): List<SeriesCategory> {
        return try {
            val file = File(context.filesDir, FILE_SERIES_CATEGORIES)
            if (!file.exists()) return emptyList()
            file.bufferedReader().use { reader ->
                val type = object : TypeToken<List<SeriesCategory>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSeries(series: List<SeriesItem>) {
        saveToFile(FILE_SERIES_STREAMS, series)
    }

    fun getSeries(): List<SeriesItem> {
        return try {
            val file = File(context.filesDir, FILE_SERIES_STREAMS)
            if (!file.exists()) return emptyList()
            file.bufferedReader().use { reader ->
                val type = object : TypeToken<List<SeriesItem>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Live Categories & Streams ---

    fun saveLiveCategories(categories: List<LiveCategory>) {
        saveToFile(FILE_LIVE_CATEGORIES, categories)
    }

    fun getLiveCategories(): List<LiveCategory> {
        return try {
            val file = File(context.filesDir, FILE_LIVE_CATEGORIES)
            if (!file.exists()) return emptyList()
            file.bufferedReader().use { reader ->
                val type = object : TypeToken<List<LiveCategory>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveLiveStreams(streams: List<LiveStream>) {
        saveToFile(FILE_LIVE_STREAMS, streams)
    }

    fun getLiveStreams(): List<LiveStream> {
        return try {
            val file = File(context.filesDir, FILE_LIVE_STREAMS)
            if (!file.exists()) return emptyList()
            file.bufferedReader().use { reader ->
                val type = object : TypeToken<List<LiveStream>>() {}.type
                gson.fromJson(reader, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0L)
    }

    fun hasCachedCatalog(): Boolean {
        return File(context.filesDir, FILE_VOD_CATEGORIES).exists() ||
                File(context.filesDir, FILE_VOD_STREAMS).exists()
    }
}
