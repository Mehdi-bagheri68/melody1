package com.example.data.provider

import com.example.data.api.ArchiveApiService
import com.example.data.api.ItunesApiService
import com.example.data.model.AppLanguage
import com.example.data.model.ContentOrigin
import com.example.data.model.Country
import com.example.data.model.DatePrecision
import com.example.data.model.PopularitySignal
import com.example.data.model.ReleaseKind
import com.example.data.model.Track
import com.example.data.repository.ArtworkResolver
import com.example.data.repository.CatalogDataSource
import com.example.data.repository.SourceResolver
import com.example.util.AppLogger
import com.example.util.IdGenerator
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * Section 3.1 & Section 8: Provider Evaluation Status Matrix.
 */
enum class ProviderStatus {
    APPROVED,
    CHANGED,
    CONDITIONAL,
    METADATA_ONLY,
    BLOCKED_NO_BACKEND,
    REJECTED,
    NOT_TESTED
}

enum class ProviderCapability {
    SEARCH,
    LATEST_RELEASES,
    POPULAR_CHART,
    STREAM_FULL,
    STREAM_PREVIEW,
    ARTWORK,
    METADATA_ENRICHMENT,
    SMS_OTP
}

data class ProviderMatrixEntry(
    val id: String,
    val displayName: String,
    val docsUrl: String,
    val authModel: String,
    val capabilities: Set<ProviderCapability>,
    val rateLimitNotes: String,
    val termsNotes: String,
    val status: ProviderStatus
)

/**
 * Central registry documenting every evaluated provider and its verified legal/technical status.
 */
