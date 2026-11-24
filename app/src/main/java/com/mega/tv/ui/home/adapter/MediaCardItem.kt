package com.mega.tv.ui.home.adapter

data class MediaCardItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val mediaType: String, // "movie", "series", "live"
    val directSource: String? = null,
    val progressPercentage: Int = 0,
    val tmdbId: String? = null,
    val year: String? = null,
    val rating: String? = null,
    val overview: String? = null
)
