package com.mega.tv.data.model

import com.google.gson.annotations.SerializedName

// Vod (Movies)
data class VodCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

data class VodStream(
    @SerializedName("stream_id") val streamId: Any,
    @SerializedName("name") val name: String,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("tmdb_id") val tmdbId: Any? = null,
    @SerializedName("direct_source") val directSource: String? = null
) {
    fun getStreamIdString(): String = streamId.toString()
    fun getTmdbIdString(): String? = tmdbId?.toString()?.takeIf { it.isNotBlank() && it != "null" }
}

data class VodInfoResponse(
    @SerializedName("info") val info: VodInfoDetail? = null,
    @SerializedName("movie_data") val movieData: VodMovieData? = null
)

data class VodInfoDetail(
    @SerializedName("name") val name: String? = null,
    @SerializedName("cover_big") val coverBig: String? = null,
    @SerializedName("movie_image") val movieImage: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("duration") val duration: String? = null,
    @SerializedName("tmdb_id") val tmdbId: Any? = null
) {
    fun getEffectivePlot(): String = plot ?: description ?: ""
    fun getEffectiveCover(): String? = coverBig ?: movieImage
    fun getTmdbIdString(): String? = tmdbId?.toString()?.takeIf { it.isNotBlank() && it != "null" }
}

data class VodMovieData(
    @SerializedName("stream_id") val streamId: Any? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("container_extension") val containerExtension: String? = "mp4",
    @SerializedName("direct_source") val directSource: String? = null
)

// Series
data class SeriesCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

data class SeriesItem(
    @SerializedName("series_id") val seriesId: Any,
    @SerializedName("name") val name: String,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("tmdb_id") val tmdbId: Any? = null,
    @SerializedName("category_id") val categoryId: String? = null
) {
    fun getSeriesIdString(): String = seriesId.toString()
    fun getTmdbIdString(): String? = tmdbId?.toString()?.takeIf { it.isNotBlank() && it != "null" }
}

data class SeriesInfoResponse(
    @SerializedName("info") val info: SeriesInfoDetail? = null,
    @SerializedName("seasons") val seasons: List<SeriesSeason> = emptyList(),
    @SerializedName("episodes") val episodes: Map<String, List<SeriesEpisode>> = emptyMap()
)

data class SeriesInfoDetail(
    @SerializedName("name") val name: String? = null,
    @SerializedName("cover") val cover: String? = null,
    @SerializedName("backdrop_path") val backdropPath: List<String>? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("genre") val genre: String? = null,
    @SerializedName("year") val year: String? = null,
    @SerializedName("rating") val rating: String? = null,
    @SerializedName("tmdb_id") val tmdbId: Any? = null
) {
    fun getEffectivePlot(): String = plot ?: description ?: ""
    fun getTmdbIdString(): String? = tmdbId?.toString()?.takeIf { it.isNotBlank() && it != "null" }
}

data class SeriesSeason(
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("name") val name: String? = null,
    @SerializedName("episode_count") val episodeCount: Int = 0
)

data class SeriesEpisode(
    @SerializedName("id") val id: Any,
    @SerializedName("episode_num") val episodeNum: Any,
    @SerializedName("title") val title: String? = null,
    @SerializedName("info") val info: SeriesEpisodeInfo? = null,
    @SerializedName("direct_source") val directSource: String? = null
) {
    fun getIdString(): String = id.toString()
    fun getEpisodeNumInt(): Int = episodeNum.toString().toIntOrNull() ?: 1
}

data class SeriesEpisodeInfo(
    @SerializedName("plot") val plot: String? = null,
    @SerializedName("still_path") val stillPath: String? = null,
    @SerializedName("movie_image") val movieImage: String? = null,
    @SerializedName("duration") val duration: String? = null
) {
    fun getEffectiveImage(): String? = stillPath ?: movieImage
}

// Live TV
data class LiveCategory(
    @SerializedName("category_id") val categoryId: String,
    @SerializedName("category_name") val categoryName: String
)

data class LiveStream(
    @SerializedName("stream_id") val streamId: Any,
    @SerializedName("name") val name: String,
    @SerializedName("stream_icon") val streamIcon: String? = null,
    @SerializedName("category_id") val categoryId: String? = null,
    @SerializedName("direct_source") val directSource: String? = null
) {
    fun getStreamIdString(): String = streamId.toString()
}