object ProviderRegistry {
    val allProviders: List<ProviderMatrixEntry> = listOf(
        ProviderMatrixEntry(
            id = "curated",
            displayName = "Bundled Curated Catalog",
            docsUrl = "assets/catalog/tracks.json",
            authModel = "None (Bundled Offline-Ready Asset)",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.LATEST_RELEASES,
                ProviderCapability.POPULAR_CHART,
                ProviderCapability.STREAM_FULL,
                ProviderCapability.ARTWORK
            ),
            rateLimitNotes = "Local asset; zero network rate limits.",
            termsNotes = "Explicitly labeled as 'Curated Catalog / آرشیو منتخب' so it never masquerades as a live global chart.",
            status = ProviderStatus.APPROVED
        ),
        ProviderMatrixEntry(
            id = "itunes",
            displayName = "Apple iTunes Search API",
            docsUrl = "https://performance-partners.apple.com/search-api",
            authModel = "Public HTTPS GET (No client secret required)",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.LATEST_RELEASES,
                ProviderCapability.POPULAR_CHART,
                ProviderCapability.STREAM_PREVIEW,
                ProviderCapability.ARTWORK,
                ProviderCapability.METADATA_ENRICHMENT
            ),
            rateLimitNotes = "~20 calls/minute per IP; cached with 15-30m TTL.",
            termsNotes = "Provides official metadata, releaseDate ISO timestamps, artwork, and 30-second audio previews. Explicitly labeled as 'iTunes 30s Preview'.",
            status = ProviderStatus.APPROVED
        ),
        ProviderMatrixEntry(
            id = "internet_archive",
            displayName = "Internet Archive Open Audio API",
            docsUrl = "https://archive.org/developers/index-apis.html",
            authModel = "Public HTTPS GET (advancedsearch.php & metadata API)",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.LATEST_RELEASES,
                ProviderCapability.STREAM_FULL,
                ProviderCapability.METADATA_ENRICHMENT
            ),
            rateLimitNotes = "Respectful query rate; cached per country/query.",
            termsNotes = "Restricted to open-access audio collections (`mediatype:audio` AND `opensource_audio`) with source attribution (`Internet Archive`).",
            status = ProviderStatus.CONDITIONAL
        ),
        ProviderMatrixEntry(
            id = "musicbrainz",
            displayName = "MusicBrainz & Cover Art Archive",
            docsUrl = "https://musicbrainz.org/doc/MusicBrainz_API",
            authModel = "Public HTTPS GET with descriptive User-Agent",
            capabilities = setOf(
                ProviderCapability.METADATA_ENRICHMENT,
                ProviderCapability.ARTWORK
            ),
            rateLimitNotes = "1 request/second per IP.",
            termsNotes = "Open metadata/artwork encyclopedia only; does not host audio streams.",
            status = ProviderStatus.METADATA_ONLY
        ),
        ProviderMatrixEntry(
            id = "audius",
            displayName = "Audius Open Music API",
            docsUrl = "https://docs.audius.org/developers/api",
            authModel = "Public Discovery Node REST API (app_name parameter)",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.POPULAR_CHART,
                ProviderCapability.STREAM_FULL
            ),
            rateLimitNotes = "Node-dependent rate limits.",
            termsNotes = "Approved for artist-uploaded open catalog; not yet wired as default regional provider.",
            status = ProviderStatus.NOT_TESTED
        ),
        ProviderMatrixEntry(
            id = "jamendo",
            displayName = "Jamendo Music API v3.0",
            docsUrl = "https://developer.jamendo.com/v3.0",
            authModel = "Requires registered client_id",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.STREAM_FULL,
                ProviderCapability.ARTWORK
            ),
            rateLimitNotes = "Requires registered app key.",
            termsNotes = "Independent/Creative Commons catalog; requires non-embedded partner client_id.",
            status = ProviderStatus.BLOCKED_NO_BACKEND
        ),
        ProviderMatrixEntry(
            id = "deezer",
            displayName = "Deezer Public API",
            docsUrl = "https://developers.deezer.com/api",
            authModel = "OAuth / App Registration",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.POPULAR_CHART,
                ProviderCapability.STREAM_PREVIEW,
                ProviderCapability.ARTWORK
            ),
            rateLimitNotes = "50 requests / 5 seconds.",
            termsNotes = "30-second preview & chart metadata only; new developer app creation is restricted.",
            status = ProviderStatus.CONDITIONAL
        ),
        ProviderMatrixEntry(
            id = "soundcloud",
            displayName = "SoundCloud Public API",
            docsUrl = "https://developers.soundcloud.com/docs/api/guide",
            authModel = "OAuth 2.1 with server-held client secret",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.STREAM_FULL
            ),
            rateLimitNotes = "Strict token rate limits.",
            termsNotes = "Cannot safely embed client_secret in a public APK without a backend token broker.",
            status = ProviderStatus.BLOCKED_NO_BACKEND
        ),
        ProviderMatrixEntry(
            id = "spotify",
            displayName = "Spotify Web API",
            docsUrl = "https://developer.spotify.com/documentation/web-api",
            authModel = "OAuth 2.0 / PKCE or Client Credentials",
            capabilities = setOf(
                ProviderCapability.SEARCH,
                ProviderCapability.METADATA_ENRICHMENT,
                ProviderCapability.ARTWORK
            ),
            rateLimitNotes = "Dynamic rolling window.",
            termsNotes = "Preview URLs and unauthenticated chart endpoints deprecated/restricted for standalone third-party players.",
            status = ProviderStatus.CHANGED
        ),
        ProviderMatrixEntry(
            id = "musics_fa_scraper",
            displayName = "Musics-Fa HTML Scraper",
            docsUrl = "https://musics-fa.com/",
            authModel = "Unauthenticated HTML Scraping",
            capabilities = emptySet(),
            rateLimitNotes = "Subject to Cloudflare/anti-bot blocks and DOM breakage.",
            termsNotes = "Disabled by default per Section 0 Rule 6 & Section 3.1 (No unauthorized HTML scraping).",
            status = ProviderStatus.REJECTED
        ),
        ProviderMatrixEntry(
            id = "youtube_extractor",
            displayName = "YouTube Raw Audio Extractor",
            docsUrl = "https://developers.google.com/youtube/terms/api-services-terms-of-service",
            authModel = "N/A",
            capabilities = emptySet(),
            rateLimitNotes = "N/A",
            termsNotes = "Direct audio stream extraction violates YouTube Terms of Service and Google Play policy.",
            status = ProviderStatus.REJECTED
        ),
        ProviderMatrixEntry(
            id = "firebase_phone_auth",
            displayName = "Firebase Authentication Phone SMS",
            docsUrl = "https://firebase.google.com/docs/auth/android/phone-auth",
            authModel = "Firebase SDK + Play Integrity / reCAPTCHA",
            capabilities = setOf(ProviderCapability.SMS_OTP),
            rateLimitNotes = "Requires Blaze plan / SMS quota in production Firebase project.",
            termsNotes = "Requires active Firebase Phone Auth provider & SMS billing in the Firebase project.",
            status = ProviderStatus.BLOCKED_NO_BACKEND
        ),
        ProviderMatrixEntry(
            id = "custom_sms_gateway",
            displayName = "Server-Side SMS OTP Gateway (Twilio / Kavenegar)",
            docsUrl = "https://www.twilio.com/docs/verify/api",
            authModel = "Server-to-Server API Secret",
            capabilities = setOf(ProviderCapability.SMS_OTP),
            rateLimitNotes = "Per-SMS carrier billing.",
            termsNotes = "Cannot embed SMS gateway secret in client APK; requires a dedicated backend.",
            status = ProviderStatus.BLOCKED_NO_BACKEND
        )
    )

    fun getStatus(providerId: String): ProviderStatus =
        allProviders.find { it.id.equals(providerId, ignoreCase = true) }?.status
            ?: ProviderStatus.NOT_TESTED
}

