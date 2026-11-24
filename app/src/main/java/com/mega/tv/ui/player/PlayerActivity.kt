package com.mega.tv.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.mega.tv.R
import com.mega.tv.data.api.AppConstants
import com.mega.tv.data.local.WatchHistoryManager
import com.mega.tv.databinding.ActivityPlayerBinding
import java.util.Locale
import kotlin.math.abs

class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var watchManager: WatchHistoryManager
    private lateinit var audioManager: AudioManager

    private var player: ExoPlayer? = null

    private var streamId: String = ""
    private var streamUrl: String = ""
    private var contentTitle: String = ""
    private var contentSubtitle: String? = null
    private var posterUrl: String? = null
    private var mediaType: String = "movie"
    private var resumePositionMs: Long = 0L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isOverlayVisible = true

    private lateinit var gestureDetector: GestureDetector
    private var screenBrightness: Float = -1f

    private val hideOverlayRunnable = Runnable {
        hideOverlay()
    }

    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            updatePlaybackProgress()
            mainHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        watchManager = WatchHistoryManager.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        extractIntentData()
        setupUI()

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.overlayControls) { _, insets ->
            val cutouts = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.displayCutout() or androidx.core.view.WindowInsetsCompat.Type.navigationBars()
            )
            binding.playerTopBar.setPadding(
                cutouts.left + (24 * resources.displayMetrics.density).toInt(),
                (16 * resources.displayMetrics.density).toInt(),
                cutouts.right + (24 * resources.displayMetrics.density).toInt(),
                (20 * resources.displayMetrics.density).toInt()
            )
            binding.playerBottomBar.setPadding(
                cutouts.left + (24 * resources.displayMetrics.density).toInt(),
                0,
                cutouts.right + (24 * resources.displayMetrics.density).toInt(),
                cutouts.bottom + (16 * resources.displayMetrics.density).toInt()
            )
            insets
        }
        setupGestures()
        initializePlayer()
    }

    private fun extractIntentData() {
        streamId = intent.getStringExtra(AppConstants.EXTRA_STREAM_ID) ?: ""
        streamUrl = intent.getStringExtra(AppConstants.EXTRA_STREAM_URL) ?: ""
        contentTitle = intent.getStringExtra(AppConstants.EXTRA_TITLE) ?: ""
        contentSubtitle = intent.getStringExtra(AppConstants.EXTRA_SUBTITLE)
        posterUrl = intent.getStringExtra(AppConstants.EXTRA_POSTER_URL)
        mediaType = intent.getStringExtra(AppConstants.EXTRA_MEDIA_TYPE) ?: "movie"

        val extraPos = intent.getLongExtra(AppConstants.EXTRA_RESUME_POSITION, 0L)
        resumePositionMs = if (extraPos > 0) extraPos else watchManager.getSavedPosition(streamId)
    }

    private fun setupUI() {
        binding.tvPlayerTitle.text = contentTitle
        if (!contentSubtitle.isNullOrBlank()) {
            binding.tvPlayerSubtitle.text = contentSubtitle
            binding.tvPlayerSubtitle.visibility = View.VISIBLE
        } else {
            binding.tvPlayerSubtitle.visibility = View.GONE
        }

        binding.btnPlayerBack.setOnClickListener {
            resetOverlayTimeout()
            saveCurrentProgressAndExit()
        }

        binding.btnPlayPause.setOnClickListener {
            resetOverlayTimeout()
            togglePlayPause()
        }

        binding.btnRewind10.setOnClickListener {
            resetOverlayTimeout()
            seekRelative(-10000)
        }

        binding.btnForward10.setOnClickListener {
            resetOverlayTimeout()
            seekRelative(10000)
        }

        if (mediaType == "live") {
            binding.timeProgressContainer.visibility = View.GONE
            binding.btnRewind10.visibility = View.GONE
            binding.btnForward10.visibility = View.GONE
        }

        binding.playerSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val duration = player?.duration ?: 0L
                    if (duration > 0) {
                        val targetMs = (progress.toLong() * duration) / 1000L
                        player?.seekTo(targetMs)
                    }
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        showOverlay()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestures() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (isOverlayVisible) hideOverlay() else showOverlay()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val screenWidth = resources.displayMetrics.widthPixels
                if (e.x < screenWidth / 2) {
                    seekRelative(-10000)
                    showGestureFeedback("⏪ -10s")
                } else {
                    seekRelative(10000)
                    showGestureFeedback("⏩ +10s")
                }
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 == null) return false
                val screenWidth = resources.displayMetrics.widthPixels
                val screenHeight = resources.displayMetrics.heightPixels
                val deltaY = (e1.y - e2.y) / screenHeight

                if (abs(distanceY) > abs(distanceX)) {
                    if (e1.x < screenWidth / 2) {
                        // Left side: Brightness
                        adjustBrightness(deltaY)
                    } else {
                        // Right side: Volume
                        adjustVolumeDelta(deltaY)
                    }
                    return true
                }
                return false
            }
        })

        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        binding.overlayControls.setOnClickListener {
            hideOverlay()
        }
    }

    private fun adjustBrightness(delta: Float) {
        val lp = window.attributes
        if (screenBrightness < 0) {
            screenBrightness = lp.screenBrightness.takeIf { it >= 0 } ?: 0.5f
        }
        screenBrightness = (screenBrightness + delta * 0.08f).coerceIn(0.01f, 1.0f)
        lp.screenBrightness = screenBrightness
        window.attributes = lp

        val percent = (screenBrightness * 100).toInt()
        showGestureFeedback("☀️ Brilho: $percent%")
    }

    private fun adjustVolumeDelta(delta: Float) {
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val step = if (delta > 0) 1 else -1

        val newVol = (currentVol + step).coerceIn(0, maxVol)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)

        val percent = ((newVol.toDouble() / maxVol.toDouble()) * 100).toInt()
        showGestureFeedback("🔊 Volume: $percent%")
    }

    private fun showGestureFeedback(text: String) {
        binding.tvGestureText.text = text
        binding.gestureIndicator.visibility = View.VISIBLE
        mainHandler.removeCallbacksAndMessages(binding.gestureIndicator)
        mainHandler.postDelayed({
            binding.gestureIndicator.visibility = View.GONE
        }, 1200)
    }

    private fun initializePlayer() {
        if (streamUrl.isBlank()) {
            finish()
            return
        }

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("MEga-MobilePlayer/1.0")
            .setDefaultRequestProperties(
                mapOf(AppConstants.HEADER_PACKAGE_NAME to AppConstants.DEFAULT_PACKAGE_NAME)
            )
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
            .setAllowCrossProtocolRedirects(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(httpDataSourceFactory)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
            .apply {
                binding.playerView.player = this

                val mediaItem = MediaItem.fromUri(Uri.parse(streamUrl))
                setMediaItem(mediaItem)

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> {
                                binding.playerBufferProgress.visibility = View.VISIBLE
                            }
                            Player.STATE_READY -> {
                                binding.playerBufferProgress.visibility = View.GONE
                                updatePlayPauseIcon(isPlaying)
                            }
                            Player.STATE_ENDED -> {
                                binding.playerBufferProgress.visibility = View.GONE
                                onContentFinished()
                            }
                            Player.STATE_IDLE -> {
                                binding.playerBufferProgress.visibility = View.GONE
                            }
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        updatePlayPauseIcon(isPlaying)
                        if (!isPlaying && player?.playbackState != Player.STATE_ENDED) {
                            saveCurrentProgress()
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        binding.playerBufferProgress.visibility = View.GONE
                        error.printStackTrace()
                    }
                })

                prepare()
                if (resumePositionMs > 0 && mediaType != "live") {
                    seekTo(resumePositionMs)
                }
                playWhenReady = true
            }

        mainHandler.post(updateProgressRunnable)
    }

    private fun togglePlayPause() {
        val p = player ?: return
        if (p.isPlaying) {
            p.pause()
            saveCurrentProgress()
        } else {
            p.play()
        }
        updatePlayPauseIcon(p.isPlaying)
    }

    private fun seekRelative(deltaMs: Long) {
        if (mediaType == "live") return
        val p = player ?: return
        val current = p.currentPosition
        val duration = p.duration
        val newPos = (current + deltaMs).coerceIn(0L, duration.coerceAtLeast(0L))
        p.seekTo(newPos)
        updatePlaybackProgress()
    }

    private fun showOverlay() {
        isOverlayVisible = true
        binding.overlayControls.alpha = 0f
        binding.overlayControls.visibility = View.VISIBLE
        binding.overlayControls.animate().alpha(1f).setDuration(180).start()
        if (com.mega.tv.util.DeviceUtils.isTv(this)) {
            binding.btnPlayPause.requestFocus()
        }
        resetOverlayTimeout()
    }

    private fun hideOverlay() {
        isOverlayVisible = false
        binding.overlayControls.visibility = View.GONE
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                    togglePlayPause()
                    showOverlay()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (!isOverlayVisible) {
                        togglePlayPause()
                        showOverlay()
                        return true
                    }
                    resetOverlayTimeout()
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!isOverlayVisible) {
                        seekRelative(-10000)
                        showGestureFeedback("⏪ -10s")
                        showOverlay()
                        return true
                    }
                    resetOverlayTimeout()
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (!isOverlayVisible) {
                        seekRelative(10000)
                        showGestureFeedback("⏩ +10s")
                        showOverlay()
                        return true
                    }
                    resetOverlayTimeout()
                }
                KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (!isOverlayVisible) {
                        showOverlay()
                        return true
                    }
                    resetOverlayTimeout()
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (isOverlayVisible) {
                        hideOverlay()
                    } else {
                        saveCurrentProgressAndExit()
                    }
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun resetOverlayTimeout() {
        mainHandler.removeCallbacks(hideOverlayRunnable)
        if (player?.isPlaying == true) {
            mainHandler.postDelayed(hideOverlayRunnable, 4000)
        }
    }

    private fun updatePlayPauseIcon(isPlaying: Boolean) {
        binding.btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
    }

    private fun updatePlaybackProgress() {
        val p = player ?: return
        val current = p.currentPosition.coerceAtLeast(0L)
        val duration = p.duration.coerceAtLeast(0L)

        binding.tvPlayerCurrentTime.text = formatTime(current)
        binding.tvPlayerTotalTime.text = formatTime(duration)

        if (duration > 0) {
            val progress = ((current.toDouble() / duration.toDouble()) * 1000).toInt()
            binding.playerSeekBar.progress = progress
        }
    }

    private fun formatTime(timeMs: Long): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    private fun saveCurrentProgress() {
        if (mediaType == "live" || streamId.isBlank()) return
        val p = player ?: return
        val current = p.currentPosition
        val duration = p.duration

        if (current > 5000 && duration > 0) {
            watchManager.saveProgress(
                streamId = streamId,
                mediaType = mediaType,
                title = contentTitle,
                subtitle = contentSubtitle,
                posterUrl = posterUrl,
                directSource = streamUrl,
                positionMs = current,
                durationMs = duration
            )
        }
    }

    private fun onContentFinished() {
        if (streamId.isNotBlank()) {
            watchManager.removeItem(streamId)
        }
        finish()
    }

    private fun saveCurrentProgressAndExit() {
        saveCurrentProgress()
        finish()
    }

    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
        player?.pause()
    }

    override fun onStop() {
        super.onStop()
        saveCurrentProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        mainHandler.removeCallbacksAndMessages(null)
        player?.release()
        player = null
    }
}
