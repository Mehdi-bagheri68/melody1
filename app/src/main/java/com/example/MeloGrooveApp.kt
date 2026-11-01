package com.example

import android.app.Application
import com.example.data.local.MeloGrooveDatabase
import com.example.data.local.UserPreferences
import com.example.data.repository.MusicRepository
import com.example.playback.MusicPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MeloGrooveApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: MeloGrooveDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var playerController: MusicPlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MeloGrooveDatabase.getDatabase(this)
        repository = MusicRepository(database.musicDao())
        userPreferences = UserPreferences(this)
        playerController = MusicPlayerController.getInstance(this)

        // Automatically record played tracks to Listening History in background
        playerController.onTrackPlayedListener = { track ->
            applicationScope.launch {
                repository.recordPlayedTrack(track)
            }
        }
    }

    companion object {
        lateinit var instance: MeloGrooveApp
            private set
    }
}