/**
 * Clean provider adapter contract (Section 2.4).
 */
interface MusicProviderAdapter {
    val providerId: String
    val status: ProviderStatus
    suspend fun fetchLatestReleases(country: Country, favoriteIds: Set<Long>, limit: Int = 15): List<Track>
    suspend fun fetchPopularTracks(country: Country, favoriteIds: Set<Long>, limit: Int = 15): List<Track>
    suspend fun search(query: String, country: Country, favoriteIds: Set<Long>, limit: Int = 25): List<Track>
}

/**
 * Adapter 1: Bundled Curated Catalog (`APPROVED` — always available offline & online,
 * explicitly labeled as Curated Catalog / آرشیو منتخب).
 */
class CuratedCatalogProviderAdapter : MusicProviderAdapter {
    override val providerId: String = "curated"
    override val status: ProviderStatus = ProviderStatus.APPROVED

    override suspend fun fetchLatestReleases(
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        val code = country.code.uppercase(Locale.ROOT)
        return CatalogDataSource.getTracks()
            .filter { it.countryCode.equals(code, ignoreCase = true) }
            .map { decorateCuratedTrack(it, favoriteIds, isPopularShelf = false, rank = null) }
    }

    override suspend fun fetchPopularTracks(
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        val code = country.code.uppercase(Locale.ROOT)
        return CatalogDataSource.getTracks()
            .filter { it.countryCode.equals(code, ignoreCase = true) }
            .sortedWith(compareByDescending<Track> { it.playCount }.thenBy { it.id })
            .mapIndexed { idx, track ->
                decorateCuratedTrack(track, favoriteIds, isPopularShelf = true, rank = idx + 1)
            }
            .take(limit)
    }

    override suspend fun search(
        query: String,
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        val clean = query.trim()
        val all = CatalogDataSource.getTracks()
        val filtered = if (clean.isBlank()) {
            all.filter { it.countryCode.equals(country.code, ignoreCase = true) }
        } else {
            all.filter {
                it.title.contains(clean, ignoreCase = true) ||
                    it.artist.contains(clean, ignoreCase = true) ||
                    it.album.contains(clean, ignoreCase = true) ||
                    it.genre.contains(clean, ignoreCase = true)
            }
        }
        return filtered.take(limit).map { decorateCuratedTrack(it, favoriteIds, isPopularShelf = false, rank = null) }
    }

