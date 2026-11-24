package com.mega.tv.ui.channels

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.mega.tv.data.api.AppConstants
import com.mega.tv.data.api.XtreamClient
import com.mega.tv.data.local.CatalogCacheManager
import com.mega.tv.data.local.PreferencesManager
import com.mega.tv.data.model.LiveCategory
import com.mega.tv.data.model.LiveStream
import com.mega.tv.data.model.SeriesSeason
import com.mega.tv.databinding.ActivityChannelsBinding
import com.mega.tv.ui.details.SeasonAdapter
import com.mega.tv.ui.player.PlayerActivity
import com.mega.tv.util.DeviceUtils
import com.mega.tv.util.SystemInsetsUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChannelsBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var catalogCache: CatalogCacheManager
    private var xtreamClient: XtreamClient? = null

    private lateinit var channelsAdapter: ChannelsAdapter
    private lateinit var categoryAdapter: SeasonAdapter
    private var categories: List<LiveCategory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DeviceUtils.applyOrientation(this)

        binding = ActivityChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        SystemInsetsUtils.setupEdgeToEdge(
            activity = this,
            rootView = binding.root
        )

        prefs = PreferencesManager.getInstance(this)
        catalogCache = CatalogCacheManager.getInstance(this)
        val xtreamUrl = prefs.getXtreamUrl()
        if (!xtreamUrl.isNullOrBlank()) {
            xtreamClient = XtreamClient(xtreamUrl)
        }

        setupUI()
        loadChannelsData()
    }

    private fun setupUI() {
        binding.btnChannelsBack.setOnClickListener { finish() }

        categoryAdapter = SeasonAdapter(emptyList()) { categorySeason ->
            val cat = categories.find { it.categoryId == categorySeason.seasonNumber.toString() }
            loadStreamsForCategory(cat?.categoryId)
        }
        binding.rvLiveCategories.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvLiveCategories.adapter = categoryAdapter

        channelsAdapter = ChannelsAdapter(emptyList()) { stream ->
            launchLivePlayer(stream)
        }
        val columns = if (DeviceUtils.isTv(this)) 5 else 3
        binding.rvChannelsGrid.layoutManager = GridLayoutManager(this, columns)
        binding.rvChannelsGrid.adapter = channelsAdapter
    }

    private fun loadChannelsData() {
        binding.channelsProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            // Instant display from disk cache if present
            val cachedCats = withContext(Dispatchers.IO) { catalogCache.getLiveCategories() }
            if (cachedCats.isNotEmpty()) {
                bindCategories(cachedCats)
                loadStreamsForCategory(cachedCats.first().categoryId)
            }

            val client = xtreamClient ?: return@launch
            val catsRes = client.getLiveCategories()
            catsRes.onSuccess { cats ->
                withContext(Dispatchers.IO) { catalogCache.saveLiveCategories(cats) }
                bindCategories(cats)
                loadStreamsForCategory(cats.firstOrNull()?.categoryId)
            }
        }
    }

    private fun bindCategories(cats: List<LiveCategory>) {
        categories = cats
        val seasonItems = cats.mapIndexed { index, cat ->
            SeriesSeason(
                seasonNumber = cat.categoryId.toIntOrNull() ?: (index + 1),
                name = cat.categoryName
            )
        }
        categoryAdapter.updateSeasons(seasonItems, seasonItems.firstOrNull()?.seasonNumber ?: 1)
    }

    private fun loadStreamsForCategory(categoryId: String?) {
        binding.channelsProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val cachedStreams = withContext(Dispatchers.IO) { catalogCache.getLiveStreams() }
            if (cachedStreams.isNotEmpty() && !categoryId.isNullOrBlank()) {
                val filtered = cachedStreams.filter { it.categoryId == categoryId }
                if (filtered.isNotEmpty()) {
                    channelsAdapter.updateChannels(filtered)
                    binding.channelsProgressBar.visibility = View.GONE
                }
            }

            val client = xtreamClient ?: return@launch
            val streamsRes = client.getLiveStreams(categoryId)
            streamsRes.onSuccess { list ->
                channelsAdapter.updateChannels(list)
                if (DeviceUtils.isTv(this@ChannelsActivity)) {
                    binding.rvChannelsGrid.post { binding.rvChannelsGrid.getChildAt(0)?.requestFocus() }
                }
            }
            binding.channelsProgressBar.visibility = View.GONE
        }
    }

    private fun launchLivePlayer(stream: LiveStream) {
        val url = stream.directSource ?: return
        val sanitizedUrl = xtreamClient?.sanitizePlaybackUrl(url) ?: url

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra(AppConstants.EXTRA_STREAM_ID, stream.getStreamIdString())
            putExtra(AppConstants.EXTRA_STREAM_URL, sanitizedUrl)
            putExtra(AppConstants.EXTRA_TITLE, stream.name)
            putExtra(AppConstants.EXTRA_POSTER_URL, stream.streamIcon)
            putExtra(AppConstants.EXTRA_MEDIA_TYPE, "live")
        }
        startActivity(intent)
    }
}
