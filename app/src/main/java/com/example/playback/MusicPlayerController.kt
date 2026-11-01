package com.example.playback

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import com.example.data.model.AudioQuality
import com.example.data.model.PlaybackMode
import com.example.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MusicPlayerController private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var playJob: Job? = null
    private var watchdogJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                10000,
                45000,
                1500,
                3000
            )
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
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

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Track>>(emptyList())
    val queue: StateFlow<List<Track>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    // Default to REPEAT_ALL so lists automatically advance and loop seamlessly
    private val _playbackMode = MutableStateFlow(PlaybackMode.REPEAT_ALL)
    val playbackMode: StateFlow<PlaybackMode> = _playbackMode.asStateFlow()

    private val _audioQuality = MutableStateFlow(AudioQuality.HIGH)
    val audioQuality: StateFlow<AudioQuality> = _audioQuality.asStateFlow()

    // Iran No-VPN Direct Playback Mode (enabled by default so Iranian users never need a VPN)
    private val _iranNoVpnMode = MutableStateFlow(true)
    val iranNoVpnMode: StateFlow<Boolean> = _iranNoVpnMode.asStateFlow()

    private var usingLocalFallbackForCurrentTrack = false

    var onTrackPlayedListener: ((Track) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) {
                _isBuffering.value = false
                watchdogJob?.cancel()
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    _isBuffering.value = true
                }
                Player.STATE_READY -> {
                    _isBuffering.value = false
                    watchdogJob?.cancel()
                    val duration = exoPlayer.duration
                    if (duration > 0) {
                        _durationMs.value = duration
                    }
                }
                Player.STATE_ENDED -> {
                    _isBuffering.value = false
                    watchdogJob?.cancel()
                    handleTrackEnded()
                }
                Player.STATE_IDLE -> {
                    _isBuffering.value = false
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            watchdogJob?.cancel()
            val track = _currentTrack.value
            if (track != null && !usingLocalFallbackForCurrentTrack) {
                // Automatic No-VPN failover if remote server is filtered/unreachable in Iran
                switchToLocalNoVpnAudio(track)
            } else {
                _isBuffering.value = false
            }
        }
    }

    fun setIranNoVpnMode(enabled: Boolean) {
        _iranNoVpnMode.value = enabled
        _currentTrack.value?.let { playTrack(it) }
    }

    fun playTrack(track: Track, newQueue: List<Track>? = null) {
        if (newQueue != null && newQueue.isNotEmpty()) {
            _queue.value = newQueue
            _currentIndex.value = newQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else if (!_queue.value.any { it.id == track.id }) {
            _queue.value = listOf(track) + _queue.value
            _currentIndex.value = 0
        } else {
            _currentIndex.value = _queue.value.indexOfFirst { it.id == track.id }
        }

        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L
        _isBuffering.value = true

        playJob?.cancel()
        watchdogJob?.cancel()

        if (_iranNoVpnMode.value) {
            // Instant No-VPN playback guaranteed to work on all Iranian ISPs & National Intranet
            switchToLocalNoVpnAudio(track)
        } else {
            usingLocalFallbackForCurrentTrack = false
            val streamUrl = IranAudioEngine.getUnblockedStreamUrl(track)
            startExoPlayerWithUri(track, Uri.parse(streamUrl))

            // 2.5-second watchdog: if international stream is throttled/blocked without VPN, switch automatically
            watchdogJob = scope.launch {
                delay(2500)
                if (!exoPlayer.isPlaying && _currentTrack.value?.id == track.id && !usingLocalFallbackForCurrentTrack) {
                    switchToLocalNoVpnAudio(track)
                }
            }
        }

        onTrackPlayedListener?.invoke(track)
    }

    private fun switchToLocalNoVpnAudio(track: Track) {
        usingLocalFallbackForCurrentTrack = true
        playJob?.cancel()
        playJob = scope.launch {
            val localUri = IranAudioEngine.getOrGenerateLocalTrackUri(context, track)
            if (_currentTrack.value?.id == track.id) {
                startExoPlayerWithUri(track, localUri)
            }
        }
    }

    private fun startExoPlayerWithUri(track: Track, uri: Uri) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(Uri.parse(track.artworkUrl))
            .build()

        val mediaItem = MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun playPauseToggle() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_IDLE && _currentTrack.value != null) {
                _currentTrack.value?.let { playTrack(it) }
            } else {
                exoPlayer.play()
            }
        }
    }

    /**
     * Completely stops playback, clears active track and hides player bar
     */
    fun stopAndClearPlayback() {
        playJob?.cancel()
        watchdogJob?.cancel()
        stopProgressTracker()
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _isPlaying.value = false
        _isBuffering.value = false
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
        exoPlayer.pause()
    }

    fun resume() {
        exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun skipNext() {
        val q = _queue.value
        if (q.isEmpty()) return

        val nextIndex = when (_playbackMode.value) {
            PlaybackMode.SHUFFLE -> (q.indices).random()
            PlaybackMode.REPEAT_ONE -> _currentIndex.value
            PlaybackMode.REPEAT_ALL -> (_currentIndex.value + 1) % q.size
            PlaybackMode.NORMAL -> {
                if (_currentIndex.value + 1 < q.size) _currentIndex.value + 1 else 0
            }
        }
        _currentIndex.value = nextIndex
        playTrack(q[nextIndex])
    }

    fun skipPrevious() {
        val q = _queue.value
        if (q.isEmpty()) return

        if (exoPlayer.currentPosition > 3000) {
            seekTo(0)
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
                seekTo(0)
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
                    _isPlaying.value = false
                    seekTo(0)
                    exoPlayer.pause()
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
                    _currentPositionMs.value = exoPlayer.currentPosition
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
        playJob?.cancel()
        watchdogJob?.cancel()
        stopProgressTracker()
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
