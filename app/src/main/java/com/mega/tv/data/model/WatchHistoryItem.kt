package com.mega.tv.data.model

import com.google.gson.annotations.SerializedName

data class WatchHistoryItem(
    @SerializedName("stream_id") val streamId: String,
    @SerializedName("media_type") val mediaType: String, // "movie", "series_episode", "live"
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("poster_url") val posterUrl: String? = null,
    @SerializedName("backdrop_url") val backdropUrl: String? = null,
    @SerializedName("direct_source") val directSource: String,
    @SerializedName("position_ms") val positionMs: Long = 0L,
    @SerializedName("duration_ms") val durationMs: Long = 0L,
    @SerializedName("last_watched_timestamp") val lastWatchedTimestamp: Long = System.currentTimeMillis()
) {
    val progressPercentage: Int
        get() = if (durationMs > 0) ((positionMs.toDouble() / durationMs.toDouble()) * 100).toInt() else 0
}
