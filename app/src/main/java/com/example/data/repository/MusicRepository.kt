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
import com.example.data.model.ContentOrigin
import com.example.data.model.CountriesData
import com.example.data.model.Country
import com.example.data.model.DatePrecision
import com.example.data.model.Playlist
import com.example.data.model.PopularitySignal
import com.example.data.model.ReleaseKind
import com.example.data.model.Track
import com.example.data.provider.CuratedCatalogProviderAdapter
import com.example.data.provider.EditorialBanner
import com.example.data.provider.EditorialBannerValidator
import com.example.data.provider.InternetArchiveProviderAdapter
import com.example.data.provider.ItunesProviderAdapter
import com.example.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Multi-Source MusicRepository with Stale-While-Revalidate Refresh Orchestrator.
 * Remediated per Section 2.1, 2.2, 2.3, 2.4, 2.5, and 2.6 of MELOGROOVE_PRODUCT_AUDIT_AND_REMEDIATION_PLAN:
 * - Uses official/approved provider adapters ([ItunesProviderAdapter], [InternetArchiveProviderAdapter], [CuratedCatalogProviderAdapter]).
 * - Disables unauthorized HTML scraping (`musics-fa.com` scraper disabled by default).
 * - Implements Stale-While-Revalidate TTL tracking (Trending = 15m, New Releases = 30m, Editorial Banners = 6h)
 *   and preserves last-known-good section snapshots when a network provider fails.
 * - Deduplicates tracks across providers and across Home shelves.
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

    data class HomeSectionSnapshot(
        val tracks: List<Track>,
        val fetchedAtMs: Long,
        val hasLiveProviderItems: Boolean
    )

    private val cacheMutex = Mutex()
    private val searchCache = LinkedHashMap<String, CacheEntry>(MAX_CACHE_ENTRIES, 0.75f, true)

    private val trendingSnapshots = ConcurrentHashMap<String, HomeSectionSnapshot>()
    private val newReleasesSnapshots = ConcurrentHashMap<String, HomeSectionSnapshot>()
    private val editorialBannerTimestampByCountry = ConcurrentHashMap<String, Long>()

    private val curatedAdapter = CuratedCatalogProviderAdapter()
    private val itunesAdapter = ItunesProviderAdapter(itunesApi)
    private val archiveAdapter = InternetArchiveProviderAdapter(archiveApi)

    /**
     * Synchronous initial data getters evaluated via [GroupEngine] so HomeScreen renders
     * immediately on cold start (Stale-While-Revalidate Phase 1) with `perArtistCap = 1` on New Releases.
     */
    fun getInitialNewReleasesSync(country: Country): List<Track> {
        val code = country.code.uppercase(Locale.ROOT)
        newReleasesSnapshots[code]?.tracks?.takeIf { it.isNotEmpty() }?.let { return it }

        val countryTracks = getCuratedTracksForCountry(country, emptySet())
        val group = CatalogDataSource.getDefaultNewReleasesGroup(code)
        return GroupEngine.evaluate(
            group = group,
            tracks = countryTracks,
            artists = CatalogDataSource.getArtistsById()
        )
    }

    fun getInitialTrendingSync(country: Country): List<Track> {
        val code = country.code.uppercase(Locale.ROOT)
        trendingSnapshots[code]?.tracks?.takeIf { it.isNotEmpty() }?.let { return it }

        val newReleaseIds = getInitialNewReleasesSync(country).map { it.id }.toSet()
        val countryTracks = getCuratedTracksForCountry(country, emptySet())
        val group = CatalogDataSource.getDefaultTrendingGroup(code)
        val deduplicated = GroupEngine.evaluate(
            group = group,
            tracks = countryTracks,
            artists = CatalogDataSource.getArtistsById(),
            excludeTrackIds = newReleaseIds
        )
        return if (deduplicated.isNotEmpty()) {
            deduplicated
        } else {
            GroupEngine.evaluate(
                group = group,
                tracks = countryTracks,
                artists = CatalogDataSource.getArtistsById()
            )
        }
    }

    fun getEditorialBannersForCountry(
        country: Country,
        nowMs: Long = System.currentTimeMillis()
    ): List<EditorialBanner> {
        val code = country.code.uppercase(Locale.ROOT)
        editorialBannerTimestampByCountry[code] = nowMs
        return EditorialBannerValidator.filterActiveForCountry(
            banners = EditorialBannerValidator.getDefaultEditorialBanners(),
            countryCode = code,
            nowMs = nowMs
        )
    }

    /**
     * Section 2.1 & 2.6: Checks if any Home section for [countryCode] has exceeded its TTL.
     */
    fun isHomeSectionExpired(
        countryCode: String,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        val code = countryCode.uppercase(Locale.ROOT)
        val trendingSnap = trendingSnapshots[code]
        val releasesSnap = newReleasesSnapshots[code]
        if (trendingSnap == null || releasesSnap == null) return true
        val trendingExpired = (nowMs - trendingSnap.fetchedAtMs) >= TRENDING_TTL_MS
        val releasesExpired = (nowMs - releasesSnap.fetchedAtMs) >= NEW_RELEASES_TTL_MS
        return trendingExpired || releasesExpired
    }

    fun getHomeLastUpdatedMs(countryCode: String): Long {
        val code = countryCode.uppercase(Locale.ROOT)
        val t1 = trendingSnapshots[code]?.fetchedAtMs ?: 0L
        val t2 = newReleasesSnapshots[code]?.fetchedAtMs ?: 0L
        return maxOf(t1, t2)
    }

    fun hasLiveProviderItems(countryCode: String): Boolean {
        val code = countryCode.uppercase(Locale.ROOT)
        return (trendingSnapshots[code]?.hasLiveProviderItems == true) ||
            (newReleasesSnapshots[code]?.hasLiveProviderItems == true)
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
            term.trim().lowercase(Locale.ROOT),
            (countryCode ?: "").trim().uppercase(Locale.ROOT),
            (genreFilter ?: "").trim().lowercase(Locale.ROOT),
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

        val targetCountry = CountriesData.getCountryByCode(countryCode ?: "IR")
        val combinedPool = mutableListOf<Track>()
        var encounteredNetworkError = false

        // 1. Curated Catalog (APPROVED — always fast and available offline/Iran No-VPN)
        val baseCurated = if (!countryCode.isNullOrBlank() && term.isBlank()) {
            getCuratedTracksForCountry(targetCountry, favoriteIds)
        } else {
            getAllCuratedFullTracks(favoriteIds)
        }

        val curatedMatches = if (term.isBlank()) {
            baseCurated
        } else {
            baseCurated.filter {
                it.title.contains(term, ignoreCase = true) ||
                    it.artist.contains(term, ignoreCase = true) ||
                    it.album.contains(term, ignoreCase = true) ||
                    it.genre.contains(term, ignoreCase = true)
            }
        }
        combinedPool.addAll(curatedMatches)

        // 2. Official Apple iTunes Search API (APPROVED — metadata, exact releaseDate, artwork, and 30s preview)
        if (!iranNoVpnMode && term.isNotBlank()) {
            val itunesTracks = itunesAdapter.search(
                query = term,
                country = targetCountry,
                favoriteIds = favoriteIds,
                limit = 20
            )
            if (itunesTracks.isEmpty() && !SourceResolver.isHostHealthy("itunes.apple.com")) {
                encounteredNetworkError = true
            }
            combinedPool.addAll(itunesTracks)
        }

        // 3. Internet Archive Open Audio API (CONDITIONAL — open-access full audio streams)
        if (!iranNoVpnMode && term.isNotBlank()) {
            val archiveTracks = archiveAdapter.search(
                query = term,
                country = targetCountry,
                favoriteIds = favoriteIds,
                limit = 4
            )
            combinedPool.addAll(archiveTracks)
        }

        var results = GroupEngine.deduplicateTracks(
            combinedPool.filter {
                it.title.isNotBlank() && it.artist.isNotBlank() && SourceResolver.isValidStreamUrl(it.previewUrl)
            }
        )

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

        // Also enrich artist discography from official iTunes Search when reachable
        if (cleanTarget.isNotBlank() && SourceResolver.isHostHealthy("itunes.apple.com")) {
            val liveItunes = itunesAdapter.search(
                query = cleanTarget,
                country = CountriesData.usa,
                favoriteIds = favoriteIds,
                limit = 12
            ).filter {
                it.artistId == targetSlug || it.artist.contains(cleanTarget, ignoreCase = true)
            }
            matched.addAll(liveItunes)
        }

        val unique = GroupEngine.deduplicateTracks(
            matched.filter {
                it.title.isNotBlank() && it.artist.isNotBlank() && SourceResolver.isValidStreamUrl(it.previewUrl)
            }
        )

        when (sortOption) {
            ArtistSortOption.TRENDING -> unique.sortedWith(
                compareByDescending<Track> { !it.isPreviewOnly }
                    .thenByDescending { it.playCount }
                    .thenBy { it.id }
            )
            ArtistSortOption.NEWEST -> unique.sortedWith(
                compareByDescending<Track> { it.computeReleaseSortKey() }
                    .thenByDescending { it.datePrecision.weight }
                    .thenBy { it.id }
            )
        }
    }

    /**
     * Section 2.1, 2.2, and 2.6:
     * Evaluates Latest Releases for [country] with Stale-While-Revalidate caching (TTL = 30 min),
     * live provider discovery where available, date-precision ranking, and `perArtistCap = 1`.
     */
    suspend fun getNewReleasesForCountry(
        country: Country,
        forceRefresh: Boolean = false,
        iranNoVpnMode: Boolean = true
    ): List<Track> = withContext(Dispatchers.IO) {
        val code = country.code.uppercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val existingSnap = newReleasesSnapshots[code]

        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("getNewReleasesForCountry", "Failed reading favorites", e)
            emptySet()
        }

        if (!forceRefresh && existingSnap != null && (now - existingSnap.fetchedAtMs) < NEW_RELEASES_TTL_MS) {
            return@withContext existingSnap.tracks.map { it.copy(isFavorite = favoriteIds.contains(it.id)) }
        }

        val curatedTracks = getCuratedTracksForCountry(country, favoriteIds)
        val liveTracks = if (!iranNoVpnMode && SourceResolver.isHostHealthy("itunes.apple.com")) {
            itunesAdapter.fetchLatestReleases(country, favoriteIds, limit = 10)
        } else {
            emptyList()
        }

        val mergedPool = GroupEngine.deduplicateTracks(curatedTracks + liveTracks)
        val group = CatalogDataSource.getDefaultNewReleasesGroup(code)
        val evaluated = GroupEngine.evaluate(
            group = group,
            tracks = mergedPool,
            artists = CatalogDataSource.getArtistsById()
        )

        val finalTracks = if (evaluated.isNotEmpty()) {
            evaluated
        } else {
            existingSnap?.tracks ?: curatedTracks.take(10)
        }

        newReleasesSnapshots[code] = HomeSectionSnapshot(
            tracks = finalTracks,
            fetchedAtMs = now,
            hasLiveProviderItems = liveTracks.isNotEmpty()
        )
        finalTracks
    }

    /**
     * Section 2.1, 2.3, and 2.6:
     * Evaluates Popular / Editor Picks for [country] with Stale-While-Revalidate caching (TTL = 15 min),
     * cross-shelf deduplication against [excludeTrackIds], and honest popularity attribution.
     */
    suspend fun getTrendingForCountry(
        country: Country,
        forceRefresh: Boolean = false,
        iranNoVpnMode: Boolean = true,
        excludeTrackIds: Set<Long> = emptySet()
    ): List<Track> = withContext(Dispatchers.IO) {
        val code = country.code.uppercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val existingSnap = trendingSnapshots[code]

        val favoriteIds = try {
            musicDao.getAllFavoriteIds().first().toSet()
        } catch (e: Exception) {
            AppLogger.w("getTrendingForCountry", "Failed reading favorites", e)
            emptySet()
        }

        if (!forceRefresh && existingSnap != null && (now - existingSnap.fetchedAtMs) < TRENDING_TTL_MS) {
            val cached = existingSnap.tracks.map { it.copy(isFavorite = favoriteIds.contains(it.id)) }
            val deduped = if (excludeTrackIds.isNotEmpty()) {
                cached.filter { it.id !in excludeTrackIds }.ifEmpty { cached }
            } else {
                cached
            }
            return@withContext deduped
        }

        val curatedTracks = getCuratedTracksForCountry(country, favoriteIds)
        val livePopular = if (!iranNoVpnMode && SourceResolver.isHostHealthy("itunes.apple.com")) {
            itunesAdapter.fetchPopularTracks(country, favoriteIds, limit = 10)
        } else {
            emptyList()
        }

        val mergedPool = GroupEngine.deduplicateTracks(curatedTracks + livePopular)
        val group = CatalogDataSource.getDefaultTrendingGroup(code)
        val evaluatedDedup = GroupEngine.evaluate(
            group = group,
            tracks = mergedPool,
            artists = CatalogDataSource.getArtistsById(),
            excludeTrackIds = excludeTrackIds
        )
        val evaluated = if (evaluatedDedup.isNotEmpty()) {
            evaluatedDedup
        } else {
            GroupEngine.evaluate(
                group = group,
                tracks = mergedPool,
                artists = CatalogDataSource.getArtistsById()
            )
        }

        val finalTracks = evaluated.mapIndexed { idx, track ->
            val rank = idx + 1
            val signal = if (track.contentOrigin == ContentOrigin.LIVE_PROVIDER) {
                PopularitySignal(
                    sourceId = track.sourceId,
                    labelFa = "محبوب در ${track.sourceLabelFa} #$rank",
                    labelEn = "${track.sourceLabelEn} Top #$rank",
                    rank = rank,
                    retrievedAtMs = now,
                    isVerifiedLiveSignal = true
                )
            } else {
                PopularitySignal(
                    sourceId = "curated",
                    labelFa = "منتخب کاتالوگ #$rank",
                    labelEn = "Editor Pick #$rank",
                    rank = rank,
                    retrievedAtMs = now,
                    isVerifiedLiveSignal = false
                )
            }
            track.copy(popularitySignal = signal)
        }

        trendingSnapshots[code] = HomeSectionSnapshot(
            tracks = finalTracks,
            fetchedAtMs = now,
            hasLiveProviderItems = livePopular.isNotEmpty()
        )
        finalTracks
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
        val code = country.code.uppercase(Locale.ROOT)
        return CatalogDataSource.getTracks()
            .filter { it.countryCode.equals(code, ignoreCase = true) }
            .map { decorateCuratedCatalogTrack(it, favoriteIds) }
    }

    fun getAllCuratedFullTracks(favoriteIds: Set<Long> = emptySet()): List<Track> {
        return CatalogDataSource.getTracks()
            .map { decorateCuratedCatalogTrack(it, favoriteIds) }
    }

    private fun decorateCuratedCatalogTrack(track: Track, favoriteIds: Set<Long>): Track {
        val yearClean = track.releaseYear.take(4)
        return track.copy(
            isFavorite = favoriteIds.contains(track.id),
            releaseDateIso = yearClean,
            datePrecision = DatePrecision.YEAR,
            releaseKind = if (track.album.equals("Single", ignoreCase = true)) ReleaseKind.SINGLE else ReleaseKind.ALBUM,
            contentOrigin = ContentOrigin.CURATED_CATALOG,
            sourceLabelFa = "آرشیو منتخب",
            sourceLabelEn = "Curated Catalog",
            isPreviewOnly = false,
            popularitySignal = PopularitySignal(
                sourceId = "curated",
                labelFa = "منتخب کاتالوگ",
                labelEn = "Curated Pick",
                rank = null,
                isVerifiedLiveSignal = false
            )
        )
    }

    companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val TRENDING_TTL_MS = 15 * 60 * 1000L
        const val NEW_RELEASES_TTL_MS = 30 * 60 * 1000L
        const val BANNERS_TTL_MS = 6 * 60 * 60 * 1000L
        const val MAX_CACHE_ENTRIES = 40
    }
}
