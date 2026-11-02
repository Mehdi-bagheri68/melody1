package com.example.data.repository

import com.example.data.model.CountriesData
import com.example.data.model.Track
import kotlin.math.abs

/**
 * Isolated, resilient HTML parser for live Persian music search results (Fixes BUG-04 & BUG-05).
 *
 * Features:
 * - Detects anti-bot / CAPTCHA pages so callers can disable degraded sources gracefully.
 * - Supports multiple attribute orders, single/double quotes, and lazy-loaded image attributes (`data-src`, `src`).
 * - Decodes HTML entities (`&#8211;`, `&amp;`, `&quot;`, etc.).
 * - Validates every extracted DTO before converting to [Track] (requires non-blank title, artist, and valid `.mp3` URL).
 * - Sets `durationMs = 0L` (unknown duration) instead of a fake hardcoded `215000L` (Fixes BUG-05), allowing Media3
 *   to populate the real duration once the stream is prepared.
 */
object MusicsFaHtmlParser {

    data class RawScrapedTrackDto(
        val rawTitleHeading: String,
        val mp3Url: String,
        val artworkUrl: String?
    )

    sealed class ParseOutcome {
        data class Success(val tracks: List<Track>) : ParseOutcome()
        object EmptyResult : ParseOutcome()
        object CaptchaOrBlocked : ParseOutcome()
        data class StructureChanged(val reason: String) : ParseOutcome()
    }

    private val articleRegex = Regex("<article\\b[^>]*>(.*?)</article>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val mp3Regex = Regex("""https?://dls\.musics-fa\.com/[^\s"'<>]+\.mp3""", RegexOption.IGNORE_CASE)
    private val imgRegex = Regex("""https?://musics-fa\.com/wp-content/uploads/[^\s"'<>]+\.(?:jpg|jpeg|png|webp)""", RegexOption.IGNORE_CASE)
    private val titleRegex = Regex("""<h[123]\b[^>]*>.*?<a\b[^>]*>(.*?)</a>""", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val htmlTagStripRegex = Regex("<[^>]+>")

    fun parseHtml(
        html: String,
        query: String,
        favoriteIds: Set<Long>,
        defaultArtist: String? = null,
        countryCode: String = "IR"
    ): ParseOutcome {
        if (html.isBlank()) return ParseOutcome.EmptyResult

        val lowerHtml = html.lowercase()
        if (lowerHtml.contains("cf-chl-bypass") ||
            lowerHtml.contains("g-recaptcha") ||
            lowerHtml.contains("just a moment...") ||
            lowerHtml.contains("attention required! | cloudflare")
        ) {
            return ParseOutcome.CaptchaOrBlocked
        }

        val articleMatches = articleRegex.findAll(html).toList()
        if (articleMatches.isEmpty()) {
            return if (lowerHtml.contains("موردی یافت نشد") || lowerHtml.contains("nothing found")) {
                ParseOutcome.EmptyResult
            } else {
                ParseOutcome.StructureChanged("No <article> blocks found in response")
            }
        }

        val validatedTracks = mutableListOf<Track>()

        for (match in articleMatches) {
            val block = match.groupValues.getOrNull(1) ?: continue
            val mp3Url = mp3Regex.find(block)?.value?.trim() ?: continue
            if (!SourceResolver.isValidStreamUrl(mp3Url)) continue

            val rawImgUrl = imgRegex.find(block)?.value?.trim()
            val fullImgUrl = rawImgUrl?.replace(Regex("-\\d+x\\d+(?=\\.(jpg|jpeg|png|webp)$)", RegexOption.IGNORE_CASE), "")
                ?: ""

            val rawHeadingHtml = titleRegex.find(block)?.groupValues?.getOrNull(1) ?: ""
            val decodedHeading = decodeHtmlEntities(htmlTagStripRegex.replace(rawHeadingHtml, "")).trim()

            val dto = RawScrapedTrackDto(
                rawTitleHeading = decodedHeading.ifBlank { query.trim() },
                mp3Url = mp3Url,
                artworkUrl = fullImgUrl
            )

            toValidatedTrack(dto, query, favoriteIds, defaultArtist, countryCode)?.let {
                validatedTracks.add(it)
            }
        }

        return if (validatedTracks.isEmpty()) {
            ParseOutcome.EmptyResult
        } else {
            ParseOutcome.Success(validatedTracks.distinctBy { it.previewUrl })
        }
    }

    private fun toValidatedTrack(
        dto: RawScrapedTrackDto,
        fallbackQuery: String,
        favoriteIds: Set<Long>,
        defaultArtist: String?,
        countryCode: String
    ): Track? {
        val cleaned = dto.rawTitleHeading
            .replace("دانلود آهنگ جدید", "")
            .replace("دانلود آهنگ", "")
            .replace("دانلود ریمیکس", "ریمیکس")
            .replace("با کیفیت عالی", "")
            .replace("پخش آنلاین", "")
            .trim()

        val artist = (defaultArtist?.takeIf { it.isNotBlank() }
            ?: extractArtistFromTitleOrMp3(cleaned, dto.mp3Url, fallbackQuery)).trim()
        val songTitle = cleaned
            .replace(artist, "", ignoreCase = true)
            .replace(Regex("^[\\-–—:|]+|[\\-–—:|]+$"), "")
            .trim()
            .ifBlank { cleaned }

        if (songTitle.isBlank() || artist.isBlank() || !SourceResolver.isValidStreamUrl(dto.mp3Url)) {
            return null
        }

        val year = when {
            dto.mp3Url.contains("/2026/") || (dto.artworkUrl?.contains("/2026/") == true) -> "2026"
            dto.mp3Url.contains("/2025/") || (dto.artworkUrl?.contains("/2025/") == true) || dto.mp3Url.contains("/1404/") -> "2025"
            dto.mp3Url.contains("/2024/") || (dto.artworkUrl?.contains("/2024/") == true) || dto.mp3Url.contains("/1403/") -> "2024"
            else -> "2023"
        }

        // Stable composite hash using both source ID ("musics_fa") and canonical MP3 URL (Fixes BUG-14)
        val stableId = 1_000_000L + (abs("musics_fa:${dto.mp3Url}".hashCode().toLong()) % 900_000_000L)

        return Track(
            id = stableId,
            title = songTitle,
            artist = artist,
            album = "تک‌آهنگ ($year)",
            artworkUrl = dto.artworkUrl ?: "",
            previewUrl = dto.mp3Url,
            genre = "پاپ",
            durationMs = 0L, // Fixes BUG-05: Unknown duration until Media3 prepares the stream
            releaseYear = year,
            countryCode = countryCode,
            isFavorite = favoriteIds.contains(stableId),
            playCount = 85000 + (abs(stableId.toInt()) % 35000),
            sourceId = "musics_fa"
        )
    }

    private fun extractArtistFromTitleOrMp3(cleanedTitle: String, mp3Url: String, fallbackQuery: String): String {
        for (country in CountriesData.allCountries) {
            for (knownArtist in country.topArtists) {
                if (cleanedTitle.contains(knownArtist, ignoreCase = true)) return knownArtist
            }
        }
        val fileName = mp3Url.substringAfterLast("/").replace("%20", " ")
        val artistPart = fileName.substringBefore(" - ").trim()
        return if (artistPart.isNotBlank() && artistPart.length < 35) artistPart else fallbackQuery.ifBlank { "خواننده" }
    }

    fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&apos;", "'")
            .replace("&#8211;", "–")
            .replace("&#8212;", "—")
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
