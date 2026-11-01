package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "response") val response: ArchiveSearchDocs? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveSearchDocs(
    @Json(name = "numFound") val numFound: Int? = 0,
    @Json(name = "docs") val docs: List<ArchiveDocDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDocDto(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "creator") val creator: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "year") val year: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFilesResponse(
    @Json(name = "result") val result: List<ArchiveFileDto>? = null,
    @Json(name = "files") val files: List<ArchiveFileDto>? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFileDto(
    @Json(name = "name") val name: String,
    @Json(name = "format") val format: String? = null,
    @Json(name = "length") val length: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "artist") val artist: String? = null,
    @Json(name = "album") val album: String? = null
)

interface ArchiveApiService {
    @GET("advancedsearch.php")
    suspend fun searchAudio(
        @Query("q", encoded = true) query: String,
        @Query("rows") rows: Int = 15,
        @Query("output") output: String = "json"
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}/files")
    suspend fun getFiles(
        @Path("identifier") identifier: String
    ): ArchiveFilesResponse
}

object ArchiveApiClient {
    private const val BASE_URL = "https://archive.org/"

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

    val service: ArchiveApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(ArchiveApiService::class.java)
    }
}
