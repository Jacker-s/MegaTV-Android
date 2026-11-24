package com.mega.tv.data.api

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mega.tv.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class XtreamClient(
    private val baseXtreamUrl: String,
    private val client: OkHttpClient = ApiClient.okHttpClient,
    private val gson: Gson = Gson()
) {

    /**
     * Builds a full URL for the requested action, preserving all query params
     * present in baseXtreamUrl (including username, password, package_name)
     * and adding action and optional action params.
     */
    fun buildUrl(action: String? = null, extraParams: Map<String, String> = emptyMap()): String {
        val parsedUri = Uri.parse(baseXtreamUrl)
        val uriBuilder = parsedUri.buildUpon()

        if (!action.isNullOrBlank()) {
            uriBuilder.appendQueryParameter("action", action)
        }

        // Ensure package_name is always present in query params
        val existingPkg = parsedUri.getQueryParameter("package_name")
        if (existingPkg.isNullOrBlank()) {
            uriBuilder.appendQueryParameter("package_name", AppConstants.DEFAULT_PACKAGE_NAME)
        }

        for ((key, value) in extraParams) {
            uriBuilder.appendQueryParameter(key, value)
        }

        return uriBuilder.build().toString()
    }

    /**
     * Ensures playback direct_source URLs preserve &package_name=...
     * for whitelist security authorization with 24h cache.
     */
    fun sanitizePlaybackUrl(directSource: String?): String {
        if (directSource.isNullOrBlank()) return ""
        val uri = Uri.parse(directSource)
        val existingPkg = uri.getQueryParameter("package_name")
        return if (existingPkg.isNullOrBlank()) {
            val delimiter = if (directSource.contains("?")) "&" else "?"
            "$directSource${delimiter}package_name=${AppConstants.DEFAULT_PACKAGE_NAME}"
        } else {
            directSource
        }
    }

    private suspend inline fun <reified T> executeGet(url: String): Result<T> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header(AppConstants.HEADER_PACKAGE_NAME, AppConstants.DEFAULT_PACKAGE_NAME)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
                }
                val body = response.body?.string()
                if (body.isNullOrBlank()) {
                    return@withContext Result.failure(Exception("Empty response body"))
                }

                val type = object : TypeToken<T>() {}.type
                val data: T = gson.fromJson(body, type)
                Result.success(data)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 1. Sem action: validação / server_info
    suspend fun getAuthInfo(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = buildUrl(null)
            val request = Request.Builder()
                .url(url)
                .header(AppConstants.HEADER_PACKAGE_NAME, AppConstants.DEFAULT_PACKAGE_NAME)
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(response.body?.string() ?: "")
                } else {
                    Result.failure(Exception("Auth failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Vod Categories
    suspend fun getVodCategories(): Result<List<VodCategory>> {
        val url = buildUrl("get_vod_categories")
        return executeGet<List<VodCategory>>(url)
    }

    // 3. Vod Streams (Movies)
    suspend fun getVodStreams(categoryId: String? = null): Result<List<VodStream>> {
        val params = if (!categoryId.isNullOrBlank()) mapOf("category_id" to categoryId) else emptyMap()
        val url = buildUrl("get_vod_streams", params)
        return executeGet<List<VodStream>>(url)
    }

    // 4. Vod Info (Movie details)
    suspend fun getVodInfo(vodId: String): Result<VodInfoResponse> {
        val url = buildUrl("get_vod_info", mapOf("vod_id" to vodId))
        return executeGet<VodInfoResponse>(url)
    }

    // 5. Series Categories
    suspend fun getSeriesCategories(): Result<List<SeriesCategory>> {
        val url = buildUrl("get_series_categories")
        return executeGet<List<SeriesCategory>>(url)
    }

    // 6. Series List
    suspend fun getSeries(categoryId: String? = null): Result<List<SeriesItem>> {
        val params = if (!categoryId.isNullOrBlank()) mapOf("category_id" to categoryId) else emptyMap()
        val url = buildUrl("get_series", params)
        return executeGet<List<SeriesItem>>(url)
    }

    // 7. Series Info (Seasons & Episodes)
    suspend fun getSeriesInfo(seriesId: String): Result<SeriesInfoResponse> {
        val url = buildUrl("get_series_info", mapOf("series_id" to seriesId))
        return executeGet<SeriesInfoResponse>(url)
    }

    // 8. Live Categories
    suspend fun getLiveCategories(): Result<List<LiveCategory>> {
        val url = buildUrl("get_live_categories")
        return executeGet<List<LiveCategory>>(url)
    }

    // 9. Live Streams (Channels)
    suspend fun getLiveStreams(categoryId: String? = null): Result<List<LiveStream>> {
        val params = if (!categoryId.isNullOrBlank()) mapOf("category_id" to categoryId) else emptyMap()
        val url = buildUrl("get_live_streams", params)
        return executeGet<List<LiveStream>>(url)
    }
}
