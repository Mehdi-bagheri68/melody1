package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.R

@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val playerController = MusicPlayerController.getInstance(this)
        val basePlayer = playerController.exoPlayer

        // Ensure audio attributes & network wake mode are active for background / lock-screen playback
        basePlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            true
        )
        basePlayer.setWakeMode(C.WAKE_MODE_NETWORK)

        // Wrap ExoPlayer in a ForwardingPlayer that exposes all queue & seek commands
        // to the Android Notification Bar, Lock Screen, Bluetooth Car Stereo, and Headsets
        val forwardingPlayer = object : ForwardingPlayer(basePlayer) {

            override fun getAvailableCommands(): Player.Commands {
                return super.getAvailableCommands()
                    .buildUpon()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_STOP)
                    .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_FORWARD)
                    .add(Player.COMMAND_SEEK_BACK)
                    .add(Player.COMMAND_SEEK_TO_NEXT)
                    .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                    .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                    .add(Player.COMMAND_GET_CURRENT_MEDIA_ITEM)
                    .add(Player.COMMAND_GET_TIMELINE)
                    .add(Player.COMMAND_GET_METADATA)
                    .build()
            }

            override fun isCommandAvailable(command: Int): Boolean {
                return when (command) {
                    Player.COMMAND_PLAY_PAUSE,
                    Player.COMMAND_STOP,
                    Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_FORWARD,
                    Player.COMMAND_SEEK_BACK,
                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> true
                    else -> super.isCommandAvailable(command)
                }
            }

            override fun hasNextMediaItem(): Boolean = true

            override fun hasPreviousMediaItem(): Boolean = true

            override fun seekToNext() {
                playerController.skipNext()
            }

            override fun seekToNextMediaItem() {
                playerController.skipNext()
            }

            override fun seekToPrevious() {
                playerController.skipPrevious()
            }

            override fun seekToPreviousMediaItem() {
                playerController.skipPrevious()
            }

            override fun seekForward() {
                val target = (basePlayer.currentPosition + 10_000L)
                    .coerceAtMost(basePlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE)
                playerController.seekTo(target)
            }

            override fun seekBack() {
                val target = (basePlayer.currentPosition - 10_000L).coerceAtLeast(0L)
                playerController.seekTo(target)
            }

            override fun seekTo(positionMs: Long) {
                playerController.seekTo(positionMs)
            }

            override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
                playerController.seekTo(positionMs)
            }

            override fun play() {
                playerController.resume()
            }

            override fun pause() {
                playerController.pause()
            }

            override fun stop() {
                playerController.stopAndClearPlayback()
            }
        }

        val activityIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            pendingIntentFlags
        )

        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .build()
            .apply {
                setSmallIcon(R.mipmap.ic_launcher)
            }
        setMediaNotificationProvider(notificationProvider)

        mediaSession = MediaSession.Builder(this, forwardingPlayer)
            .setSessionActivity(sessionActivityPendingIntent)
            .setCallback(object : MediaSession.Callback {
                override fun onConnect(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo
                ): MediaSession.ConnectionResult {
                    val sessionCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon().build()
                    val playerCommands = MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS
                        .buildUpon()
                        .add(Player.COMMAND_PLAY_PAUSE)
                        .add(Player.COMMAND_STOP)
                        .add(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_FORWARD)
                        .add(Player.COMMAND_SEEK_BACK)
                        .add(Player.COMMAND_SEEK_TO_NEXT)
                        .add(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS)
                        .add(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
                        .build()
                    return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                        .setAvailableSessionCommands(sessionCommands)
                        .setAvailablePlayerCommands(playerCommands)
                        .build()
                }

                override fun onCustomCommand(
                    session: MediaSession,
                    controller: MediaSession.ControllerInfo,
                    customCommand: androidx.media3.session.SessionCommand,
                    args: Bundle
                ) = super.onCustomCommand(session, controller, customCommand, args)
            })
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Return START_STICKY so the OS keeps the playback service alive in background / lock screen
        return START_STICKY
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || (!player.playWhenReady && player.mediaItemCount == 0)) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