    private fun decorateCuratedTrack(
        track: Track,
        favoriteIds: Set<Long>,
        isPopularShelf: Boolean,
        rank: Int?
    ): Track {
        val yearClean = track.releaseYear.take(4)
        val signal = if (isPopularShelf && rank != null) {
            PopularitySignal(
                sourceId = "curated",
                labelFa = "منتخب سردبیر #$rank",
                labelEn = "Editor Pick #$rank",
                rank = rank,
                isVerifiedLiveSignal = false
            )
        } else {
            PopularitySignal(
                sourceId = "curated",
                labelFa = "آرشیو منتخب",
                labelEn = "Curated Catalog",
                rank = rank,
                isVerifiedLiveSignal = false
            )
        }
        return track.copy(
            isFavorite = favoriteIds.contains(track.id),
            releaseDateIso = yearClean,
            datePrecision = DatePrecision.YEAR,
            releaseKind = if (track.album.equals("Single", ignoreCase = true)) ReleaseKind.SINGLE else ReleaseKind.ALBUM,
            contentOrigin = ContentOrigin.CURATED_CATALOG,
            sourceLabelFa = "آرشیو منتخب",
            sourceLabelEn = "Curated Catalog",
            isPreviewOnly = false,
            popularitySignal = signal
        )
    }
}

/**
 * Adapter 2: Official Apple iTunes Search API (`APPROVED` for metadata, exact release dates,
 * artwork, and 30-second preview streams — explicitly labeled as 30s Previews).
 */
