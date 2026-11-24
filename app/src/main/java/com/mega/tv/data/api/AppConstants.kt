package com.mega.tv.data.api

object AppConstants {
    const val DEFAULT_PACKAGE_NAME = "com.mega.tv"
    const val HEADER_PACKAGE_NAME = "X-Package-Name"

    // Base URL da API REST MegaEmbed App
    const val DEFAULT_APP_BASE_URL = "https://deli.megaembed.com/api/app/"

    // SharedPreferences
    const val PREFS_NAME = "mega_tv_preferences"

    // TMDB
    const val TMDB_API_BASE_URL = "https://api.themoviedb.org/3/"
    const val TMDB_POSTER_W500 = "https://image.tmdb.org/t/p/w500"
    const val TMDB_BACKDROP_W1280 = "https://image.tmdb.org/t/p/w1280"

    // Intent Extras
    const val EXTRA_STREAM_ID = "extra_stream_id"
    const val EXTRA_STREAM_URL = "extra_stream_url"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_SUBTITLE = "extra_subtitle"
    const val EXTRA_POSTER_URL = "extra_poster_url"
    const val EXTRA_BACKDROP_URL = "extra_backdrop_url"
    const val EXTRA_MEDIA_TYPE = "extra_media_type" // "movie", "series_episode", "live"
    const val EXTRA_TMDB_ID = "extra_tmdb_id"
    const val EXTRA_RESUME_POSITION = "extra_resume_position"
}
