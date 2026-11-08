package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.example.data.auth.AuthRepository
import com.example.data.auth.AuthState
import com.example.data.auth.LocalEmailAuthRepository
import com.example.data.local.MeloGrooveDatabase
import com.example.data.local.UserPreferences
import com.example.data.remote.DriveStorageService
import com.example.data.repository.CatalogDataSource
import com.example.data.repository.MusicRepository
import com.example.playback.MusicPlayerController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MeloGrooveApp : Application(), ImageLoaderFactory {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: MeloGrooveDatabase
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var userPreferences: UserPreferences
        private set

    lateinit var authRepository: AuthRepository
        private set

    lateinit var driveStorageService: DriveStorageService
        private set

    lateinit var playerController: MusicPlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        CatalogDataSource.initialize(this)
        database = MeloGrooveDatabase.getDatabase(this)
        userPreferences = UserPreferences(this)
        authRepository = LocalEmailAuthRepository(database.musicDao(), userPreferences)
        driveStorageService = DriveStorageService(userPreferences)
        repository = MusicRepository(database.musicDao())
        playerController = MusicPlayerController.getInstance(this)
        playerController.setAudioQuality(userPreferences.audioQuality.value)
        playerController.setIranNoVpnMode(userPreferences.iranNoVpnMode.value)

        // Automatically record played tracks to Listening History in background
        playerController.onTrackPlayedListener = { track ->
            applicationScope.launch {
                val userId = userPreferences.userProfile.value?.uid?.takeIf { it.isNotBlank() }
                    ?: (authRepository.state.value as? AuthState.SignedIn)?.uid
                    ?: "local"
                repository.recordPlayedTrack(track, userId)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header(
                        "User-Agent",
                        "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                    )
                    .header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("melogroove_covers_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB cover cache
                    .build()
            }
            .respectCacheHeaders(false)
            .build()
    }

    companion object {
        lateinit var instance: MeloGrooveApp
            private set
    }
}