class ItunesProviderAdapter(
    private val itunesApi: ItunesApiService
) : MusicProviderAdapter {
    override val providerId: String = "itunes"
    override val status: ProviderStatus = ProviderStatus.APPROVED

    override suspend fun fetchLatestReleases(
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        if (!SourceResolver.isHostHealthy("itunes.apple.com")) return emptyList()
        val storeCountry = mapStorefrontCountry(country.code)
        val query = country.topArtists.firstOrNull() ?: country.defaultSearchTerm
        return try {
            val response = itunesApi.searchTracks(
                term = query,
                country = storeCountry,
                limit = (limit * 2).coerceAtMost(40)
            )
            SourceResolver.recordSuccess("https://itunes.apple.com/search")
            response.results.mapNotNull { dto ->
                mapDtoToTrack(dto, country.code, favoriteIds, rank = null, isPopularQuery = false)
            }
                .sortedWith(
                    compareByDescending<Track> { it.computeReleaseSortKey() }
                        .thenByDescending { it.datePrecision.weight }
                        .thenBy { it.id }
                )
                .take(limit)
        } catch (e: Exception) {
            SourceResolver.recordFailure("https://itunes.apple.com/search")
            AppLogger.w("ItunesProviderAdapter", "fetchLatestReleases failed for ${country.code}", e)
            emptyList()
        }
    }

    override suspend fun fetchPopularTracks(
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        if (!SourceResolver.isHostHealthy("itunes.apple.com")) return emptyList()
        val storeCountry = mapStorefrontCountry(country.code)
        val term = country.defaultSearchTerm.ifBlank { "Top Hits" }
        return try {
            val response = itunesApi.searchTracks(
                term = term,
                country = storeCountry,
                limit = (limit * 2).coerceAtMost(40)
            )
            SourceResolver.recordSuccess("https://itunes.apple.com/search")
            response.results.mapIndexedNotNull { index, dto ->
                mapDtoToTrack(dto, country.code, favoriteIds, rank = index + 1, isPopularQuery = true)
            }.take(limit)
        } catch (e: Exception) {
            SourceResolver.recordFailure("https://itunes.apple.com/search")
            AppLogger.w("ItunesProviderAdapter", "fetchPopularTracks failed for ${country.code}", e)
            emptyList()
        }
    }

    override suspend fun search(
        query: String,
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        if (query.isBlank() || !SourceResolver.isHostHealthy("itunes.apple.com")) return emptyList()
        val storeCountry = mapStorefrontCountry(country.code)
        return try {
            val response = itunesApi.searchTracks(
                term = query.trim(),
                country = storeCountry,
                limit = limit
            )
            SourceResolver.recordSuccess("https://itunes.apple.com/search")
            response.results.mapIndexedNotNull { idx, dto ->
                mapDtoToTrack(dto, country.code, favoriteIds, rank = idx + 1, isPopularQuery = false)
            }
        } catch (e: Exception) {
            SourceResolver.recordFailure("https://itunes.apple.com/search")
            AppLogger.w("ItunesProviderAdapter", "search failed for query=$query", e)
            emptyList()
        }
    }

    private fun mapStorefrontCountry(code: String): String {
        return when (code.uppercase(Locale.ROOT)) {
            "IR" -> "US" // iTunes has no IR storefront; Persian artists are indexed on the US/global storefront
            "TR", "US", "GB", "ES", "FR", "DE" -> code.uppercase(Locale.ROOT)
            else -> "US"
        }
    }

    private fun mapDtoToTrack(
        dto: com.example.data.api.ItunesTrackDto,
        countryCode: String,
        favoriteIds: Set<Long>,
        rank: Int?,
        isPopularQuery: Boolean
    ): Track? {
        val title = dto.trackName?.trim().orEmpty()
        val artist = dto.artistName?.trim().orEmpty()
        val streamUrl = dto.previewUrl?.trim().orEmpty()
        if (title.isBlank() || artist.isBlank() || !SourceResolver.isValidStreamUrl(streamUrl)) {
            return null
        }
        val id = dto.trackId?.let { 2_000_000L + kotlin.math.abs(it) }
            ?: IdGenerator.stable64BitId("itunes", streamUrl, offset = 2_000_000L)
        val artistId = Track.slugifyArtistId(artist)
        val rawCover = dto.artworkUrl100?.replace("100x100bb", "600x600bb").orEmpty()
        val ownedArt = ArtworkResolver.buildOwnedArtworkOrNull(rawCover, artistId)

        val rawIso = dto.releaseDate?.trim().orEmpty()
        val isoDate = if (rawIso.length >= 10) rawIso.take(10) else rawIso.take(4)
        val precision = DatePrecision.inferFromIso(isoDate)
        val releaseYear = isoDate.take(4).ifBlank { "2025" }
        val collectionName = dto.collectionName?.trim().orEmpty()
        val releaseKind = when {
            collectionName.contains(" - EP", ignoreCase = true) -> ReleaseKind.EP
            collectionName.contains(" - Single", ignoreCase = true) || collectionName.isBlank() -> ReleaseKind.SINGLE
            else -> ReleaseKind.ALBUM
        }

        val signal = PopularitySignal(
            sourceId = "itunes",
            labelFa = if (isPopularQuery && rank != null) "محبوب در iTunes #$rank" else "پیش‌نمایش iTunes",
            labelEn = if (isPopularQuery && rank != null) "iTunes Top #$rank" else "iTunes Preview",
            rank = rank,
            isVerifiedLiveSignal = true
        )

        return Track(
            id = id,
            title = title,
            artist = artist,
            album = collectionName.ifBlank { "Single" },
            artworkUrl = ownedArt?.url ?: "",
            previewUrl = streamUrl,
            genre = dto.primaryGenreName?.takeIf { it.isNotBlank() } ?: "Pop",
            durationMs = (dto.trackTimeMillis ?: 30_000L).coerceAtLeast(0L),
            releaseYear = releaseYear,
            countryCode = countryCode,
            isFavorite = favoriteIds.contains(id),
            playCount = 0, // Never fabricate play counts for iTunes tracks
            sourceId = "itunes",
            artistId = artistId,
            artwork = ownedArt,
            releaseDateIso = isoDate.ifBlank { null },
            datePrecision = precision,
            releaseKind = releaseKind,
            contentOrigin = ContentOrigin.LIVE_PROVIDER,
            sourceLabelFa = "iTunes",
            sourceLabelEn = "iTunes",
            isPreviewOnly = true,
            popularitySignal = signal
        )
    }
}

