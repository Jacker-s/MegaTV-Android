package com.mega.tv.ui.home

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.mega.tv.R
import com.mega.tv.data.api.ApiClient
import com.mega.tv.data.api.AppConstants
import com.mega.tv.data.api.XtreamClient
import com.mega.tv.data.local.CatalogCacheManager
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.local.WatchHistoryManager
import com.mega.tv.databinding.ActivityHomeBinding
import com.mega.tv.ui.channels.ChannelsActivity
import com.mega.tv.ui.details.MovieDetailActivity
import com.mega.tv.ui.details.SeriesDetailActivity
import com.mega.tv.ui.home.adapter.CategoryRowAdapter
import com.mega.tv.ui.home.adapter.CategoryRowModel
import com.mega.tv.ui.home.adapter.MediaCardAdapter
import com.mega.tv.ui.home.adapter.MediaCardItem
import com.mega.tv.ui.plans.PlansActivity
import com.mega.tv.ui.player.PlayerActivity
import com.mega.tv.ui.profile.ProfileActivity
import com.mega.tv.ui.search.SearchActivity
import com.mega.tv.ui.theme.ThemeManager
import com.mega.tv.util.AnimUtils
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var watchManager: WatchHistoryManager
    private lateinit var catalogCache: CatalogCacheManager

    private var xtreamClient: XtreamClient? = null

    private lateinit var continueWatchingAdapter: MediaCardAdapter
    private lateinit var trendingAdapter: MediaCardAdapter
    private lateinit var categoryAdapter: CategoryRowAdapter

    private val spotlightItems = mutableListOf<MediaCardItem>()
    private var currentSpotlightIndex = 0
    private var spotlightTimerJob: Job? = null
    private var currentTab: String = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root,
            topView = binding.topNavBar,
            bottomView = binding.bottomNavContainer
        )

        prefs = PreferencesManager.getInstance(this)
        watchManager = WatchHistoryManager.getInstance(this)
        catalogCache = CatalogCacheManager.getInstance(this)

        if (!prefs.isUserSubscribed() && (prefs.getConfig()?.authEnabled != false)) {
            startActivity(Intent(this, PlansActivity::class.java))
            finish()
            return
        }

        initClients()
        setupNavigation()
        setupAdapters()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        selectNavTab(currentTab)
        loadContinueWatching()
    }

    private fun initClients() {
        val xtreamUrl = prefs.getXtreamUrl()
        if (!xtreamUrl.isNullOrBlank()) {
            xtreamClient = XtreamClient(xtreamUrl)
        }

        prefs.getConfig()?.logoUrl?.let { logoUrl ->
            if (logoUrl.isNotBlank()) {
                binding.imgHomeLogo.load(logoUrl) { crossfade(true) }
            }
        }
    }

    private fun setupNavigation() {
        // Mobile Bottom Tabs + Header Shortcuts
        binding.tabNavHome?.setOnClickListener { onHomeTabSelected() }
        binding.tabNavMovies?.setOnClickListener {
            selectNavTab("movies")
            filterByCategoryType("movie")
        }
        binding.tabNavSeries?.setOnClickListener {
            selectNavTab("series")
            filterByCategoryType("series")
        }
        binding.tabNavChannels?.setOnClickListener {
            startActivity(Intent(this, ChannelsActivity::class.java))
        }
        binding.tabNavProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        binding.cardQuickChannels?.setOnClickListener {
            startActivity(Intent(this, ChannelsActivity::class.java))
        }
        binding.btnHeaderLiveTv?.setOnClickListener {
            startActivity(Intent(this, ChannelsActivity::class.java))
        }
        binding.btnHeaderSearch?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        // TV Sidebar Rail
        binding.tvNavSearch?.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
        binding.tvNavHome?.setOnClickListener { onHomeTabSelected() }
        binding.tvNavMovies?.setOnClickListener {
            selectNavTab("movies")
            filterByCategoryType("movie")
        }
        binding.tvNavSeries?.setOnClickListener {
            selectNavTab("series")
            filterByCategoryType("series")
        }
        binding.tvNavChannels?.setOnClickListener {
            startActivity(Intent(this, ChannelsActivity::class.java))
        }
        binding.tvNavProfile?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        selectNavTab("home")

        if (DeviceUtils.isTv(this)) {
            binding.btnBannerWatch.requestFocus()
        }
    }

    private fun onHomeTabSelected() {
        if (currentTab != "home") {
            selectNavTab("home")
            loadData()
        } else {
            binding.nestedScrollHome.smoothScrollTo(0, 0)
        }
    }

    private fun selectNavTab(tab: String) {
        currentTab = tab
        val primaryColor = ThemeManager.getPrimaryColor(this)
        val inactiveColor = Color.parseColor("#9EFFFFFF")

        fun updateMobileTab(icon: android.widget.ImageView?, text: android.widget.TextView?, isSelected: Boolean) {
            val color = if (isSelected) primaryColor else inactiveColor
            icon?.setColorFilter(color)
            text?.setTextColor(color)
            text?.typeface = if (isSelected) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

        updateMobileTab(binding.iconNavHome, binding.textNavHome, tab == "home")
        updateMobileTab(binding.iconNavMovies, binding.textNavMovies, tab == "movies")
        updateMobileTab(binding.iconNavSeries, binding.textNavSeries, tab == "series")
        updateMobileTab(binding.iconNavChannels, binding.textNavChannels, tab == "channels")
        updateMobileTab(binding.iconNavProfile, binding.textNavProfile, tab == "profile")

        binding.tvNavHome?.isSelected = (tab == "home")
        binding.tvNavMovies?.isSelected = (tab == "movies")
        binding.tvNavSeries?.isSelected = (tab == "series")
        binding.tvNavChannels?.isSelected = (tab == "channels")
        binding.tvNavProfile?.isSelected = (tab == "profile")
    }

    private fun setupAdapters() {
        continueWatchingAdapter = MediaCardAdapter(emptyList()) { item ->
            openPlayer(item, item.progressPercentage)
        }
        binding.rvContinueWatching.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvContinueWatching.adapter = continueWatchingAdapter

        trendingAdapter = MediaCardAdapter(emptyList()) { item ->
            onMediaCardClicked(item)
        }
        binding.rvTrending.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTrending.adapter = trendingAdapter

        categoryAdapter = CategoryRowAdapter(emptyList()) { item ->
            onMediaCardClicked(item)
        }
        binding.rvCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvCategories.adapter = categoryAdapter

        binding.btnBannerWatch.setSecondary(false)
        binding.btnBannerDetails.setSecondary(true)

        binding.btnBannerWatch.setOnClickListener {
            if (spotlightItems.isNotEmpty() && currentSpotlightIndex in spotlightItems.indices) {
                openPlayer(spotlightItems[currentSpotlightIndex], 0)
            }
        }

        binding.btnBannerDetails.setOnClickListener {
            if (spotlightItems.isNotEmpty() && currentSpotlightIndex in spotlightItems.indices) {
                onMediaCardClicked(spotlightItems[currentSpotlightIndex])
            }
        }
    }

    private fun loadData() {
        binding.homeProgressBar.visibility = View.VISIBLE
        binding.bannerContainer.visibility = View.VISIBLE
        binding.cardQuickChannels?.visibility = View.VISIBLE
        binding.sectionTrending.visibility = View.VISIBLE
        binding.nestedScrollHome.smoothScrollTo(0, 0)
        loadContinueWatching()

        lifecycleScope.launch {
            // Instant display from cached catalog if available
            loadCatalogFromCacheFirst()
            // Fresh sync
            loadTrendingAndSpotlight()
            loadXtreamCatalog()
            binding.homeProgressBar.visibility = View.GONE
        }
    }

    private suspend fun loadCatalogFromCacheFirst() {
        val cachedRows = withContext(Dispatchers.IO) {
            val rows = mutableListOf<CategoryRowModel>()
            val vodCats = catalogCache.getVodCategories()
            val vodStreams = catalogCache.getVodStreams()
            if (vodCats.isNotEmpty() && vodStreams.isNotEmpty()) {
                for (cat in vodCats.take(4)) {
                    val items = vodStreams.filter { it.categoryId == cat.categoryId }.take(15).map {
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
                    if (items.isNotEmpty()) {
                        rows.add(CategoryRowModel("vod_${cat.categoryId}", "Filmes: ${cat.categoryName}", items))
                    }
                }
            }
            rows
        }
        if (cachedRows.isNotEmpty()) {
            categoryAdapter.updateRows(cachedRows)
            binding.homeProgressBar.visibility = View.GONE
        }
    }

    private fun loadContinueWatching() {
        val history = watchManager.getHistoryList()
        if (history.isNotEmpty()) {
            binding.sectionContinueWatching.visibility = View.VISIBLE
            val items = history.map {
                MediaCardItem(
                    id = it.streamId,
                    title = it.title,
                    subtitle = it.subtitle,
                    posterUrl = it.posterUrl,
                    mediaType = it.mediaType,
                    directSource = it.directSource,
                    progressPercentage = it.progressPercentage
                )
            }
            continueWatchingAdapter.updateItems(items)
        } else {
            binding.sectionContinueWatching.visibility = View.GONE
        }
    }

    private suspend fun loadTrendingAndSpotlight() {
        val config = prefs.getConfig()
        val apiKey = config?.tmdbApiKey
        val lang = config?.tmdbLanguage ?: "pt-BR"
        val client = xtreamClient ?: return

        val vodRes = client.getVodStreams()
        val vodStreams = vodRes.getOrDefault(emptyList())
        if (vodStreams.isNotEmpty()) {
            withContext(Dispatchers.IO) { catalogCache.saveVodStreams(vodStreams) }
        }
        val vodByTmdb = vodStreams.mapNotNull { stream ->
            val tid = stream.getTmdbIdString()
            if (tid != null) tid to stream else null
        }.toMap()

        val trendingMatched = mutableListOf<MediaCardItem>()

        if (!apiKey.isNullOrBlank()) {
            try {
                val tmdbRes = ApiClient.tmdbService.getTrending(apiKey, lang)
                if (tmdbRes.isSuccessful && tmdbRes.body() != null) {
                    val tmdbResults = tmdbRes.body()!!.results
                    for (t in tmdbResults) {
                        val matchedVod = vodByTmdb[t.id.toString()]
                        if (matchedVod != null) {
                            trendingMatched.add(
                                MediaCardItem(
                                    id = matchedVod.getStreamIdString(),
                                    title = t.getEffectiveTitle().ifBlank { matchedVod.name },
                                    posterUrl = if (!t.posterPath.isNullOrBlank()) "${AppConstants.TMDB_POSTER_W500}${t.posterPath}" else matchedVod.streamIcon,
                                    backdropUrl = if (!t.backdropPath.isNullOrBlank()) "${AppConstants.TMDB_BACKDROP_W1280}${t.backdropPath}" else matchedVod.streamIcon,
                                    mediaType = "movie",
                                    directSource = matchedVod.directSource,
                                    tmdbId = matchedVod.getTmdbIdString(),
                                    year = matchedVod.year,
                                    rating = t.voteAverage?.let { String.format("%.1f", it) },
                                    overview = t.overview
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (trendingMatched.isEmpty() && vodStreams.isNotEmpty()) {
            vodStreams.take(10).forEach { vod ->
                trendingMatched.add(
                    MediaCardItem(
                        id = vod.getStreamIdString(),
                        title = vod.name,
                        posterUrl = vod.streamIcon,
                        backdropUrl = vod.streamIcon,
                        mediaType = "movie",
                        directSource = vod.directSource,
                        tmdbId = vod.getTmdbIdString(),
                        year = vod.year,
                        overview = "Disponível em alta definição."
                    )
                )
            }
        }

        if (trendingMatched.isNotEmpty()) {
            trendingAdapter.updateItems(trendingMatched)
            spotlightItems.clear()
            spotlightItems.addAll(trendingMatched.take(5))
            renderSpotlightItem(spotlightItems.first())
            startSpotlightAutoScroll()
        }
    }

    private fun renderSpotlightItem(item: MediaCardItem) {
        binding.tvBannerTitle.text = item.title
        binding.tvBannerOverview.text = item.overview ?: ""
        binding.tvBannerYear.text = item.year ?: ""
        binding.tvBannerRating.text = if (!item.rating.isNullOrBlank()) "★ ${item.rating}" else ""
        binding.tvBannerType.text = if (item.mediaType == "series") "SÉRIE" else "FILME"

        val imageToLoad = item.backdropUrl ?: item.posterUrl
        if (!imageToLoad.isNullOrBlank()) {
            binding.imgBannerBackdrop.load(imageToLoad) {
                crossfade(400)
                placeholder(R.drawable.bg_tv_card_border)
            }
        }
    }

    private fun startSpotlightAutoScroll() {
        spotlightTimerJob?.cancel()
        spotlightTimerJob = lifecycleScope.launch {
            while (isActive) {
                delay(8000)
                if (spotlightItems.isNotEmpty() && currentTab == "home") {
                    currentSpotlightIndex = (currentSpotlightIndex + 1) % spotlightItems.size
                    renderSpotlightItem(spotlightItems[currentSpotlightIndex])
                }
            }
        }
    }

    private suspend fun loadXtreamCatalog() {
        val client = xtreamClient ?: return
        val rows = mutableListOf<CategoryRowModel>()

        val vodCatsRes = client.getVodCategories()
        vodCatsRes.onSuccess { cats ->
            withContext(Dispatchers.IO) { catalogCache.saveVodCategories(cats) }
            for (cat in cats.take(4)) {
                val streamsRes = client.getVodStreams(cat.categoryId)
                streamsRes.onSuccess { streams ->
                    if (streams.isNotEmpty()) {
                        val items = streams.map {
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
                        rows.add(CategoryRowModel("vod_${cat.categoryId}", "Filmes: ${cat.categoryName}", items))
                    }
                }
            }
        }

        val seriesCatsRes = client.getSeriesCategories()
        seriesCatsRes.onSuccess { cats ->
            withContext(Dispatchers.IO) { catalogCache.saveSeriesCategories(cats) }
            for (cat in cats.take(3)) {
                val seriesRes = client.getSeries(cat.categoryId)
                seriesRes.onSuccess { seriesList ->
                    if (seriesList.isNotEmpty()) {
                        val items = seriesList.map {
                            MediaCardItem(
                                id = it.getSeriesIdString(),
                                title = it.name,
                                posterUrl = it.cover,
                                mediaType = "series",
                                tmdbId = it.getTmdbIdString(),
                                year = it.year
                            )
                        }
                        rows.add(CategoryRowModel("series_${cat.categoryId}", "Séries: ${cat.categoryName}", items))
                    }
                }
            }
        }

        if (rows.isNotEmpty()) {
            categoryAdapter.updateRows(rows)
        }

        // Background full sync into disk cache
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                client.getVodStreams().getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                    catalogCache.saveVodStreams(it)
                }
                client.getSeries().getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                    catalogCache.saveSeries(it)
                }
                client.getLiveStreams().getOrNull()?.takeIf { it.isNotEmpty() }?.let {
                    catalogCache.saveLiveStreams(it)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun filterByCategoryType(type: String) {
        binding.bannerContainer.visibility = View.GONE
        binding.cardQuickChannels?.visibility = View.GONE
        binding.sectionContinueWatching.visibility = View.GONE
        binding.sectionTrending.visibility = View.GONE
        binding.nestedScrollHome.smoothScrollTo(0, 0)

        lifecycleScope.launch {
            binding.homeProgressBar.visibility = View.VISIBLE
            val client = xtreamClient
            val rows = mutableListOf<CategoryRowModel>()

            if (type == "movie") {
                val cats = withContext(Dispatchers.IO) { catalogCache.getVodCategories() }.ifEmpty {
                    client?.getVodCategories()?.getOrDefault(emptyList()) ?: emptyList()
                }
                val cachedAllVod = withContext(Dispatchers.IO) { catalogCache.getVodStreams() }

                for (cat in cats.take(8)) {
                    val streams = cachedAllVod.filter { it.categoryId == cat.categoryId }.ifEmpty {
                        client?.getVodStreams(cat.categoryId)?.getOrDefault(emptyList()) ?: emptyList()
                    }
                    if (streams.isNotEmpty()) {
                        rows.add(
                            CategoryRowModel(
                                "vod_${cat.categoryId}",
                                cat.categoryName,
                                streams.map {
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
                            )
                        )
                    }
                }
            } else if (type == "series") {
                val cats = withContext(Dispatchers.IO) { catalogCache.getSeriesCategories() }.ifEmpty {
                    client?.getSeriesCategories()?.getOrDefault(emptyList()) ?: emptyList()
                }
                val cachedAllSeries = withContext(Dispatchers.IO) { catalogCache.getSeries() }

                for (cat in cats.take(8)) {
                    val series = cachedAllSeries.filter { it.categoryId == cat.categoryId }.ifEmpty {
                        client?.getSeries(cat.categoryId)?.getOrDefault(emptyList()) ?: emptyList()
                    }
                    if (series.isNotEmpty()) {
                        rows.add(
                            CategoryRowModel(
                                "series_${cat.categoryId}",
                                cat.categoryName,
                                series.map {
                                    MediaCardItem(
                                        id = it.getSeriesIdString(),
                                        title = it.name,
                                        posterUrl = it.cover,
                                        mediaType = "series",
                                        tmdbId = it.getTmdbIdString(),
                                        year = it.year
                                    )
                                }
                            )
                        )
                    }
                }
            }

            categoryAdapter.updateRows(rows)
            binding.homeProgressBar.visibility = View.GONE
            if (DeviceUtils.isTv(this@HomeActivity)) {
                binding.rvCategories.post { binding.rvCategories.getChildAt(0)?.requestFocus() }
            }
        }
    }

    private fun onMediaCardClicked(item: MediaCardItem) {
        when (item.mediaType) {
            "series" -> {
                val intent = Intent(this, SeriesDetailActivity::class.java).apply {
                    putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
                    putExtra(AppConstants.EXTRA_TITLE, item.title)
                    putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
                    putExtra(AppConstants.EXTRA_TMDB_ID, item.tmdbId)
                }
                startActivity(intent)
            }
            "live" -> openPlayer(item, 0)
            else -> {
                val intent = Intent(this, MovieDetailActivity::class.java).apply {
                    putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
                    putExtra(AppConstants.EXTRA_TITLE, item.title)
                    putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
                    putExtra(AppConstants.EXTRA_STREAM_URL, item.directSource)
                    putExtra(AppConstants.EXTRA_TMDB_ID, item.tmdbId)
                }
                startActivity(intent)
            }
        }
    }

    private fun openPlayer(item: MediaCardItem, resumePosition: Int) {
        val directSource = item.directSource ?: return
        val sanitizedUrl = xtreamClient?.sanitizePlaybackUrl(directSource) ?: directSource

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
            putExtra(AppConstants.EXTRA_STREAM_URL, sanitizedUrl)
            putExtra(AppConstants.EXTRA_TITLE, item.title)
            putExtra(AppConstants.EXTRA_SUBTITLE, item.subtitle)
            putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
            putExtra(AppConstants.EXTRA_MEDIA_TYPE, item.mediaType)
            putExtra(AppConstants.EXTRA_RESUME_POSITION, resumePosition.toLong())
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        spotlightTimerJob?.cancel()
    }
}
