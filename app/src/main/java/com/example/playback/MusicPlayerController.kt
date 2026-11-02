package com.example.playback

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.PowerManager
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.AudioQuality
import com.example.data.model.PlaybackMode
import com.example.data.model.Track
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(UnstableApi::class)
class MusicPlayerController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var retryJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    // Tracks whether the user intends for playback to be active (used for continuous auto-retry)
    private var userWantsPlaying: Boolean = false
    private var lastKnownPositionMs: Long = 0L

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    // Direct, unfiltered Iranian music CDN fallback streams (used only if a track has an empty/legacy URL)
    private val fallbackIranStreams = listOf(
        "https://dls.musics-fa.com/song/alibz/2026/08/Mohsen%20Chavoshi%20-%20Roode%20Sookhte%20Musics-Fa.mp3",
        "https://dls.musics-fa.com/song/alibz/2026/10/Homayoun%20Shajarian%20-%20Mitaravad%20Mahtab%20Musics-Fa.mp3",
        "https://dls.musics-fa.com/song/alibz/2026/10/Sirvan%20Khosravi%20-%20Midoonam%20Eshtebas%20Musics-Fa.mp3",
        "https://dls.musics-fa.com/song/alibz/2026/10/Behnam%20Bani%20-%20Cheshat%20Mige%20Musics-Fa.mp3",
        "https://dls.musics-fa.com/tagdl/downloads/Mohsen%20Yeganeh%20-%20Behet%20Ghol%20Midam%20(128).mp3",
        "https://dls.musics-fa.com/song/alibz/dlswm/MohammadReza%20Shajaryan%20-%20Ey%20Asheghan%20(320).mp3"
    )

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Generous buffer (up to 90 seconds) so lock-screen & weak mobile networks never underrun
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                20_000,
                90_000,
                1_500,
                3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
            )
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(12_000)
            .setReadTimeoutMs(15_000)

        // Continuous internal chunk retry policy so transient network drops auto-recover mid-stream
        val continuousLoadErrorPolicy = object : DefaultLoadErrorHandlingPolicy(Int.MAX_VALUE) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                return 1_500L
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return Int.MAX_VALUE
            }
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(continuousLoadErrorPolicy)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK) // Crucial: prevents OS from sleeping CPU/Wi-Fi after 1 min in background
            .setHandleAudioBecomingNoisy(true)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _isRetrying = MutableStateFlow(false)
    val isRetrying: StateFlow<Boolean> = _isRetrying.asStateFlow()

    private val _retryCount = MutableStateFlow(0)
    val retryCount: StateFlow<Int> = _retryCount.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _playbackMode = MutableStateFlow(PlaybackMode.REPEAT_ALL)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _audioQuality = MutableStateFlow(AudioQuality.HIGH)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    private val _iranNoVpnMode = MutableStateFlow(true)
    val iranNoVpnMode: StateFlow<Boolean> = _iranNoVpnMode.asStateFlow()

    var onTrackPlayedListener: ((Track) -> Unit)? = null

    init {
        initLocks()
        registerNetworkCallback()
    }

    private fun initLocks() {
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "MeloGroove::MediaPlaybackWakeLock"
            )?.apply {
                setReferenceCounted(false)
            }

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val wifiLockMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                @Suppress("DEPRECATION")
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager?.createWifiLock(wifiLockMode, "MeloGroove::MediaWifiLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (_: Exception) {
        }
    }

    private fun acquirePlaybackLocks() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(4 * 60 * 60 * 1000L) // Up to 4 hours continuous background playback
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (_: Exception) {
        }
    }

    private fun releasePlaybackLocks() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            if (wifiLock?.isHeld == true) {
                wifiLock?.release()
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Ensures MediaPlaybackService is started and bound via MediaController so Android OS
     * promotes it to a Foreground Service with Lock Screen & Notification Bar controls.
     */
    fun ensurePlaybackServiceStarted() {
        try {
            val serviceIntent = Intent(context, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (_: Exception) {
            try {
                context.startService(Intent(context, MediaPlaybackService::class.java))
            } catch (_: Exception) {
            }
        }

        if (controllerFuture == null) {
            try {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, MediaPlaybackService::class.java)
                )
                controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            } catch (_: Exception) {
            }
        }
    }

    /**
     * Automatically detects when internet connectivity returns and immediately resumes
     * downloading/playing the current song from the exact position where it stalled.
     */
    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        scope.launch {
                            val track = _currentTrack.value
                            if (track != null && userWantsPlaying &&
                                (_isRetrying.value || exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null)
                            ) {
                                attemptResumeCurrentTrack(track)
                            }
                        }
                    }
                }
            )
        } catch (_: Exception) {
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                _isBuffering.value = false
                _isRetrying.value = false
                _retryCount.value = 0
                retryJob?.cancel()
                acquirePlaybackLocks()
                startProgressTracker()
            } else {
                stopProgressTracker()
                if (!userWantsPlaying && !_isBuffering.value) {
                    releasePlaybackLocks()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _isBuffering.value = true
                    if (userWantsPlaying) {
                        acquirePlaybackLocks()
                    }
                }
                Player.STATE_READY -> {
                    _isBuffering.value = false
                    _isRetrying.value = false
                    _retryCount.value = 0
                    retryJob?.cancel()
                    val duration = exoPlayer.duration
                    if (duration > 0) {
                        _durationMs.value = duration
                    }
                    if (userWantsPlaying && !exoPlayer.isPlaying) {
                        exoPlayer.play()
                    }
                }
                Player.STATE_ENDED -> {
                    _isBuffering.value = false
                    _isRetrying.value = false
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    if (userWantsPlaying && _currentTrack.value != null && exoPlayer.playerError != null) {
                        _isBuffering.value = true
                    } else {
                        _isBuffering.value = false
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val track = _currentTrack.value ?: return
            // Save the furthest valid playback position so we resume right where the song paused
            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (currentPos > 0L) {
                lastKnownPositionMs = maxOf(lastKnownPositionMs, currentPos)
            }
            userWantsPlaying = true
            _isBuffering.value = true
            _isRetrying.value = true
            startContinuousRetryLoop(track)
        }
    }

    /**
     * Continuously retries preparing & playing the current track every 2.5 seconds
     * until the download/network issue resolves, then automatically continues from lastKnownPositionMs.
     */
    private fun startContinuousRetryLoop(track: Track) {
        retryJob?.cancel()
        retryJob = scope.launch {
            while (isActive && userWantsPlaying && _currentTrack.value?.id == track.id) {
                _retryCount.value += 1
                _isBuffering.value = true
                _isRetrying.value = true

                attemptResumeCurrentTrack(track)

                delay(2_500L)

                if (exoPlayer.isPlaying || (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.playerError == null)) {
                    _isRetrying.value = false
                    _isBuffering.value = false
                    _retryCount.value = 0
                    break
                }
            }
        }
    }

    private fun attemptResumeCurrentTrack(track: Track) {
        val resumePos = maxOf(lastKnownPositionMs, _currentPositionMs.value, exoPlayer.currentPosition)
            .coerceAtLeast(0L)
        val cleanStreamUrl = sanitizeStreamUrl(track)
        val uri = Uri.parse(cleanStreamUrl)

        if (exoPlayer.mediaItemCount == 0) {
            val mediaItem = buildMediaItem(track, uri)
            exoPlayer.setMediaItem(mediaItem, resumePos)
        } else {
            if (resumePos > 0L) {
                exoPlayer.seekTo(resumePos)
            }
        }
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    fun setIranNoVpnMode(enabled: Boolean) {
        _iranNoVpnMode.value = enabled
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        ensurePlaybackServiceStarted()
        acquirePlaybackLocks()

        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            _currentIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = listOf(track) + _queue.value
            _currentIndex.value = 0
        } else {
            _currentIndex.value = _queue.value.indexOfFirst { it.id == track.id }
        }

        retryJob?.cancel()
        userWantsPlaying = true
        lastKnownPositionMs = 0L
        _isRetrying.value = false
        _retryCount.value = 0
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L
        _isBuffering.value = true

        val cleanStreamUrl = sanitizeStreamUrl(track)
        startExoPlayerWithUri(track, Uri.parse(cleanStreamUrl), 0L)

        onTrackPlayedListener?.invoke(track)
    }

    private fun sanitizeStreamUrl(track: Track): String {
        val raw = track.previewUrl.trim()
        if (raw.isEmpty() ||
            raw.contains("soundhelix.com", ignoreCase = true) ||
            raw.contains("archive.org", ignoreCase = true)
        ) {
            return fallbackIranStreams[abs(track.id.toInt()) % fallbackIranStreams.size]
        }
        return raw.replace(" ", "%20")
    }

    private fun loadBundledArtworkBytes(artworkUrl: String, trackId: Long): ByteArray? {
        return try {
            val assetPath = if (artworkUrl.startsWith("file:///android_asset/")) {
                artworkUrl.removePrefix("file:///android_asset/")
            } else {
                val bundledIds = listOf(
                    1013, 1014, 1015, 1016, 1017, 1018, 1019, 1020, 1021,
                    1023, 1024, 1025, 1026, 1027, 1001, 1002, 1003, 1004,
                    1005, 1006, 1007, 1008, 1009, 1010, 1011, 1012
                )
                val fallbackId = bundledIds[abs(trackId.toInt()) % bundledIds.size]
                "covers/cover_$fallbackId.jpg"
            }
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    private fun buildMediaItem(track: Track, uri: Uri): MediaItem {
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setDisplayTitle(track.title)
            .setSubtitle(track.artist)

        val artworkBytes = loadBundledArtworkBytes(track.artworkUrl, track.id)
        if (artworkBytes != null) {
            metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        }
        if (track.artworkUrl.startsWith("http")) {
            metadataBuilder.setArtworkUri(Uri.parse(track.artworkUrl))
        }

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun startExoPlayerWithUri(track: Track, uri: Uri, startPositionMs: Long = 0L) {
        val mediaItem = buildMediaItem(track, uri)
        exoPlayer.setMediaItem(mediaItem, startPositionMs)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    fun playPauseToggle() {
        if (exoPlayer.isPlaying || (_isRetrying.value && userWantsPlaying)) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Completely stops playback, cancels retries, clears active track and hides player bar
     */
    fun stopAndClearPlayback() {
        userWantsPlaying = false
        retryJob?.cancel()
        retryJob = null
        stopProgressTracker()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        releasePlaybackLocks()
        _isPlaying.value = false
        _isBuffering.value = false
        _isRetrying.value = false
        _retryCount.value = 0
        lastKnownPositionMs = 0L
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _currentTrack.value = null
    }

    fun removeTrackFromQueue(trackId: Long) {
        val currentQ = _queue.value.toMutableList()
        val removedIndex = currentQ.indexOfFirst { it.id == trackId }
        if (removedIndex == -1) return

        val wasCurrent = _currentTrack.value?.id == trackId
        currentQ.removeAt(removedIndex)
        _queue.value = currentQ

        if (currentQ.isEmpty()) {
            stopAndClearPlayback()
        } else if (wasCurrent) {
            val nextIdx = removedIndex.coerceAtMost(currentQ.size - 1)
            _currentIndex.value = nextIdx
            playTrack(currentQ[nextIdx], currentQ)
        } else {
            _currentIndex.value = currentQ.indexOfFirst { it.id == _currentTrack.value?.id }.coerceAtLeast(0)
        }
    }

    fun pause() {
        userWantsPlaying = false
        retryJob?.cancel()
        _isRetrying.value = false
        _isBuffering.value = false
        exoPlayer.playWhenReady = false
        exoPlayer.pause()
        releasePlaybackLocks()
    }

    fun resume() {
        userWantsPlaying = true
        ensurePlaybackServiceStarted()
        acquirePlaybackLocks()
        val track = _currentTrack.value
        if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null) {
            if (track != null) {
                attemptResumeCurrentTrack(track)
            }
        } else {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val safePos = positionMs.coerceAtLeast(0L)
        lastKnownPositionMs = safePos
        _currentPositionMs.value = safePos
        exoPlayer.seekTo(safePos)
        val track = _currentTrack.value
        if (userWantsPlaying && track != null && (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null)) {
            attemptResumeCurrentTrack(track)
        }
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = when (_playbackMode.value) {
            PlaybackMode.SHUFFLE -> if (q.size > 1) {
                val candidates = q.indices.filter { it != _currentIndex.value }
                candidates.random()
            } else 0
            PlaybackMode.REPEAT_ONE -> (_currentIndex.value + 1) % q.size
            PlaybackMode.REPEAT_ALL -> (_currentIndex.value + 1) % q.size
            PlaybackMode.NORMAL -> (_currentIndex.value + 1) % q.size
        }
        _currentIndex.value = nextIndex
        playTrack(q[nextIndex])
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (exoPlayer.currentPosition > 4_000L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (_currentIndex.value - 1 >= 0) _currentIndex.value - 1 else q.size - 1
        _currentIndex.value = prevIndex
        playTrack(q[prevIndex])
    }

    private fun handleTrackEnded() {
        val q = _queue.value
        if (q.isEmpty()) return

        when (_playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> {
                lastKnownPositionMs = 0L
                seekTo(0L)
                exoPlayer.play()
            }
            PlaybackMode.REPEAT_ALL -> {
                val nextIndex = (_currentIndex.value + 1) % q.size
                _currentIndex.value = nextIndex
                playTrack(q[nextIndex])
            }
            PlaybackMode.SHUFFLE -> {
                val nextIndex = (q.indices).random()
                _currentIndex.value = nextIndex
                playTrack(q[nextIndex])
            }
            PlaybackMode.NORMAL -> {
                if (_currentIndex.value + 1 < q.size) {
                    val nextIndex = _currentIndex.value + 1
                    _currentIndex.value = nextIndex
                    playTrack(q[nextIndex])
                } else {
                    userWantsPlaying = false
                    _isPlaying.value = false
                    lastKnownPositionMs = 0L
                    seekTo(0L)
                    exoPlayer.pause()
                    releasePlaybackLocks()
                }
            }
        }
    }

    fun togglePlaybackMode() {
        _playbackMode.value = when (_playbackMode.value) {
            PlaybackMode.REPEAT_ALL -> PlaybackMode.REPEAT_ONE
            PlaybackMode.REPEAT_ONE -> PlaybackMode.SHUFFLE
            PlaybackMode.SHUFFLE -> PlaybackMode.NORMAL
            PlaybackMode.NORMAL -> PlaybackMode.REPEAT_ALL
        }
    }

    fun setPlaybackMode(mode: PlaybackMode) {
        _playbackMode.value = mode
    }

    fun setAudioQuality(quality: AudioQuality) {
        _audioQuality.value = quality
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (exoPlayer.isPlaying) {
                    val pos = exoPlayer.currentPosition.coerceAtLeast(0L)
                    _currentPositionMs.value = pos
                    if (pos > 0L) {
                        lastKnownPositionMs = pos
                    }
                    val dur = exoPlayer.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        userWantsPlaying = false
        retryJob?.cancel()
        stopProgressTracker()
        releasePlaybackLocks()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    companion object {
        @Volatile
        private var instance: MusicPlayerController? = null

        fun getInstance(context: Context): MusicPlayerController {
            return instance ?: synchronized(this) {
                instance ?: MusicPlayerController(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