/**
 * Adapter 3: Internet Archive Open Audio Adapter (`CONDITIONAL` — queries open-access audio items
 * and resolves playable MP3 files with explicit attribution).
 */
class InternetArchiveProviderAdapter(
    private val archiveApi: ArchiveApiService
) : MusicProviderAdapter {
    override val providerId: String = "internet_archive"
    override val status: ProviderStatus = ProviderStatus.CONDITIONAL

    override suspend fun fetchLatestReleases(
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> = emptyList() // Archive.org is an archival repository, not a frontline commercial new-release feed

    override suspend fun fetchPopularTracks(
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> = search(country.defaultSearchTerm, country, favoriteIds, limit = limit.coerceAtMost(6))

    override suspend fun search(
        query: String,
        country: Country,
        favoriteIds: Set<Long>,
        limit: Int
    ): List<Track> {
        val clean = query.trim()
        if (clean.isBlank() || !SourceResolver.isHostHealthy("archive.org")) return emptyList()
        return try {
            val luceneQuery = "mediatype:(audio) AND collection:(opensource_audio) AND ($clean)"
            val searchResp = archiveApi.searchAudio(query = luceneQuery, rows = limit.coerceAtMost(8))
            val docs = searchResp.response?.docs.orEmpty()
            val resolved = mutableListOf<Track>()
            for (doc in docs.take(4)) {
                val identifier = doc.identifier?.trim().orEmpty()
                if (identifier.isBlank()) continue
                val meta = try {
                    archiveApi.getFiles(identifier)
                } catch (_: Exception) {
                    null
                } ?: continue
                val filePool = meta.result ?: meta.files.orEmpty()
                val mp3File = filePool.firstOrNull { file ->
                    val name = file.name.lowercase(Locale.ROOT)
                    val format = file.format.orEmpty().lowercase(Locale.ROOT)
                    name.endsWith(".mp3") && (format.contains("mp3") || format.isBlank())
                } ?: continue
                val fileName = mp3File.name.trim()
                if (fileName.isBlank()) continue
                val streamUrl = "https://archive.org/download/$identifier/${java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")}"
                if (!SourceResolver.isValidStreamUrl(streamUrl)) continue

                val title = (mp3File.title ?: doc.title ?: identifier).trim()
                val artist = (mp3File.artist ?: doc.creator ?: "Open Audio Archive").trim()
                val artistId = Track.slugifyArtistId(artist)
                val id = IdGenerator.stable64BitId("archive", streamUrl, offset = 4_000_000L)
                val durationSec = mp3File.length?.toDoubleOrNull()?.toLong() ?: 0L
                val rawDate = doc.year?.trim().orEmpty()
                val precision = DatePrecision.inferFromIso(rawDate)

                resolved.add(
                    Track(
                        id = id,
                        title = title,
                        artist = artist,
                        album = doc.title?.takeIf { it.isNotBlank() } ?: "Internet Archive",
                        artworkUrl = "",
                        previewUrl = streamUrl,
                        genre = "Open Audio",
                        durationMs = durationSec * 1000L,
                        releaseYear = rawDate.take(4).ifBlank { "2024" },
                        countryCode = country.code,
                        isFavorite = favoriteIds.contains(id),
                        playCount = 0,
                        sourceId = "internet_archive",
                        artistId = artistId,
                        artwork = null,
                        releaseDateIso = rawDate.ifBlank { null },
                        datePrecision = precision,
                        releaseKind = ReleaseKind.SINGLE,
                        contentOrigin = ContentOrigin.LIVE_PROVIDER,
                        sourceLabelFa = "آرشیو آزاد (Archive.org)",
                        sourceLabelEn = "Internet Archive",
                        isPreviewOnly = false,
                        popularitySignal = PopularitySignal(
                            sourceId = "internet_archive",
                            labelFa = "کاتالوگ آزاد Archive.org",
                            labelEn = "Open Audio • Archive.org",
                            rank = null,
                            isVerifiedLiveSignal = true
                        )
                    )
                )
            }
            SourceResolver.recordSuccess("https://archive.org/advancedsearch.php")
            resolved
        } catch (e: Exception) {
            SourceResolver.recordFailure("https://archive.org/advancedsearch.php")
            AppLogger.w("InternetArchiveProviderAdapter", "Archive search failed for query=$clean", e)
            emptyList()
        }
    }
}

