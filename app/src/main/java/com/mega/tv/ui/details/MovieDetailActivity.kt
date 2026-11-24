package com.mega.tv.ui.details

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.mega.tv.R
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.api.AppConstants
import com.mega.tv.data.api.XtreamClient
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.VodInfoDetail
import com.mega.tv.databinding.ActivityMovieDetailBinding
import com.mega.tv.ui.home.adapter.MediaCardAdapter
import com.mega.tv.ui.home.adapter.MediaCardItem
import com.mega.tv.ui.player.PlayerActivity
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.launch

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMovieDetailBinding
    private lateinit var prefs: PreferencesManager
    private var xtreamClient: XtreamClient? = null

    private var streamId: String = ""
    private var movieTitle: String = ""
    private var posterUrl: String? = null
    private var directSourceUrl: String? = null
    private var tmdbId: String? = null

    private var trailerYoutubeKey: String? = null
    private lateinit var relatedAdapter: MediaCardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityMovieDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (DeviceUtils.isTv(this)) {
            SystemInsetsUtils.setupEdgeToEdge(this, binding.root)
        } else {
            SystemInsetsUtils.setupEdgeToEdge(this, binding.root, topView = null, bottomView = binding.scrollMovieDetail)
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                val statusInsets = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                )
                val navInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                val backParams = binding.btnMovieBack.layoutParams as? FrameLayout.LayoutParams
                if (backParams != null) {
                    backParams.topMargin = statusInsets.top + (12 * resources.displayMetrics.density).toInt()
                    binding.btnMovieBack.layoutParams = backParams
                }
                binding.scrollMovieDetail.setPadding(0, 0, 0, navInsets.bottom + (24 * resources.displayMetrics.density).toInt())
                insets
            }
            androidx.core.view.ViewCompat.requestApplyInsets(binding.root)
        }

        prefs = PreferencesManager.getInstance(this)
        val xtreamUrl = prefs.getXtreamUrl()
        if (!xtreamUrl.isNullOrBlank()) {
            xtreamClient = XtreamClient(xtreamUrl)
        }

        extractExtras()
        setupUI()
        loadDetails()
    }

    private fun extractExtras() {
        streamId = intent.getStringExtra(AppConstants.EXTRA_STREAM_ID) ?: ""
        movieTitle = intent.getStringExtra(AppConstants.EXTRA_TITLE) ?: ""
        posterUrl = intent.getStringExtra(AppConstants.EXTRA_POSTER_URL)
        directSourceUrl = intent.getStringExtra(AppConstants.EXTRA_STREAM_URL)
        tmdbId = intent.getStringExtra(AppConstants.EXTRA_TMDB_ID)
    }

    private fun setupUI() {
        binding.tvMovieTitle.text = movieTitle
        binding.btnMovieBack.setOnClickListener { finish() }

        relatedAdapter = MediaCardAdapter(emptyList()) { item ->
            val intent = Intent(this, MovieDetailActivity::class.java).apply {
                putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
                putExtra(AppConstants.EXTRA_TITLE, item.title)
                putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
                putExtra(AppConstants.EXTRA_STREAM_URL, item.directSource)
                putExtra(AppConstants.EXTRA_TMDB_ID, item.tmdbId)
            }
            startActivity(intent)
            finish()
        }
        binding.rvRelatedMovies.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRelatedMovies.adapter = relatedAdapter

        binding.btnWatchMovie.text = "▶  Assistir"
        binding.btnWatchMovie.setSecondary(false)
        binding.btnMovieTrailer.setSecondary(true)

        binding.btnWatchMovie.setOnClickListener { launchPlayer() }

        binding.btnMovieTrailer.setOnClickListener {
            trailerYoutubeKey?.let { key ->
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$key")))
            }
        }

        if (DeviceUtils.isTv(this)) {
            binding.btnWatchMovie.requestFocus()
        }
    }

    private fun loadDetails() {
        binding.movieDetailProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val client = xtreamClient
            if (client != null && streamId.isNotBlank()) {
                val infoRes = client.getVodInfo(streamId)
                infoRes.onSuccess { res ->
                    val info = res.info
                    val movieData = res.movieData
                    if (directSourceUrl.isNullOrBlank()) {
                        directSourceUrl = movieData?.directSource
                    }
                    if (tmdbId.isNullOrBlank()) {
                        tmdbId = info?.getTmdbIdString()
                    }
                    bindXtreamDetails(info)
                }
            }

            enrichWithTmdb()
            loadRelatedMovies()
            binding.movieDetailProgress.visibility = View.GONE
        }
    }

    private fun bindXtreamDetails(info: VodInfoDetail?) {
        if (info == null) return
        if (!info.name.isNullOrBlank()) binding.tvMovieTitle.text = info.name
        binding.tvMovieOverview.text = info.getEffectivePlot()
        binding.tvMovieYear.text = info.year ?: ""
        binding.tvMovieRating.text = if (!info.rating.isNullOrBlank()) "★ ${info.rating}" else ""
        binding.tvMovieGenre.text = info.genre ?: "Filme"
        binding.tvMovieDuration.text = info.duration ?: ""

        val cover = info.getEffectiveCover() ?: posterUrl
        if (!cover.isNullOrBlank()) {
            binding.imgMovieBackdrop.load(cover) {
                crossfade(true)
                placeholder(R.drawable.bg_tv_card_border)
            }
        }
    }

    private suspend fun enrichWithTmdb() {
        val tmdbIdLong = tmdbId?.toLongOrNull() ?: return
        val config = prefs.getConfig()
        val apiKey = config?.tmdbApiKey
        val lang = config?.tmdbLanguage ?: "pt-BR"
        if (apiKey.isNullOrBlank()) return

        try {
            val detailsRes = ApiClient.tmdbService.getMovieDetails(tmdbIdLong, apiKey, lang)
            if (detailsRes.isSuccessful && detailsRes.body() != null) {
                val tmdb = detailsRes.body()!!
                if (!tmdb.overview.isNullOrBlank()) binding.tvMovieOverview.text = tmdb.overview
                if (tmdb.voteAverage != null && tmdb.voteAverage > 0) {
                    binding.tvMovieRating.text = String.format("★ %.1f", tmdb.voteAverage)
                }
                if (!tmdb.releaseDate.isNullOrBlank() && tmdb.releaseDate.length >= 4) {
                    binding.tvMovieYear.text = tmdb.releaseDate.substring(0, 4)
                }
                if (tmdb.runtime != null && tmdb.runtime > 0) {
                    binding.tvMovieDuration.text = "${tmdb.runtime / 60}h ${tmdb.runtime % 60}m"
                }
                tmdb.genres?.firstOrNull()?.let { binding.tvMovieGenre.text = it.name }
                if (!tmdb.backdropPath.isNullOrBlank()) {
                    binding.imgMovieBackdrop.load("${AppConstants.TMDB_BACKDROP_W1280}${tmdb.backdropPath}") {
                        crossfade(true)
                    }
                }
            }

            val videosRes = ApiClient.tmdbService.getMovieVideos(tmdbIdLong, apiKey)
            if (videosRes.isSuccessful && videosRes.body() != null) {
                val trailer = videosRes.body()!!.results.firstOrNull {
                    it.site.equals("YouTube", ignoreCase = true) && it.type.equals("Trailer", ignoreCase = true)
                }
                if (trailer != null) {
                    trailerYoutubeKey = trailer.key
                    binding.btnMovieTrailer.visibility = View.VISIBLE
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadRelatedMovies() {
        val client = xtreamClient ?: return
        val streamsRes = client.getVodStreams()
        streamsRes.onSuccess { list ->
            val related = list.filter { it.getStreamIdString() != streamId }.take(10).map {
                MediaCardItem(
                    id = it.getStreamIdString(),
                    title = it.name,
                    posterUrl = it.streamIcon,
                    mediaType = "movie",
                    directSource = it.directSource,
                    tmdbId = it.getTmdbIdString(),
                    year = it.year
                )
            }
            relatedAdapter.updateItems(related)
        }
    }

    private fun launchPlayer() {
        val url = directSourceUrl ?: return
        val sanitizedUrl = xtreamClient?.sanitizePlaybackUrl(url) ?: url
        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(AppConstants.EXTRA_STREAM_ID, streamId)
            putExtra(AppConstants.EXTRA_STREAM_URL, sanitizedUrl)
            putExtra(AppConstants.EXTRA_TITLE, movieTitle)
            putExtra(AppConstants.EXTRA_POSTER_URL, posterUrl)
            putExtra(AppConstants.EXTRA_MEDIA_TYPE, "movie")
        })
    }
}
