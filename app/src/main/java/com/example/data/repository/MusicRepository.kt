package com.example.data.repository

import com.example.data.api.ApiClient
import com.example.data.api.ArchiveApiClient
import com.example.data.api.ArchiveApiService
import com.example.data.api.ItunesApiService
import com.example.data.local.MusicDao
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistTrackCrossRefEntity
import com.example.data.local.toFavoriteEntity
import com.example.data.local.toHistoryEntity
import com.example.data.model.ArtistSortOption
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.Playlist
import com.example.data.model.Track
import com.example.util.AppLogger
import com.example.util.IdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Refactored MusicRepository (v1.6.0):
 * - Externalizes static catalog to [CatalogDataSource] (`assets/catalog/artists.json`, `tracks.json`, `groups.json`).
 * - Enforces strict 1-to-1 artwork ownership via [ArtworkResolver] (fixes N-01).
 * - Evaluates New Releases (`perArtistCap = 1`) and Trending shelves via [GroupEngine] (fixes N-03, U-03).
 * - Uses 64-bit deterministic IDs via [IdGenerator.stable64BitId] (fixes N-07).
 */
class MusicRepository(
    private val musicDao: MusicDao,
    private val itunesApi: ItunesApiService = ApiClient.itunesApi,
    private val archiveApi: ArchiveApiService = ArchiveApiClient.service
) {
    private data class CacheEntry(
        val tracks: List<Track>,
        val createdAtMs: Long
    )

    private val cacheMutex = Mutex()
    private val searchCache = LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true)

    private val scraperClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    /**
     * Synchronous initial data getters evaluated via [GroupEngine] so HomeScreen renders
     * immediately on frame 0 with `perArtistCap = 1` on New Releases (fixes N-03 & U-03).
     */
    fun getInitialNewReleasesSync(country: Country): List<Track> {
        val countryTracks = getCuratedTracksForCountry(country, emptySet())
        val group = CatalogDataSource.getDefaultNewReleasesGroup(country.code)
        return GroupEngine.evaluate(
            group = group,
            tracks = countryTracks,
            artists = CatalogDataSource.getArtistsById()
        )
    }

    fun getInitialTrendingSync(country: Country): List<Track> {
        val countryTracks = getCuratedTracksForCountry(country, emptySet())
        val group = CatalogDataSource.getDefaultTrendingGroup(country.code)
        return GroupEngine.evaluate(
            group = group,
            tracks = countryTracks,
            artists = CatalogDataSource.getArtistsById()
        )
    }

    suspend fun clearSearchCache() {
        cacheMutex.withLock {
            searchCache.clear()
        }
    }

    private fun normalizeCacheKey(
        term: String,
        countryCode: String?,
        genreFilter: String?,
        yearFilter: String?,
        iranNoVpnMode: Boolean
    ): String {
        return listOf(
            term.trim().lowercase(),
            (countryCode ?: "").trim().uppercase(),
            (genreFilter ?: "").trim().lowercase(),
            (yearFilter ?: "").trim(),
            if (iranNoVpnMode) "novpn" else "global"
        ).joinToString("|")
    }

    suspend fun searchTracks(
        term: String,
        countryCode: String? = null,
        genreFilter: String? = null,
        yearFilter: String? = null,
        iranNoVpnMode: Boolean = true
    ): List<Track> = withContext(Dispatchers.IO) {
        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("searchTracks:favorites", "Failed to read favorite IDs", e)
            emptySet()
        }

        val cacheKey = normalizeCacheKey(term, countryCode, genreFilter, yearFilter, iranNoVpnMode)
        val now = System.currentTimeMillis()

        cacheMutex.withLock {
            val cached = searchCache[cacheKey]
            if (cached != null) {
                if (now - cached.createdAtMs <= CACHE_TTL_MS) {
                    return@withContext cached.tracks.map { track ->
                        track.copy(isFavorite = favoriteIds.contains(track.id))
                    }
                } else {
                    searchCache.remove(cacheKey)
                }
            }
        }

        val fullTracks = mutableListOf<Track>()
        var encounteredNetworkError = false

        val basePool = if (!countryCode.isNullOrBlank() && term.isBlank()) {
            getCuratedTracksForCountry(CountriesData.getCountryByCode(countryCode), favoriteIds)
        } else {
            getAllCuratedFullTracks(favoriteIds)
        }

        val curatedMatches = if (term.isBlank()) {
            basePool
        } else {
            basePool.filter {
                it.title.contains(term, ignoreCase = true) ||
                    it.artist.contains(term, ignoreCase = true) ||
                    it.album.contains(term, ignoreCase = true) ||
                    it.genre.contains(term, ignoreCase = true)
            }
        }
        fullTracks.addAll(curatedMatches)

        if (term.isNotBlank() && SourceResolver.isHostHealthy("musics-fa.com")) {
            val liveResult = fetchLiveFromMusicsFa(
                query = term,
                favoriteIds = favoriteIds,
                countryCode = countryCode ?: "IR"
            )
            if (liveResult.hadNetworkFailure) {
                encounteredNetworkError = true
            }
            fullTracks.addAll(liveResult.tracks)
        }

        if (!iranNoVpnMode && term.isNotBlank() && SourceResolver.isHostHealthy("itunes.apple.com")) {
            try {
                val itunesResp = itunesApi.searchTracks(
                    term = term.trim(),
                    country = countryCode?.takeIf { it != "IR" } ?: "US"
                )
                SourceResolver.recordSuccess("https://itunes.apple.com/search")
                val mappedItunes = itunesResp.results.mapNotNull { dto ->
                    val title = dto.trackName?.trim().orEmpty()
                    val artist = dto.artistName?.trim().orEmpty()
                    val streamUrl = dto.previewUrl?.trim().orEmpty()
                    if (title.isBlank() || artist.isBlank() || !SourceResolver.isValidStreamUrl(streamUrl)) {
                        null
                    } else {
                        val id = dto.trackId?.let { 2_000_000L + kotlin.math.abs(it) }
                            ?: IdGenerator.stable64BitId("itunes", streamUrl, offset = 2_000_000L)
                        val artistId = Track.slugifyArtistId(artist)
                        val rawCover = dto.artworkUrl100?.replace("100x100bb", "600x600bb") ?: ""
                        val ownedArt = ArtworkResolver.buildOwnedArtworkOrNull(rawCover, artistId)
                        Track(
                            id = id,
                            title = title,
                            artist = artist,
                            album = dto.collectionName?.takeIf { it.isNotBlank() } ?: "Single",
                            artworkUrl = ownedArt?.url ?: "",
                            previewUrl = streamUrl,
                            genre = dto.primaryGenreName ?: "Pop",
                            durationMs = (dto.trackTimeMillis ?: 0L).coerceAtLeast(0L),
                            releaseYear = dto.releaseDate?.take(4) ?: "2025",
                            countryCode = countryCode ?: "US",
                            isFavorite = favoriteIds.contains(id),
                            playCount = 95000,
                            sourceId = "itunes",
                            artistId = artistId,
                            artwork = ownedArt
                        )
                    }
                }
                fullTracks.addAll(mappedItunes)
            } catch (e: Exception) {
                encounteredNetworkError = true
                SourceResolver.recordFailure("https://itunes.apple.com/search")
                AppLogger.w("searchTracks:itunes", "International iTunes query failed", e)
            }
        }

        var results = fullTracks
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && SourceResolver.isValidStreamUrl(it.previewUrl) }
            .distinctBy { "${it.id}_${it.previewUrl}" }

        if (!countryCode.isNullOrBlank() && term.isBlank()) {
            results = results.filter { it.countryCode.equals(countryCode, ignoreCase = true) }
        }
        if (!genreFilter.isNullOrBlank()) {
            results = results.filter { it.genre.contains(genreFilter, ignoreCase = true) }
        }
        if (!yearFilter.isNullOrBlank()) {
            results = results.filter { it.releaseYear.startsWith(yearFilter) }
        }

        if (results.isNotEmpty() || !encounteredNetworkError) {
            cacheMutex.withLock {
                if (searchCache.size >= MAX_CACHE_ENTRIES) {
                    val oldestKey = searchCache.keys.firstOrNull()
                    if (oldestKey != null) searchCache.remove(oldestKey)
                }
                searchCache[cacheKey] = CacheEntry(results, now)
            }
        }

        results
    }

    suspend fun getTracksByArtist(
        artistName: String,
        sortOption: ArtistSortOption = ArtistSortOption.TRENDING
    ): List<Track> = withContext(Dispatchers.IO) {
        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("getTracksByArtist:favorites", "Failed reading favorite IDs", e)
            emptySet()
        }
        val allCurated = getAllCuratedFullTracks(favoriteIds)
        val cleanTarget = artistName.trim()
        val targetSlug = Track.slugifyArtistId(cleanTarget)

        val matched = allCurated.filter {
            it.artistId == targetSlug ||
                it.artist.contains(cleanTarget, ignoreCase = true) ||
                cleanTarget.contains(it.artist, ignoreCase = true)
        }.toMutableList()

        if (cleanTarget.isNotBlank() && SourceResolver.isHostHealthy("musics-fa.com")) {
            val liveArtistResult = fetchLiveFromMusicsFa(
                query = cleanTarget,
                favoriteIds = favoriteIds,
                defaultArtist = cleanTarget
            )
            matched.addAll(liveArtistResult.tracks)
        }

        val unique = matched
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() && SourceResolver.isValidStreamUrl(it.previewUrl) }
            .distinctBy { it.previewUrl }

        when (sortOption) {
            ArtistSortOption.TRENDING -> unique.sortedByDescending { it.playCount }
            ArtistSortOption.NEWEST -> unique.sortedByDescending { it.releaseYear.toIntOrNull() ?: 2026 }
        }
    }

    private data class LiveFetchResult(
        val tracks: List<Track>,
        val hadNetworkFailure: Boolean
    )

    private fun fetchLiveFromMusicsFa(
        query: String,
        favoriteIds: Set<Long>,
        defaultArtist: String? = null,
        countryCode: String = "IR"
    ): LiveFetchResult {
        val endpoint = "https://musics-fa.com/"
        return try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "$endpoint?s=$encoded"
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                )
                .build()

            scraperClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val isHard = response.code in listOf(403, 429, 503)
                    SourceResolver.recordFailure(endpoint, isHardFailure = isHard)
                    AppLogger.w("fetchLiveFromMusicsFa", "HTTP ${response.code} for query=$query")
                    return LiveFetchResult(emptyList(), hadNetworkFailure = true)
                }
                val html = response.body?.string().orEmpty()
                when (val outcome = MusicsFaHtmlParser.parseHtml(html, query, favoriteIds, defaultArtist, countryCode)) {
                    is MusicsFaHtmlParser.ParseOutcome.Success -> {
                        SourceResolver.recordSuccess(endpoint)
                        LiveFetchResult(outcome.tracks, hadNetworkFailure = false)
                    }
                    is MusicsFaHtmlParser.ParseOutcome.EmptyResult -> {
                        SourceResolver.recordSuccess(endpoint)
                        LiveFetchResult(emptyList(), hadNetworkFailure = false)
                    }
                    is MusicsFaHtmlParser.ParseOutcome.CaptchaOrBlocked -> {
                        SourceResolver.recordFailure(endpoint, isHardFailure = true)
                        AppLogger.w("fetchLiveFromMusicsFa", "Anti-bot/CAPTCHA challenge detected on musics-fa")
                        LiveFetchResult(emptyList(), hadNetworkFailure = true)
                    }
                    is MusicsFaHtmlParser.ParseOutcome.StructureChanged -> {
                        AppLogger.w("fetchLiveFromMusicsFa", "DOM structure change: ${outcome.reason}")
                        LiveFetchResult(emptyList(), hadNetworkFailure = false)
                    }
                }
            }
        } catch (e: Exception) {
            SourceResolver.recordFailure(endpoint, isHardFailure = false)
            AppLogger.w("fetchLiveFromMusicsFa", "Network exception fetching live results", e)
            LiveFetchResult(emptyList(), hadNetworkFailure = true)
        }
    }

    /**
     * Evaluates New Releases via [GroupEngine] with `perArtistCap = 1` (fixes N-03 & U-03).
     */
    suspend fun getNewReleasesForCountry(country: Country): List<Track> = withContext(Dispatchers.IO) {
        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("getNewReleasesForCountry", "Failed reading favorites", e)
            emptySet()
        }
        val countryTracks = getCuratedTracksForCountry(country, favoriteIds)
        val group = CatalogDataSource.getDefaultNewReleasesGroup(country.code)
        GroupEngine.evaluate(
            group = group,
            tracks = countryTracks,
            artists = CatalogDataSource.getArtistsById()
        )
    }

    /**
     * Evaluates Trending tracks via [GroupEngine].
     */
    suspend fun getTrendingForCountry(country: Country): List<Track> = withContext(Dispatchers.IO) {
        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("getTrendingForCountry", "Failed reading favorites", e)
            emptySet()
        }
        val countryTracks = getCuratedTracksForCountry(country, favoriteIds)
        val group = CatalogDataSource.getDefaultTrendingGroup(country.code)
        GroupEngine.evaluate(
            group = group,
            tracks = countryTracks,
            artists = CatalogDataSource.getArtistsById()
        )
    }

    suspend fun getTracksForMood(moodQuery: String, countryCode: String): List<Track> = withContext(Dispatchers.IO) {
        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("getTracksForMood", "Failed reading favorites", e)
            emptySet()
        }
        val country = CountriesData.getCountryByCode(countryCode)
        val countryTracks = getCuratedTracksForCountry(country, favoriteIds)

        val filtered = when {
            moodQuery == "new_releases" -> {
                val group = CatalogDataSource.getDefaultNewReleasesGroup(country.code)
                GroupEngine.evaluate(group, countryTracks, CatalogDataSource.getArtistsById())
            }

            moodQuery == "pop" ->
                countryTracks.filter {
                    it.genre.contains("پاپ", ignoreCase = true) ||
                        it.genre.contains("Pop", ignoreCase = true) ||
                        it.genre.contains("هیت", ignoreCase = true)
                }

            moodQuery == "traditional" ->
                countryTracks.filter {
                    it.genre.contains("سنتی", ignoreCase = true) ||
                        it.genre.contains("آرابسک", ignoreCase = true) ||
                        it.genre.contains("Arabesk", ignoreCase = true) ||
                        it.genre.contains("هیپ‌هاپ", ignoreCase = true) ||
                        it.genre.contains("Hip-Hop", ignoreCase = true) ||
                        it.genre.contains("راک", ignoreCase = true) ||
                        it.genre.contains("Rock", ignoreCase = true) ||
                        it.genre.contains("فلامنکو", ignoreCase = true) ||
                        it.genre.contains("Flamenco", ignoreCase = true) ||
                        it.genre.contains("شانسون", ignoreCase = true) ||
                        it.genre.contains("کلاسیک", ignoreCase = true)
                }

            moodQuery.contains("party", ignoreCase = true) || moodQuery.contains("dance", ignoreCase = true) ->
                countryTracks.filter {
                    it.genre.contains("شاد", ignoreCase = true) ||
                        it.genre.contains("پارتی", ignoreCase = true) ||
                        it.genre.contains("کلاب", ignoreCase = true) ||
                        it.genre.contains("دنس", ignoreCase = true) ||
                        it.genre.contains("Party", ignoreCase = true) ||
                        it.genre.contains("Dance", ignoreCase = true) ||
                        it.genre.contains("ریتم", ignoreCase = true) ||
                        it.playCount >= 120000
                }

            moodQuery.contains("drive", ignoreCase = true) || moodQuery.contains("road", ignoreCase = true) ->
                countryTracks.filterIndexed { index, track ->
                    track.genre.contains("راک", ignoreCase = true) ||
                        track.genre.contains("پاپ", ignoreCase = true) ||
                        track.genre.contains("الکترو", ignoreCase = true) ||
                        index % 2 == 0
                }

            moodQuery.contains("relax", ignoreCase = true) || moodQuery.contains("calm", ignoreCase = true) ->
                countryTracks.filter {
                    it.genre.contains("آرامش", ignoreCase = true) ||
                        it.genre.contains("سنتی", ignoreCase = true) ||
                        it.genre.contains("عاشقانه", ignoreCase = true) ||
                        it.genre.contains("پیانو", ignoreCase = true) ||
                        it.genre.contains("گیتار", ignoreCase = true) ||
                        it.genre.contains("آکوستیک", ignoreCase = true) ||
                        it.genre.contains("احساسی", ignoreCase = true)
                }

            moodQuery.contains("workout", ignoreCase = true) ->
                countryTracks.filter {
                    it.genre.contains("شاد", ignoreCase = true) ||
                        it.genre.contains("انرژی", ignoreCase = true) ||
                        it.genre.contains("کلاب", ignoreCase = true) ||
                        it.genre.contains("هیپ‌هاپ", ignoreCase = true) ||
                        it.genre.contains("راک", ignoreCase = true) ||
                        it.genre.contains("مدرن", ignoreCase = true) ||
                        it.playCount >= 125000
                }

            else ->
                countryTracks.filter {
                    it.genre.contains("عاشقانه", ignoreCase = true) ||
                        it.genre.contains("احساسی", ignoreCase = true) ||
                        it.genre.contains("دامار", ignoreCase = true) ||
                        it.genre.contains("بالاد", ignoreCase = true) ||
                        it.genre.contains("سنتی", ignoreCase = true)
                }
        }

        if (filtered.isNotEmpty()) filtered else countryTracks
    }

    internal fun sanitizeStoredTrack(track: Track): Track {
        val curatedMatch = getAllCuratedFullTracks(emptySet()).find { curated ->
            curated.id == track.id && (
                curated.title.equals(track.title, ignoreCase = true) ||
                    curated.artist.equals(track.artist, ignoreCase = true)
                )
        }
        if (curatedMatch != null) {
            return curatedMatch.copy(isFavorite = track.isFavorite)
        }

        val effectiveArtistId = track.artistId.ifBlank { Track.slugifyArtistId(track.artist) }
        val ownedArtwork = ArtworkResolver.buildOwnedArtworkOrNull(track.artworkUrl, effectiveArtistId)

        val cleanMp3 = if (SourceResolver.isValidStreamUrl(track.previewUrl)) {
            track.previewUrl
        } else {
            ""
        }

        return track.copy(
            artworkUrl = ownedArtwork?.url ?: "",
            previewUrl = cleanMp3,
            sourceId = if (track.id < 10_000L) "curated" else "stored",
            artistId = effectiveArtistId,
            artwork = ownedArtwork
        )
    }

    // Room Favorites
    fun getAllFavorites(): Flow<List<Track>> {
        return musicDao.getAllFavorites().map { entities ->
            entities.map { sanitizeStoredTrack(it.toTrack()) }
        }
    }

    fun isFavorite(trackId: Long): Flow<Boolean> = musicDao.isFavorite(trackId)

    suspend fun toggleFavorite(track: Track, userId: String = "local"): Boolean = withContext(Dispatchers.IO) {
        val currentFavs = musicDao.getAllFavoriteIds().first()
        val isFav = currentFavs.contains(track.id)
        val newState = if (isFav) {
            musicDao.deleteFavorite(track.id)
            false
        } else {
            musicDao.insertFavorite(track.toFavoriteEntity(userId))
            true
        }
        cacheMutex.withLock {
            for ((key, entry) in searchCache.entries) {
                val updatedList = entry.tracks.map { cachedTrack ->
                    if (cachedTrack.id == track.id) cachedTrack.copy(isFavorite = newState) else cachedTrack
                }
                searchCache[key] = entry.copy(tracks = updatedList)
            }
        }
        newState
    }

    // Room History
    fun getListeningHistory(): Flow<List<Track>> {
        return musicDao.getListeningHistory().map { entities ->
            val favIds = musicDao.getAllFavoriteIds().first().toSet()
            entities.map { sanitizeStoredTrack(it.toTrack(isFavorite = favIds.contains(it.trackId))) }
        }
    }

    suspend fun recordPlayedTrack(track: Track, userId: String = "local") = withContext(Dispatchers.IO) {
        musicDao.insertHistory(track.toHistoryEntity(userId))
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        musicDao.clearHistory()
    }

    // Room Playlists
    fun getAllPlaylists(): Flow<List<Playlist>> {
        return musicDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                val count = musicDao.getTrackCountForPlaylist(entity.id)
                Playlist(
                    id = entity.id,
                    name = entity.name,
                    description = entity.description,
                    trackCount = count,
                    createdAt = entity.createdAt
                )
            }
        }
    }

    suspend fun createPlaylist(name: String, description: String = "", userId: String = "local"): Long = withContext(Dispatchers.IO) {
        musicDao.insertPlaylist(PlaylistEntity(name = name, description = description, userId = userId))
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        musicDao.deletePlaylistAndTracks(playlistId)
    }

    suspend fun addTrackToPlaylist(playlistId: Long, track: Track) = withContext(Dispatchers.IO) {
        val effectiveArtistId = track.artistId.ifBlank { Track.slugifyArtistId(track.artist) }
        val ownedArt = track.artwork ?: ArtworkResolver.buildOwnedArtworkOrNull(track.artworkUrl, effectiveArtistId)
        musicDao.insertTrackToPlaylist(
            PlaylistTrackCrossRefEntity(
                playlistId = playlistId,
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUrl = ownedArt?.url ?: "",
                previewUrl = track.previewUrl,
                genre = track.genre,
                durationMs = track.durationMs,
                releaseYear = track.releaseYear,
                countryCode = track.countryCode,
                artistId = effectiveArtistId,
                ownerArtistId = ownedArt?.ownerArtistId ?: effectiveArtistId
            )
        )
    }

    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) = withContext(Dispatchers.IO) {
        musicDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    fun getTracksForPlaylist(playlistId: Long): Flow<List<Track>> {
        return musicDao.getTracksForPlaylist(playlistId).map { crossRefs ->
            val favIds = musicDao.getAllFavoriteIds().first().toSet()
            crossRefs.map { sanitizeStoredTrack(it.toTrack(isFavorite = favIds.contains(it.trackId))) }
        }
    }

    fun getCuratedTracksForCountry(country: Country, favoriteIds: Set<Long>): List<Track> {
        val code = country.code.uppercase()
        return CatalogDataSource.getTracks()
            .filter { it.countryCode.equals(code, ignoreCase = true) }
            .map { it.copy(isFavorite = favoriteIds.contains(it.id)) }
    }

    fun getAllCuratedFullTracks(favoriteIds: Set<Long> = emptySet()): List<Track> {
        return CatalogDataSource.getTracks()
            .map { it.copy(isFavorite = favoriteIds.contains(it.id)) }
    }

    companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val MAX_CACHE_ENTRIES = 40
    }
}