// =========================================================================
// SECTION 2.5: DYNAMIC EDITORIAL BANNERS & CAMPAIGN VALIDATION
// =========================================================================

enum class EditorialBannerActionType {
    MOOD,
    COUNTRY,
    SEARCH,
    ARTIST
}

data class EditorialBanner(
    val id: String,
    val titleFa: String,
    val titleEn: String,
    val subtitleFa: String,
    val subtitleEn: String,
    val badgeFa: String,
    val badgeEn: String,
    val ctaFa: String,
    val ctaEn: String,
    val imageUrl: String = "",
    val accentHex: Long = 0xFFFFB800,
    val secondaryHex: Long = 0xFF00BFA5,
    val actionType: EditorialBannerActionType = EditorialBannerActionType.MOOD,
    val actionTarget: String = "newest",
    val countryCodes: Set<String> = emptySet(), // empty = all countries
    val startsAtIso: String = "2025-01-01T00:00:00Z",
    val endsAtIso: String = "2028-12-31T23:59:59Z",
    val priority: Int = 10,
    val isActive: Boolean = true,
    val contentVersion: Int = 1
) {
    fun getTitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) titleFa else titleEn
    fun getSubtitle(lang: AppLanguage): String = if (lang == AppLanguage.FA) subtitleFa else subtitleEn
    fun getBadge(lang: AppLanguage): String = if (lang == AppLanguage.FA) badgeFa else badgeEn
    fun getCta(lang: AppLanguage): String = if (lang == AppLanguage.FA) ctaFa else ctaEn
}

object EditorialBannerValidator {
    private val ALLOWED_MOOD_TARGETS = setOf(
        "newest", "pop", "party", "drive", "chill", "traditional", "workout", "romance"
    )
    private val ALLOWED_COUNTRY_CODES = setOf("IR", "TR", "US", "GB", "ES", "FR", "DE")

    fun isValid(
        banner: EditorialBanner,
        countryCode: String? = null,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!banner.isActive) return false
        if (banner.id.isBlank() || banner.titleFa.isBlank() || banner.titleEn.isBlank()) return false
        if (banner.actionTarget.isBlank() || banner.actionTarget.contains("javascript:", ignoreCase = true)) {
            return false
        }

        // Validate imageUrl if present (must be local asset or HTTPS; never arbitrary HTTP/script)
        if (banner.imageUrl.isNotBlank()) {
            val validImage = banner.imageUrl.startsWith("https://") ||
                banner.imageUrl.startsWith("file:///android_asset/")
            if (!validImage) return false
        }

        // Validate allowlisted action targets
        val validTarget = when (banner.actionType) {
            EditorialBannerActionType.MOOD -> banner.actionTarget in ALLOWED_MOOD_TARGETS
            EditorialBannerActionType.COUNTRY -> banner.actionTarget.uppercase(Locale.ROOT) in ALLOWED_COUNTRY_CODES
            EditorialBannerActionType.SEARCH -> banner.actionTarget.length in 1..80
            EditorialBannerActionType.ARTIST -> banner.actionTarget.length in 1..80
        }
        if (!validTarget) return false

