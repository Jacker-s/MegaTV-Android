package com.mega.tv.ui.search

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.mega.tv.R
import com.mega.tv.data.api.AppConstants
import com.mega.tv.data.api.XtreamClient
import com.mega.tv.data.local.CatalogCacheManager
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.SeriesItem
import com.mega.tv.data.model.VodStream
import com.mega.tv.databinding.ActivitySearchBinding
import com.mega.tv.ui.details.MovieDetailActivity
import com.mega.tv.ui.details.SeriesDetailActivity
import com.mega.tv.ui.home.adapter.MediaCardAdapter
import com.mega.tv.ui.home.adapter.MediaCardItem
import com.mega.tv.ui.theme.ThemeManager
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var catalogCache: CatalogCacheManager
    private var xtreamClient: XtreamClient? = null

    private lateinit var searchAdapter: MediaCardAdapter
    private val allMovies = mutableListOf<VodStream>()
    private val allSeries = mutableListOf<SeriesItem>()

    private var currentFilter = "all"
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root,
            topView = binding.searchHeader,
            bottomView = binding.rvSearchResults
        )

        prefs = PreferencesManager.getInstance(this)
        catalogCache = CatalogCacheManager.getInstance(this)
        prefs.getXtreamUrl()?.let { url ->
            if (url.isNotBlank()) xtreamClient = XtreamClient(url)
        }

        setupUI()
        loadCatalogForSearch()
    }

    private fun setupUI() {
        binding.btnSearchBack.setOnClickListener { finish() }
        binding.btnClearSearch.setOnClickListener { binding.etSearchQuery.setText("") }

        searchAdapter = MediaCardAdapter(emptyList()) { item ->
            onResultClicked(item)
        }

        val isTv = DeviceUtils.isTv(this)
        val spanCount = if (isTv) 4 else 3
        binding.rvSearchResults.layoutManager = GridLayoutManager(this, spanCount)
        binding.rvSearchResults.adapter = searchAdapter

        binding.etSearchQuery.doAfterTextChanged { text ->
            val query = text?.toString()?.trim().orEmpty()
            binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
            scheduleSearch(query)
        }

        binding.chipFilterAll.setOnClickListener { setFilter("all") }
        binding.chipFilterMovies.setOnClickListener { setFilter("movie") }
        binding.chipFilterSeries.setOnClickListener { setFilter("series") }

        if (isTv) {
            binding.etSearchQuery.showSoftInputOnFocus = false
            binding.tvSearchKeyboard?.attachEditText(binding.etSearchQuery)
            binding.tvSearchKeyboard?.onDoneClickListener = {
                if (searchAdapter.itemCount > 0) {
                    binding.rvSearchResults.getChildAt(0)?.requestFocus()
                }
            }
            binding.tvSearchKeyboard?.requestFocus()
        }

        updateFilterChips()
    }

    private fun setFilter(filter: String) {
        if (currentFilter != filter) {
            currentFilter = filter
            updateFilterChips()
            performSearch(binding.etSearchQuery.text?.toString()?.trim().orEmpty())
        }
    }

    private fun updateFilterChips() {
        val primaryColor = ThemeManager.getPrimaryColor(this)

        fun updateChip(chip: android.widget.TextView, selected: Boolean) {
            chip.isSelected = selected
            if (DeviceUtils.isTv(this)) {
                chip.setBackgroundResource(R.drawable.bg_tv_tab)
                chip.setTextColor(if (selected) primaryColor else Color.parseColor("#B3FFFFFF"))
            } else {
                if (selected) {
                    chip.setBackgroundResource(R.drawable.bg_tv_tab)
                    chip.setTextColor(primaryColor)
                } else {
                    chip.setBackgroundResource(R.drawable.bg_circle_button)
                    chip.setTextColor(Color.parseColor("#88FFFFFF"))
                }
            }
        }

        updateChip(binding.chipFilterAll, currentFilter == "all")
        updateChip(binding.chipFilterMovies, currentFilter == "movie")
        updateChip(binding.chipFilterSeries, currentFilter == "series")
    }

    private fun loadCatalogForSearch() {
        lifecycleScope.launch {
            binding.pbSearchLoading.visibility = View.VISIBLE

            val cachedMovies = withContext(Dispatchers.IO) { catalogCache.getVodStreams() }
            val cachedSeries = withContext(Dispatchers.IO) { catalogCache.getSeries() }

            if (cachedMovies.isNotEmpty()) {
                allMovies.clear()
                allMovies.addAll(cachedMovies)
            }
            if (cachedSeries.isNotEmpty()) {
                allSeries.clear()
                allSeries.addAll(cachedSeries)
            }

            val client = xtreamClient
            if ((allMovies.isEmpty() || allSeries.isEmpty()) && client != null) {
                withContext(Dispatchers.IO) {
                    client.getVodStreams().getOrNull()?.let {
                        allMovies.clear()
                        allMovies.addAll(it)
                        catalogCache.saveVodStreams(it)
                    }
                    client.getSeries().getOrNull()?.let {
                        allSeries.clear()
                        allSeries.addAll(it)
                        catalogCache.saveSeries(it)
                    }
                }
            }

            binding.pbSearchLoading.visibility = View.GONE

            val initialQuery = binding.etSearchQuery.text?.toString()?.trim().orEmpty()
            if (initialQuery.isNotEmpty()) {
                performSearch(initialQuery)
            }
        }
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            delay(180)
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            binding.tvSearchResultsCount.visibility = View.GONE
            binding.layoutEmptySearch.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "Busque por filmes ou séries"
            binding.tvEmptySubtitle.text = "Digite o nome do filme ou série que deseja assistir."
            searchAdapter.updateItems(emptyList())
            return
        }

        val q = query.lowercase()
        val results = mutableListOf<MediaCardItem>()

        if (currentFilter == "all" || currentFilter == "movie") {
            for (m in allMovies) {
                if (m.name.lowercase().contains(q)) {
                    results.add(
                        MediaCardItem(
                            id = m.getStreamIdString(),
                            title = m.name,
                            posterUrl = m.streamIcon,
                            backdropUrl = m.streamIcon,
                            mediaType = "movie",
                            directSource = m.directSource,
                            tmdbId = m.getTmdbIdString(),
                            year = m.year
                        )
                    )
                }
            }
        }

        if (currentFilter == "all" || currentFilter == "series") {
            for (s in allSeries) {
                if (s.name.lowercase().contains(q)) {
                    results.add(
                        MediaCardItem(
                            id = s.getSeriesIdString(),
                            title = s.name,
                            posterUrl = s.cover,
                            backdropUrl = s.cover,
                            mediaType = "series",
                            tmdbId = s.getTmdbIdString(),
                            year = s.year,
                            subtitle = s.genre
                        )
                    )
                }
            }
        }

        if (results.isEmpty()) {
            binding.tvSearchResultsCount.visibility = View.GONE
            binding.layoutEmptySearch.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "Nenhum resultado encontrado"
            binding.tvEmptySubtitle.text = "Não encontramos títulos com o termo \"$query\"."
            searchAdapter.updateItems(emptyList())
        } else {
            binding.layoutEmptySearch.visibility = View.GONE
            binding.tvSearchResultsCount.visibility = View.VISIBLE
            binding.tvSearchResultsCount.text = "${results.size} título(s) encontrado(s)"
            searchAdapter.updateItems(results)
        }
    }

    private fun onResultClicked(item: MediaCardItem) {
        if (item.mediaType == "series") {
            startActivity(Intent(this, SeriesDetailActivity::class.java).apply {
                putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
                putExtra(AppConstants.EXTRA_TITLE, item.title)
                putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
                putExtra(AppConstants.EXTRA_TMDB_ID, item.tmdbId)
            })
        } else {
            startActivity(Intent(this, MovieDetailActivity::class.java).apply {
                putExtra(AppConstants.EXTRA_STREAM_ID, item.id)
                putExtra(AppConstants.EXTRA_TITLE, item.title)
                putExtra(AppConstants.EXTRA_POSTER_URL, item.posterUrl)
                putExtra(AppConstants.EXTRA_STREAM_URL, item.directSource)
                putExtra(AppConstants.EXTRA_TMDB_ID, item.tmdbId)
            })
        }
    }
}
