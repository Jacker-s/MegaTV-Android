package com.mega.tv.data.model

import com.google.gson.annotations.SerializedName

data class TmdbGenre(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String
)

data class TmdbMovieDetails(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("runtime") val runtime: Int? = null,
    @SerializedName("genres") val genres: List<TmdbGenre>? = emptyList()
)

data class TmdbTvDetails(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("genres") val genres: List<TmdbGenre>? = emptyList()
)

data class TmdbVideoItem(
    @SerializedName("id") val id: String,
    @SerializedName("key") val key: String,
    @SerializedName("name") val name: String? = null,
    @SerializedName("site") val site: String? = null,
    @SerializedName("type") val type: String? = null
)

data class TmdbVideosResponse(
    @SerializedName("results") val results: List<TmdbVideoItem> = emptyList()
)

data class TmdbTrendingItem(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("overview") val overview: String? = null,
    @SerializedName("poster_path") val posterPath: String? = null,
    @SerializedName("backdrop_path") val backdropPath: String? = null,
    @SerializedName("media_type") val mediaType: String? = null,
    @SerializedName("vote_average") val voteAverage: Double? = null,
    @SerializedName("release_date") val releaseDate: String? = null,
    @SerializedName("first_air_date") val firstAirDate: String? = null
) {
    fun getEffectiveTitle(): String = title ?: name ?: ""
    fun getEffectiveYear(): String {
        val date = releaseDate ?: firstAirDate ?: ""
        return if (date.length >= 4) date.substring(0, 4) else ""
    }
}

data class TmdbTrendingResponse(
    @SerializedName("results") val results: List<TmdbTrendingItem> = emptyList()
)