        // Validate schedule window
        val startMs = parseIsoToEpochMs(banner.startsAtIso) ?: return false
        val endMs = parseIsoToEpochMs(banner.endsAtIso) ?: return false
        if (nowMs < startMs || nowMs > endMs) return false

        // Validate country targeting
        if (!countryCode.isNullOrBlank() && banner.countryCodes.isNotEmpty()) {
            val matchesCountry = banner.countryCodes.any { it.equals(countryCode, ignoreCase = true) }
            if (!matchesCountry) return false
        }

        return true
    }

    fun filterActiveForCountry(
        banners: List<EditorialBanner>,
        countryCode: String,
        nowMs: Long = System.currentTimeMillis()
    ): List<EditorialBanner> {
        return banners
            .filter { isValid(it, countryCode, nowMs) }
            .sortedByDescending { it.priority }
    }

    internal fun parseIsoToEpochMs(iso: String): Long? {
        return try {
            val clean = iso.trim()
            val pattern = if (clean.endsWith("Z")) "yyyy-MM-dd'T'HH:mm:ss'Z'" else "yyyy-MM-dd"
            val sdf = SimpleDateFormat(pattern, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            sdf.parse(clean)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun getDefaultEditorialBanners(): List<EditorialBanner> = listOf(
        EditorialBanner(
            id = "campaign_ir_curated_2026",
            titleFa = "گلچین موسیقی اصیل و پاپ ایران",
            titleEn = "Persian Pop & Classical Spotlight",
            subtitleFa = "آثار منتخب با کاور اختصاصی، کیفیت 320kbps و تفکیک دقیق هنرمندان",
            subtitleEn = "Curated Persian catalog with verified artist attribution & 320kbps streams",
            badgeFa = "کمپین ویژه ایران",
            badgeEn = "IRAN SPOTLIGHT",
            ctaFa = "کاوش تازه‌ها",
            ctaEn = "Explore Releases",
            accentHex = 0xFFFFB800,
            secondaryHex = 0xFF00BFA5,
            actionType = EditorialBannerActionType.MOOD,
            actionTarget = "newest",
            countryCodes = setOf("IR"),
            priority = 100
        ),
        EditorialBanner(
            id = "campaign_global_fresh_discovery",
            titleFa = "کشف موسیقی روز جهان و آرشیو منتخب",
            titleEn = "Global Discovery & Curated Picks",
            subtitleFa = "ترکیب پیش‌نمایش‌های رسمی iTunes و آرشیو کامل بدون نیاز به تحریم‌شکن",
            subtitleEn = "Combining official iTunes 30s previews with full-length curated streams",
            badgeFa = "کشف هوشمند",
            badgeEn = "SMART DISCOVERY",
            ctaFa = "شنیدن منتخب‌ها",
            ctaEn = "Listen Now",
            accentHex = 0xFF26A69A,
            secondaryHex = 0xFFFFB300,
            actionType = EditorialBannerActionType.MOOD,
            actionTarget = "pop",
            countryCodes = emptySet(),
            priority = 80
        ),
        EditorialBanner(
            id = "campaign_night_drive",
            titleFa = "پلی‌لیست اختصاصی رانندگی در شب",
            titleEn = "Late Night Drive Sessions",
            subtitleFa = "ریتم‌های منتخب برای تمرکز و آرامش در جاده",
            subtitleEn = "Curated electronic, pop, and acoustic tracks for the road",
            badgeFa = "ویژه جاده",
            badgeEn = "NIGHT DRIVE",
            ctaFa = "پخش پلی‌لیست جاده",
            ctaEn = "Start Drive Mix",
            accentHex = 0xFFFF7043,
            secondaryHex = 0xFF5C6BC0,
            actionType = EditorialBannerActionType.MOOD,
            actionTarget = "drive",
            countryCodes = emptySet(),
            priority = 60
        )
    )
}
