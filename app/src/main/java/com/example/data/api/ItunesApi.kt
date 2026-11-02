package com.example.data.api

import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ItunesResponse(
    @Json(name = "resultCount") val resultCount: Int,
    @Json(name = "results") val results: List<ItunesTrackDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ItunesTrackDto(
    @Json(name = "trackId") val trackId: Long?,
    @Json(name = "collectionId") val collectionId: Long?,
    @Json(name = "trackName") val trackName: String?,
    @Json(name = "artistName") val artistName: String?,
    @Json(name = "collectionName") val collectionName: String?,
    @Json(name = "artworkUrl100") val artworkUrl100: String?,
    @Json(name = "artworkUrl60") val artworkUrl60: String?,
    @Json(name = "previewUrl") val previewUrl: String?,
    @Json(name = "primaryGenreName") val primaryGenreName: String?,
    @Json(name = "trackTimeMillis") val trackTimeMillis: Long?,
    @Json(name = "releaseDate") val releaseDate: String?,
    @Json(name = "country") val country: String?
)

interface ItunesApiService {
    @GET("search")
    suspend fun searchTracks(
        @Query("term") term: String,
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 50,
        @Query("country") country: String? = null
    ): ItunesResponse
}

object ApiClient {
    private const val BASE_URL = "https://itunes.apple.com/"

    private val okHttpClient: OkHttpClient by lazy {
        // Fixes BUG-12: Never use Level.BODY in Release builds; redact sensitive headers
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
            redactHeader("Cookie")
            redactHeader("Set-Cookie")
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val itunesApi: ItunesApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ItunesApiService::class.java)
    }
}
