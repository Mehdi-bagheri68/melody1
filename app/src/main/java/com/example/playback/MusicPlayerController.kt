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
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.model.AudioQuality
import com.example.data.model.ErrorCategory
import com.example.data.model.PlaybackError
import com.example.data.model.PlaybackMode
import com.example.data.model.Track
import com.example.data.repository.SourceResolver
import com.example.util.AppLogger
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
import java.io.IOException
import kotlin.math.min

@OptIn(UnstableApi::class)
class MusicPlayerController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var progressJob: Job? = null
    private var retryJob: Job? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private var userWantsPlaying: Boolean = false
    private var lastKnownPositionMs: Long = 0L
    private var activeStreamUrl: String = ""
    private var fallbackSameTrackUrl: String? = null
    private var triedSameTrackFallbackUrl: Boolean = false

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private var registeredNetworkCallback: ConnectivityManager.NetworkCallback? = null

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

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

        // Fixes BUG-02: Coordinated Media3 LoadErrorHandlingPolicy.
        // Fails fast (C.TIME_UNSET) on permanent HTTP 401/403/404/410 or format errors,
        // and performs at most 2 quick internal chunk retries on transient network glitches
        // before handing control to the app-level exponential backoff classifier.
        val smartLoadErrorPolicy = object : DefaultLoadErrorHandlingPolicy(2) {
            override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
                val ex = loadErrorInfo.exception
                if (ex is ParserException || ex is UnrecognizedInputFormatException) {
                    return C.TIME_UNSET
                }
                if (ex is HttpDataSource.InvalidResponseCodeException) {
                    val code = ex.responseCode
                    if (code in listOf(400, 401, 403, 404, 410, 451)) {
                        return C.TIME_UNSET
                    }
                    if (code == 429) {
                        return parseRetryAfterMs(ex.headerFields) ?: 5_000L
                    }
                }
                return if (loadErrorInfo.errorCount <= 2) {
                    1_000L * loadErrorInfo.errorCount
                } else {
                    C.TIME_UNSET
                }
            }

            override fun getMinimumLoadableRetryCount(dataType: Int): Int = 2
        }

        val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(smartLoadErrorPolicy)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setWakeMode(C.WAKE_MODE_NETWORK)
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

    private val _playbackError = MutableStateFlow<PlaybackError?>(null)
    val playbackError: StateFlow<PlaybackError?> = _playbackError.asStateFlow()

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

    private val _hasQualityVariant = MutableStateFlow(true)
    val hasQualityVariant: StateFlow<Boolean> = _hasQualityVariant.asStateFlow()

    private val _effectiveBitrateLabel = MutableStateFlow("320 kbps")
    val effectiveBitrateLabel: StateFlow<String> = _effectiveBitrateLabel.asStateFlow()

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
        } catch (e: Exception) {
            AppLogger.w("initLocks", "Could not initialize WakeLock/WifiLock", e)
        }
    }

    // Fixes BUG-11: Bounded 30-minute WakeLock timeout refreshed only during active playback
    private fun acquirePlaybackLocks() {
        try {
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire(30 * 60 * 1000L)
            }
            if (wifiLock?.isHeld == false) {
                wifiLock?.acquire()
            }
        } catch (e: Exception) {
            AppLogger.w("acquirePlaybackLocks", "Failed acquiring playback locks", e)
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
        } catch (e: Exception) {
            AppLogger.w("releasePlaybackLocks", "Failed releasing playback locks", e)
        }
    }

    fun ensurePlaybackServiceStarted() {
        try {
            val serviceIntent = Intent(context, MediaPlaybackService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            AppLogger.w("ensurePlaybackServiceStarted", "startForegroundService fallback", e)
            try {
                context.startService(Intent(context, MediaPlaybackService::class.java))
            } catch (inner: Exception) {
                AppLogger.w("ensurePlaybackServiceStarted", "startService failed", inner)
            }
        }

        if (controllerFuture == null) {
            try {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, MediaPlaybackService::class.java)
                )
                controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
            } catch (e: Exception) {
                AppLogger.w("ensurePlaybackServiceStarted", "MediaController buildAsync failed", e)
            }
        }
    }

    // Fixes BUG-11: Stores callback reference and unregisters in release()
    private fun registerNetworkCallback() {
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    scope.launch {
                        val track = _currentTrack.value
                        val currentErr = _playbackError.value
                        val canAutoRecover = currentErr == null || currentErr.recoverable
                        if (track != null && userWantsPlaying && canAutoRecover &&
                            (_isRetrying.value || exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null)
                        ) {
                            _retryCount.value = 0
                            _playbackError.value = null
                            attemptResumeCurrentTrack(track, activeStreamUrl)
                        }
                    }
                }
            }
            connectivityManager.registerNetworkCallback(request, callback)
            registeredNetworkCallback = callback
        } catch (e: Exception) {
            AppLogger.w("registerNetworkCallback", "Could not register network callback", e)
        }
    }

    private fun unregisterNetworkCallback() {
        val callback = registeredNetworkCallback ?: return
        try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            connectivityManager?.unregisterNetworkCallback(callback)
        } catch (e: Exception) {
            AppLogger.w("unregisterNetworkCallback", "Could not unregister network callback", e)
        } finally {
            registeredNetworkCallback = null
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                _isBuffering.value = false
                _isRetrying.value = false
                _retryCount.value = 0
                _playbackError.value = null
                retryJob?.cancel()
                if (activeStreamUrl.isNotBlank()) {
                    SourceResolver.recordSuccess(activeStreamUrl)
                }
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
                    _playbackError.value = null
                    retryJob?.cancel()
                    // Fixes BUG-05: Always sync real duration from Media3 once prepared
                    val realDuration = exoPlayer.duration
                    if (realDuration > 0L && realDuration != C.TIME_UNSET) {
                        _durationMs.value = realDuration
                        _currentTrack.value?.let { current ->
                            if (current.durationMs != realDuration) {
                                _currentTrack.value = current.copy(durationMs = realDuration)
                            }
                        }
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
                    if (!userWantsPlaying || exoPlayer.playerError == null) {
                        _isBuffering.value = false
                        releasePlaybackLocks()
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val track = _currentTrack.value ?: return
            if (!userWantsPlaying) return

            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            if (currentPos > 0L) {
                lastKnownPositionMs = maxOf(lastKnownPositionMs, currentPos)
            }

            val classified = classifyPlaybackError(error, track, activeStreamUrl)
            AppLogger.w("onPlayerError", "Track=${track.id} code=${classified.code} recoverable=${classified.recoverable}", error)

            // Fixes BUG-01 & BUG-02:
            // If non-recoverable (e.g. HTTP 404 on 320kbps URL), try the SAME track's 128kbps fallback URL once if available.
            val altSameTrackUrl = fallbackSameTrackUrl
            if (!classified.recoverable && !triedSameTrackFallbackUrl &&
                !altSameTrackUrl.isNullOrBlank() && altSameTrackUrl != activeStreamUrl
            ) {
                triedSameTrackFallbackUrl = true
                activeStreamUrl = altSameTrackUrl
                AppLogger.d("onPlayerError", "Trying same-track alternate bitrate variant for track=${track.id}")
                attemptResumeCurrentTrack(track, activeStreamUrl)
                return
            }

            if (!classified.recoverable) {
                // Permanent source/format error -> Stop infinite retry immediately and inform user!
                SourceResolver.recordFailure(activeStreamUrl, isHardFailure = true)
                retryJob?.cancel()
                _isRetrying.value = false
                _isBuffering.value = false
                _isPlaying.value = false
                _playbackError.value = classified
                releasePlaybackLocks()
            } else {
                // Transient network / 5xx / 429 error -> Exponential backoff with bounded maxAttempts
                SourceResolver.recordFailure(activeStreamUrl, isHardFailure = false)
                startExponentialBackoffRetry(track, classified)
            }
        }
    }

    /**
     * Classifies [PlaybackException] into permanent vs. transient categories (Fixes BUG-02).
     */
    internal fun classifyPlaybackError(
        error: PlaybackException,
        track: Track,
        sourceUrl: String
    ): PlaybackError {
        val cause = error.cause

        if (cause is ParserException || cause is UnrecognizedInputFormatException ||
            error.errorCode in listOf(
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED
            )
        ) {
            return PlaybackError(
                code = "DECODER_FORMAT_${error.errorCode}",
                category = ErrorCategory.DECODER_FORMAT,
                sourceUrl = sourceUrl,
                recoverable = false,
                userMessageFa = "فرمت فایل صوتی «${track.title}» قابل پخش نیست.",
                userMessageEn = "Audio format for \"${track.title}\" is not supported."
            )
        }

        if (cause is HttpDataSource.InvalidResponseCodeException) {
            val status = cause.responseCode
            return when {
                status in listOf(401, 403, 404, 410, 451) -> PlaybackError(
                    code = "HTTP_$status",
                    category = ErrorCategory.SOURCE_NOT_FOUND,
                    httpStatusCode = status,
                    sourceUrl = sourceUrl,
                    recoverable = false,
                    userMessageFa = "فایل صوتی این آهنگ روی سرور یافت نشد (خطای $status).",
                    userMessageEn = "Audio file not found or access denied (HTTP $status)."
                )
                status == 429 -> PlaybackError(
                    code = "HTTP_429",
                    category = ErrorCategory.RATE_LIMITED,
                    httpStatusCode = 429,
                    sourceUrl = sourceUrl,
                    recoverable = true,
                    maxAttempts = MAX_RETRY_ATTEMPTS,
                    userMessageFa = "ترافیک سرور بالاست (۴۲۹)؛ در حال تلاش مجدد هوشمند...",
                    userMessageEn = "Server rate-limited (429); retrying with backoff..."
                )
                status in 500..599 -> PlaybackError(
                    code = "HTTP_$status",
                    category = ErrorCategory.NETWORK_TRANSIENT,
                    httpStatusCode = status,
                    sourceUrl = sourceUrl,
                    recoverable = true,
                    maxAttempts = MAX_RETRY_ATTEMPTS,
                    userMessageFa = "خطای موقت سرور ($status)؛ در حال تلاش مجدد...",
                    userMessageEn = "Temporary server error ($status); retrying..."
                )
                else -> PlaybackError(
                    code = "HTTP_$status",
                    category = ErrorCategory.SOURCE_NOT_FOUND,
                    httpStatusCode = status,
                    sourceUrl = sourceUrl,
                    recoverable = false,
                    userMessageFa = "خطا در دریافت فایل صوتی (کد $status).",
                    userMessageEn = "Failed to load audio stream (HTTP $status)."
                )
            }
        }

        if (cause is HttpDataSource.HttpDataSourceException || cause is IOException ||
            error.errorCode in listOf(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
            )
        ) {
            return PlaybackError(
                code = "NETWORK_IO_${error.errorCode}",
                category = ErrorCategory.NETWORK_TRANSIENT,
                sourceUrl = sourceUrl,
                recoverable = true,
                maxAttempts = MAX_RETRY_ATTEMPTS,
                userMessageFa = "اختلال در اتصال اینترنت؛ در حال تلاش مجدد خودکار...",
                userMessageEn = "Network connection interrupted; auto-retrying..."
            )
        }

        return PlaybackError(
            code = "PLAYER_${error.errorCode}",
            category = ErrorCategory.UNKNOWN,
            sourceUrl = sourceUrl,
            recoverable = false,
            userMessageFa = "خطا در پخش آهنگ «${track.title}».",
            userMessageEn = "Playback error for \"${track.title}\"."
        )
    }

    /**
     * Fixes BUG-02: Exponential backoff retry loop with a strict upper bound ([MAX_RETRY_ATTEMPTS]).
     */
    private fun startExponentialBackoffRetry(track: Track, initialError: PlaybackError) {
        retryJob?.cancel()
        retryJob = scope.launch {
            var attempt = _retryCount.value
            while (isActive && userWantsPlaying && _currentTrack.value?.id == track.id && attempt < MAX_RETRY_ATTEMPTS) {
                attempt += 1
                _retryCount.value = attempt
                _isBuffering.value = true
                _isRetrying.value = true
                _playbackError.value = initialError.copy(
                    attemptCount = attempt,
                    maxAttempts = MAX_RETRY_ATTEMPTS,
                    timestampMs = System.currentTimeMillis()
                )

                val baseDelayMs = if (initialError.category == ErrorCategory.RATE_LIMITED) 4_000L else 1_500L
                val backoffMs = min(baseDelayMs * (1L shl (attempt - 1).coerceAtMost(4)), 15_000L)
                delay(backoffMs)

                if (!isActive || !userWantsPlaying || _currentTrack.value?.id != track.id) {
                    break
                }

                attemptResumeCurrentTrack(track, activeStreamUrl)

                delay(2_200L)

                if (exoPlayer.isPlaying || (exoPlayer.playbackState == Player.STATE_READY && exoPlayer.playerError == null)) {
                    _isRetrying.value = false
                    _isBuffering.value = false
                    _retryCount.value = 0
                    _playbackError.value = null
                    break
                }
            }

            // If max attempts reached and still failing, stop looping and present manual retry option
            if (isActive && userWantsPlaying && _currentTrack.value?.id == track.id &&
                !exoPlayer.isPlaying && exoPlayer.playerError != null && _retryCount.value >= MAX_RETRY_ATTEMPTS
            ) {
                _isRetrying.value = false
                _isBuffering.value = false
                _playbackError.value = initialError.copy(
                    attemptCount = MAX_RETRY_ATTEMPTS,
                    maxAttempts = MAX_RETRY_ATTEMPTS,
                    userMessageFa = "اتصال پس از $MAX_RETRY_ATTEMPTS تلاش برقرار نشد. برای تلاش مجدد لمس کنید.",
                    userMessageEn = "Connection failed after $MAX_RETRY_ATTEMPTS attempts. Tap Retry to try again.",
                    timestampMs = System.currentTimeMillis()
                )
                releasePlaybackLocks()
            }
        }
    }

    /**
     * Manual retry triggered by user button in MiniPlayer or FullPlayerSheet (Fixes BUG-02 & UI-07).
     */
    fun retryCurrentTrackManually() {
        val track = _currentTrack.value ?: return
        retryJob?.cancel()
        userWantsPlaying = true
        triedSameTrackFallbackUrl = false
        _retryCount.value = 0
        _playbackError.value = null
        _isRetrying.value = false
        _isBuffering.value = true

        when (val resolution = SourceResolver.resolveTrackStream(track, _audioQuality.value, _iranNoVpnMode.value)) {
            is SourceResolver.StreamResolution.Available -> {
                activeStreamUrl = resolution.primaryUrl
                fallbackSameTrackUrl = resolution.fallbackSameTrackUrl
                _hasQualityVariant.value = resolution.hasMultiBitrateVariant
                _effectiveBitrateLabel.value = resolution.effectiveBitrateLabel
                attemptResumeCurrentTrack(track, activeStreamUrl)
            }
            is SourceResolver.StreamResolution.Unavailable -> {
                _isBuffering.value = false
                _playbackError.value = resolution.error
            }
        }
    }

    /**
     * Fixes BUG-03:
     * Verifies that the ExoPlayer's current MediaItem matches [track]'s ID and [targetStreamUrl]
     * before seeking; if different or empty, sets the new MediaItem at the clamped resume position.
     */
    private fun attemptResumeCurrentTrack(track: Track, targetStreamUrl: String) {
        if (!SourceResolver.isValidStreamUrl(targetStreamUrl)) {
            _isBuffering.value = false
            _isRetrying.value = false
            _playbackError.value = PlaybackError(
                code = "SOURCE_UNAVAILABLE",
                category = ErrorCategory.SOURCE_UNAVAILABLE,
                recoverable = false,
                userMessageFa = "منبع صوتی معتبر برای آهنگ «${track.title}» در دسترس نیست.",
                userMessageEn = "No valid audio source available for \"${track.title}\"."
            )
            return
        }

        val rawResumePos = maxOf(lastKnownPositionMs, _currentPositionMs.value, exoPlayer.currentPosition)
            .coerceAtLeast(0L)
        val knownDur = _durationMs.value
        val clampedResumePos = if (knownDur > 0L) {
            rawResumePos.coerceIn(0L, (knownDur - 1_000L).coerceAtLeast(0L))
        } else {
            rawResumePos
        }

        val uri = Uri.parse(targetStreamUrl)
        val currentItem = exoPlayer.currentMediaItem
        val currentUriString = currentItem?.localConfiguration?.uri?.toString()
        val isSameMediaItem = exoPlayer.mediaItemCount > 0 &&
            currentItem?.mediaId == track.id.toString() &&
            currentUriString == targetStreamUrl

        if (!isSameMediaItem) {
            val mediaItem = buildMediaItem(track, uri)
            exoPlayer.setMediaItem(mediaItem, clampedResumePos)
        } else if (clampedResumePos > 0L) {
            exoPlayer.seekTo(clampedResumePos)
        }

        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        exoPlayer.play()
    }

    fun setIranNoVpnMode(enabled: Boolean) {
        val changed = _iranNoVpnMode.value != enabled
        _iranNoVpnMode.value = enabled
        if (changed) {
            val active = _currentTrack.value
            if (active != null && _playbackError.value != null) {
                retryCurrentTrackManually()
            }
        }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        ensurePlaybackServiceStarted()

        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            _currentIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = listOf(track) + _queue.value
            _currentIndex.value = 0
        } else {
            _currentIndex.value = _queue.value.indexOfFirst { it.id == track.id }
        }

        // Fixes BUG-03: Reset all retry & position states cleanly when switching tracks
        retryJob?.cancel()
        lastKnownPositionMs = 0L
        triedSameTrackFallbackUrl = false
        _isRetrying.value = false
        _retryCount.value = 0
        _playbackError.value = null
        _currentTrack.value = track
        _durationMs.value = track.durationMs.coerceAtLeast(0L)
        _currentPositionMs.value = 0L

        // Fixes BUG-01: Resolve strictly this track's stream URL — never substitute another song!
        when (val resolution = SourceResolver.resolveTrackStream(track, _audioQuality.value, _iranNoVpnMode.value)) {
            is SourceResolver.StreamResolution.Available -> {
                userWantsPlaying = true
                _isBuffering.value = true
                activeStreamUrl = resolution.primaryUrl
                fallbackSameTrackUrl = resolution.fallbackSameTrackUrl
                _hasQualityVariant.value = resolution.hasMultiBitrateVariant
                _effectiveBitrateLabel.value = resolution.effectiveBitrateLabel
                acquirePlaybackLocks()
                startExoPlayerWithUri(track, Uri.parse(activeStreamUrl), 0L)
                onTrackPlayedListener?.invoke(track)
            }
            is SourceResolver.StreamResolution.Unavailable -> {
                userWantsPlaying = false
                activeStreamUrl = ""
                fallbackSameTrackUrl = null
                _isBuffering.value = false
                _isPlaying.value = false
                _playbackError.value = resolution.error
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                releasePlaybackLocks()
            }
        }
    }

    /**
     * Fixes UI-14: Only load bundled artwork bytes if the track explicitly points to a bundled asset cover.
     * Never substitute a random album cover for external tracks on Lock Screen / Notification.
     */
    private fun loadBundledArtworkBytes(artworkUrl: String): ByteArray? {
        if (!artworkUrl.startsWith("file:///android_asset/")) return null
        return try {
            val assetPath = artworkUrl.removePrefix("file:///android_asset/")
            context.assets.open(assetPath).use { it.readBytes() }
        } catch (e: Exception) {
            AppLogger.w("loadBundledArtworkBytes", "Failed reading bundled cover asset", e)
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

        val artworkBytes = loadBundledArtworkBytes(track.artworkUrl)
        if (artworkBytes != null) {
            metadataBuilder.setArtworkData(artworkBytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
        } else if (track.artworkUrl.startsWith("http")) {
            metadataBuilder.setArtworkUri(Uri.parse(track.artworkUrl))
        }

        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun startExoPlayerWithUri(track: Track, uri: Uri, startPositionMs: Long = 0L) {
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setMaxAudioBitrate(_audioQuality.value.maxBitrateBps)
            .build()
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
        _playbackError.value = null
        lastKnownPositionMs = 0L
        activeStreamUrl = ""
        fallbackSameTrackUrl = null
        triedSameTrackFallbackUrl = false
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
        val track = _currentTrack.value ?: return
        userWantsPlaying = true
        _playbackError.value = null
        ensurePlaybackServiceStarted()
        acquirePlaybackLocks()
        if (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null || activeStreamUrl.isBlank()) {
            when (val resolution = SourceResolver.resolveTrackStream(track, _audioQuality.value, _iranNoVpnMode.value)) {
                is SourceResolver.StreamResolution.Available -> {
                    activeStreamUrl = resolution.primaryUrl
                    fallbackSameTrackUrl = resolution.fallbackSameTrackUrl
                    attemptResumeCurrentTrack(track, activeStreamUrl)
                }
                is SourceResolver.StreamResolution.Unavailable -> {
                    userWantsPlaying = false
                    _isBuffering.value = false
                    _playbackError.value = resolution.error
                    releasePlaybackLocks()
                }
            }
        } else {
            exoPlayer.playWhenReady = true
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val knownDur = _durationMs.value
        // Fixes BUG-05 & UI-08: Ignore seeks if duration is unknown (<= 0L)
        if (knownDur <= 0L) return
        val safePos = positionMs.coerceIn(0L, knownDur)
        lastKnownPositionMs = safePos
        _currentPositionMs.value = safePos
        exoPlayer.seekTo(safePos)
        val track = _currentTrack.value
        if (userWantsPlaying && track != null && (exoPlayer.playbackState == Player.STATE_IDLE || exoPlayer.playerError != null)) {
            attemptResumeCurrentTrack(track, activeStreamUrl)
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

        if (exoPlayer.currentPosition > 4_000L && _durationMs.value > 0L) {
            seekTo(0L)
            return
        }

        val prevIndex = if (_currentIndex.value - 1 >= 0) _currentIndex.value - 1 else q.size - 1
        _currentIndex.value = prevIndex
        playTrack(q[prevIndex])
    }

    private fun handleTrackEnded() {
        val q = _queue.value
        lastKnownPositionMs = 0L
        _retryCount.value = 0
        _isRetrying.value = false
        _playbackError.value = null
        if (q.isEmpty()) {
            releasePlaybackLocks()
            return
        }

        when (_playbackMode.value) {
            PlaybackMode.REPEAT_ONE -> {
                exoPlayer.seekTo(0L)
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
                    exoPlayer.seekTo(0L)
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

    /**
     * Fixes BUG-09: Applies real bitrate constraint on ExoPlayer and seamlessly switches
     * the active stream URL between 320kbps and 128kbps variants (when available) while preserving position.
     */
    fun setAudioQuality(quality: AudioQuality) {
        _audioQuality.value = quality
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setMaxAudioBitrate(quality.maxBitrateBps)
                .build()
        } catch (e: Exception) {
            AppLogger.w("setAudioQuality", "Could not update trackSelectionParameters", e)
        }

        val track = _currentTrack.value ?: return
        when (val resolution = SourceResolver.resolveTrackStream(track, quality, _iranNoVpnMode.value)) {
            is SourceResolver.StreamResolution.Available -> {
                _hasQualityVariant.value = resolution.hasMultiBitrateVariant
                _effectiveBitrateLabel.value = resolution.effectiveBitrateLabel
                if (resolution.primaryUrl != activeStreamUrl && SourceResolver.isValidStreamUrl(resolution.primaryUrl)) {
                    val savedPos = exoPlayer.currentPosition.coerceAtLeast(lastKnownPositionMs).coerceAtLeast(0L)
                    val wasPlaying = userWantsPlaying || exoPlayer.isPlaying
                    activeStreamUrl = resolution.primaryUrl
                    fallbackSameTrackUrl = resolution.fallbackSameTrackUrl
                    triedSameTrackFallbackUrl = false
                    val newMediaItem = buildMediaItem(track, Uri.parse(activeStreamUrl))
                    exoPlayer.setMediaItem(newMediaItem, savedPos)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = wasPlaying
                    if (wasPlaying) exoPlayer.play()
                }
            }
            is SourceResolver.StreamResolution.Unavailable -> {
                _hasQualityVariant.value = false
            }
        }
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
                    if (dur > 0L && dur != C.TIME_UNSET) {
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

    private fun parseRetryAfterMs(headers: Map<String, List<String>>?): Long? {
        val raw = headers?.entries?.firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }
            ?.value?.firstOrNull()?.trim() ?: return null
        val seconds = raw.toLongOrNull() ?: return null
        return (seconds * 1_000L).coerceIn(1_000L, 30_000L)
    }

    fun release() {
        userWantsPlaying = false
        retryJob?.cancel()
        stopProgressTracker()
        releasePlaybackLocks()
        unregisterNetworkCallback()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    companion object {
        const val MAX_RETRY_ATTEMPTS = 6

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
