package com.mega.tv.ui.details

import android.content.Intent
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
import com.mega.tv.data.model.SeriesEpisode
import com.mega.tv.data.model.SeriesInfoDetail
import com.mega.tv.data.model.SeriesSeason
import com.mega.tv.databinding.ActivitySeriesDetailBinding
import com.mega.tv.ui.home.adapter.MediaCardAdapter
import com.mega.tv.ui.home.adapter.MediaCardItem
import com.mega.tv.ui.player.PlayerActivity
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.launch

class SeriesDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeriesDetailBinding
    private lateinit var prefs: PreferencesManager
    private var xtreamClient: XtreamClient? = null

    private var seriesId: String = ""
    private var seriesTitle: String = ""
    private var posterUrl: String? = null
    private var tmdbId: String? = null

    private lateinit var seasonAdapter: SeasonAdapter
    private lateinit var episodeAdapter: EpisodeAdapter
    private lateinit var relatedAdapter: MediaCardAdapter

    private var seasonsList: List<SeriesSeason> = emptyList()
    private var episodesMap: Map<String, List<SeriesEpisode>> = emptyMap()
    private var currentSeasonNumber: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivitySeriesDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (DeviceUtils.isTv(this)) {
            SystemInsetsUtils.setupEdgeToEdge(this, binding.root)
        } else {
            SystemInsetsUtils.setupEdgeToEdge(this, binding.root, topView = null, bottomView = binding.scrollSeriesDetail)
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
                val statusInsets = insets.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.displayCutout()
                )
                val navInsets = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                val backParams = binding.btnSeriesBack.layoutParams as? FrameLayout.LayoutParams
                if (backParams != null) {
                    backParams.topMargin = statusInsets.top + (12 * resources.displayMetrics.density).toInt()
                    binding.btnSeriesBack.layoutParams = backParams
                }
                binding.scrollSeriesDetail.setPadding(0, 0, 0, navInsets.bottom + (24 * resources.displayMetrics.density).toInt())
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
        loadSeriesInfo()
    }

    private fun extractExtras() {
        seriesId = intent.getStringExtra(AppConstants.EXTRA_STREAM_ID) ?: ""
        seriesTitle = intent.getStringExtra(AppConstants.EXTRA_TITLE) ?: ""
        posterUrl = intent.getStringExtra(AppConstants.EXTRA_POSTER_URL)
        tmdbId = intent.getStringExtra(AppConstants.EXTRA_TMDB_ID)
    }

    private fun setupUI() {
        binding.tvSeriesTitle.text = seriesTitle
        binding.btnSeriesBack.setOnClickListener { finish() }

        seasonAdapter = SeasonAdapter(emptyList()) { season ->
            currentSeasonNumber = season.seasonNumber
            showEpisodesForSeason(currentSeasonNumber)
        }
        binding.rvSeasons.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvSeasons.adapter = seasonAdapter

        episodeAdapter = EpisodeAdapter(emptyList()) { episode ->
            launchPlayerForEpisode(episode)
        }
        binding.rvEpisodes.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvEpisodes.adapter = episodeAdapter

        relatedAdapter = MediaCardAdapter(emptyList()) { item ->
            startActivity(Intent(this, SeriesDetailActivity::class.java).apply {
                putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
                putExtra(AppConstants.EXTRA_TITLE, item.title)
                putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
                putExtra(AppConstants.EXTRA_TMDB_ID, item.tmdbId)
            })
            finish()
        }
        binding.rvRelatedSeries.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvRelatedSeries.adapter = relatedAdapter

        binding.btnWatchSeries.text = "▶  Assistir Agora"
        binding.btnWatchSeries.setSecondary(false)

        binding.btnWatchSeries.setOnClickListener {
            val firstEpisode = episodesMap[currentSeasonNumber.toString()]?.firstOrNull()
                ?: episodesMap.values.firstOrNull()?.firstOrNull()
            if (firstEpisode != null) {
                launchPlayerForEpisode(firstEpisode)
            }
        }

        if (DeviceUtils.isTv(this)) {
            binding.btnWatchSeries.requestFocus()
        }
    }

    private fun loadSeriesInfo() {
        binding.seriesDetailProgress.visibility = View.VISIBLE

        lifecycleScope.launch {
            val client = xtreamClient
            if (client != null && seriesId.isNotBlank()) {
                val res = client.getSeriesInfo(seriesId)
                res.onSuccess { seriesInfoRes ->
                    val info = seriesInfoRes.info
                    seasonsList = seriesInfoRes.seasons
                    episodesMap = seriesInfoRes.episodes
                    if (tmdbId.isNullOrBlank()) {
                        tmdbId = info?.getTmdbIdString()
                    }
                    bindSeriesDetails(info)
                    bindSeasonsAndEpisodes()
                }
            }

            enrichWithTmdb()
            loadRelatedSeries()
            binding.seriesDetailProgress.visibility = View.GONE
        }
    }

    private fun bindSeriesDetails(info: SeriesInfoDetail?) {
        if (info == null) return
        if (!info.name.isNullOrBlank()) binding.tvSeriesTitle.text = info.name
        binding.tvSeriesOverview.text = info.getEffectivePlot()
        binding.tvSeriesYear.text = info.year ?: ""
        binding.tvSeriesRating.text = if (!info.rating.isNullOrBlank()) "★ ${info.rating}" else ""
        binding.tvSeriesGenre.text = info.genre ?: "Série"

        val backdrop = info.backdropPath?.firstOrNull() ?: info.cover ?: posterUrl
        if (!backdrop.isNullOrBlank()) {
            binding.imgSeriesBackdrop.load(backdrop) {
                crossfade(true)
                placeholder(R.drawable.bg_tv_card_border)
            }
        }
    }

    private fun bindSeasonsAndEpisodes() {
        val seasons = if (seasonsList.isNotEmpty()) {
            seasonsList
        } else {
            episodesMap.keys.mapNotNull { it.toIntOrNull() }.sorted().map {
                SeriesSeason(seasonNumber = it, name = "Temporada $it")
            }
        }

        if (seasons.isNotEmpty()) {
            currentSeasonNumber = seasons.first().seasonNumber
            seasonAdapter.updateSeasons(seasons, currentSeasonNumber)
            showEpisodesForSeason(currentSeasonNumber)
        }
    }

    private fun showEpisodesForSeason(seasonNumber: Int) {
        val episodes = episodesMap[seasonNumber.toString()] ?: emptyList()
        binding.tvEpisodesHeading.text = "Episódios - Temporada $seasonNumber (${episodes.size})"
        episodeAdapter.updateEpisodes(episodes)
    }

    private suspend fun enrichWithTmdb() {
        val tmdbIdLong = tmdbId?.toLongOrNull() ?: return
        val config = prefs.getConfig()
        val apiKey = config?.tmdbApiKey
        val lang = config?.tmdbLanguage ?: "pt-BR"
        if (apiKey.isNullOrBlank()) return

        try {
            val res = ApiClient.tmdbService.getTvDetails(tmdbIdLong, apiKey, lang)
            if (res.isSuccessful && res.body() != null) {
                val tv = res.body()!!
                if (!tv.overview.isNullOrBlank()) binding.tvSeriesOverview.text = tv.overview
                if (tv.voteAverage != null && tv.voteAverage > 0) {
                    binding.tvSeriesRating.text = String.format("★ %.1f", tv.voteAverage)
                }
                if (!tv.firstAirDate.isNullOrBlank() && tv.firstAirDate.length >= 4) {
                    binding.tvSeriesYear.text = tv.firstAirDate.substring(0, 4)
                }
                tv.genres?.firstOrNull()?.let { binding.tvSeriesGenre.text = it.name }
                if (!tv.backdropPath.isNullOrBlank()) {
                    binding.imgSeriesBackdrop.load("${AppConstants.TMDB_BACKDROP_W1280}${tv.backdropPath}") {
                        crossfade(true)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadRelatedSeries() {
        val client = xtreamClient ?: return
        val res = client.getSeries()
        res.onSuccess { list ->
            val related = list.filter { it.getSeriesIdString() != seriesId }.take(10).map {
                MediaCardItem(
                    id = it.getSeriesIdString(),
                    title = it.name,
                    posterUrl = it.cover,
                    mediaType = "series",
                    tmdbId = it.getTmdbIdString(),
                    year = it.year
                )
            }
            relatedAdapter.updateItems(related)
        }
    }

    private fun launchPlayerForEpisode(episode: SeriesEpisode) {
        val directSource = episode.directSource ?: return
        val sanitizedUrl = xtreamClient?.sanitizePlaybackUrl(directSource) ?: directSource
        val episodeTitle = "${seriesTitle} - T${currentSeasonNumber}E${episode.episodeNum}"

        startActivity(Intent(this, PlayerActivity::class.java).apply {
            putExtra(AppConstants.EXTRA_STREAM_ID, "${seriesId}_s${currentSeasonNumber}_e${episode.id}")
            putExtra(AppConstants.EXTRA_STREAM_URL, sanitizedUrl)
            putExtra(AppConstants.EXTRA_TITLE, episodeTitle)
            putExtra(AppConstants.EXTRA_SUBTITLE, episode.title)
            putExtra(AppConstants.EXTRA_POSTER_URL, episode.info?.getEffectiveImage() ?: posterUrl)
            putExtra(AppConstants.EXTRA_MEDIA_TYPE, "series_episode")
        })
    }
}
