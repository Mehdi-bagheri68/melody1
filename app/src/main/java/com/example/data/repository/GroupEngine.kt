package com.example.data.repository

import com.example.data.model.Artist
import com.example.data.model.GroupKind
import com.example.data.model.GroupSection
import com.example.data.model.GroupSort
import com.example.data.model.Track
import com.example.data.model.TrackGroup
import java.util.Locale
import kotlin.math.abs

/**
 * Pure evaluation engine for [TrackGroup] (`CURATED` and `SMART`).
 * Remediated per Section 2.2, 2.3, and 2.4 of MELOGROOVE_PRODUCT_AUDIT_AND_REMEDIATION_PLAN:
 * - Release sorting (`RELEASE_YEAR_DESC`) uses `Track.computeReleaseSortKey()` and `datePrecision`
 *   so verified full-date (`YYYY-MM-DD`) releases rank ahead of year-only catalog items of the same year,
 *   without fabricating month/day and with deterministic tie-breaking by `id`.
 * - Provides conservative cross-provider deduplication (`deduplicateTracks`) comparing normalized
 *   artist + title + duration tolerance (`<= 3000ms`), preferring full streams over 30s previews.
 */
object GroupEngine {

    fun evaluate(
        group: TrackGroup,
        tracks: List<Track>,
        artists: Map<String, Artist> = emptyMap(),
        excludeTrackIds: Set<Long> = emptySet()
    ): List<Track> {
        if (!group.enabled) return emptyList()

        val deduplicatedPool = deduplicateTracks(tracks)
        val availableTracks = if (excludeTrackIds.isEmpty()) {
            deduplicatedPool
        } else {
            deduplicatedPool.filter { it.id !in excludeTrackIds }
        }

        return when (group.kind) {
            GroupKind.CURATED -> {
                val byId = availableTracks.associateBy { it.id }
                group.trackIds.mapNotNull { byId[it] }
            }
            GroupKind.SMART -> {
                val r = group.rule ?: return emptyList()
                val filtered = availableTracks.asSequence()
                    .filter { t ->
                        r.countries.isEmpty() || r.countries.any { it.equals(t.countryCode, ignoreCase = true) }
                    }
                    .filter { t ->
                        r.genres.isEmpty() || r.genres.any { g -> t.genre.contains(g, ignoreCase = true) }
                    }
                    .filter { t ->
                        r.minYear == null || (t.releaseYear.take(4).toIntOrNull() ?: 0) >= r.minYear
                    }
                    .toList()

                val sorted = when (r.sort) {
                    GroupSort.PLAY_COUNT_DESC -> filtered.sortedWith(
                        compareByDescending<Track> { it.popularitySignal?.isVerifiedLiveSignal == true }
                            .thenBy { it.popularitySignal?.rank ?: Int.MAX_VALUE }
                            .thenByDescending { it.playCount }
                            .thenBy { it.id }
                    )
                    GroupSort.RELEASE_YEAR_DESC -> filtered.sortedWith(
                        compareByDescending<Track> { it.computeReleaseSortKey() }
                            .thenByDescending { it.datePrecision.weight }
                            .thenBy { it.id }
                    )
                    GroupSort.ADDED_DESC -> filtered.sortedByDescending { it.id }
                }

                val capped = if (r.perArtistCap != null && r.perArtistCap > 0) {
                    val counts = mutableMapOf<String, Int>()
                    sorted.filter { t ->
                        val artistKey = t.artistId.ifBlank { Track.slugifyArtistId(t.artist) }
                        val c = counts.getOrDefault(artistKey, 0)
                        if (c < r.perArtistCap) {
                            counts[artistKey] = c + 1
                            true
                        } else {
                            false
                        }
                    }
                } else {
                    sorted
                }

                capped.take(r.limit)
            }
        }
    }

    /**
     * Conservative track deduplication across providers (Section 2.2 & 2.4).
     * Two tracks are considered duplicates only when:
     * 1. Their normalized artist slugs match, AND
     * 2. Their normalized titles (stripping punctuation/whitespace) match, AND
     * 3. Neither title indicates a distinct live/remix/acoustic version unless both do, AND
     * 4. Their durations are within [durationToleranceMs] (or at least one has unknown duration `0L`).
     * When a duplicate pair is found, prefers full-length stream (`!isPreviewOnly`) or higher date precision.
     */
    fun deduplicateTracks(
        tracks: List<Track>,
        durationToleranceMs: Long = 3_000L
    ): List<Track> {
        if (tracks.size <= 1) return tracks
        val accepted = mutableListOf<Track>()

        for (candidate in tracks) {
            val matchIndex = accepted.indexOfFirst { existing ->
                areDuplicateTracks(existing, candidate, durationToleranceMs)
            }
            if (matchIndex < 0) {
                accepted.add(candidate)
            } else {
                val existing = accepted[matchIndex]
                val preferred = selectPreferredDuplicate(existing, candidate)
                accepted[matchIndex] = preferred
            }
        }
        return accepted
    }

    internal fun areDuplicateTracks(
        a: Track,
        b: Track,
        durationToleranceMs: Long = 3_000L
    ): Boolean {
        if (a.id == b.id) return true
        if (a.previewUrl.isNotBlank() && a.previewUrl.equals(b.previewUrl, ignoreCase = true)) return true

        val artistA = a.artistId.ifBlank { Track.slugifyArtistId(a.artist) }
        val artistB = b.artistId.ifBlank { Track.slugifyArtistId(b.artist) }
        if (!artistA.equals(artistB, ignoreCase = true)) return false

        val normTitleA = normalizeTitleForDedup(a.title)
        val normTitleB = normalizeTitleForDedup(b.title)
        if (normTitleA.isBlank() || normTitleA != normTitleB) return false

        if (a.durationMs > 0L && b.durationMs > 0L && !a.isPreviewOnly && !b.isPreviewOnly) {
            if (abs(a.durationMs - b.durationMs) > durationToleranceMs) return false
        }
        return true
    }

    private fun selectPreferredDuplicate(existing: Track, candidate: Track): Track {
        return when {
            !existing.isPreviewOnly && candidate.isPreviewOnly ->
                if (candidate.datePrecision.weight > existing.datePrecision.weight) {
                    existing.copy(
                        releaseDateIso = candidate.releaseDateIso,
                        datePrecision = candidate.datePrecision
                    )
                } else {
                    existing
                }
            existing.isPreviewOnly && !candidate.isPreviewOnly ->
                if (existing.datePrecision.weight > candidate.datePrecision.weight) {
                    candidate.copy(
                        releaseDateIso = existing.releaseDateIso,
                        datePrecision = existing.datePrecision
                    )
                } else {
                    candidate
                }
            candidate.datePrecision.weight > existing.datePrecision.weight -> candidate
            else -> existing
        }
    }

    internal fun normalizeTitleForDedup(rawTitle: String): String {
        return rawTitle.trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[\\s_\\-–—]+"), " ")
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF ()]"), "")
            .trim()
    }

    /**
     * Evaluates multiple groups in order, optionally deduplicating tracks across adjacent shelves.
     */
    fun evaluateAllSections(
        groups: List<TrackGroup>,
        tracks: List<Track>,
        artists: Map<String, Artist> = emptyMap(),
        deduplicateAcrossGroups: Boolean = false
    ): List<GroupSection> {
        val seenTrackIds = mutableSetOf<Long>()
        return groups
            .filter { it.enabled }
            .sortedBy { it.order }
            .mapNotNull { group ->
                val resolved = evaluate(
                    group = group,
                    tracks = tracks,
                    artists = artists,
                    excludeTrackIds = if (deduplicateAcrossGroups) seenTrackIds else emptySet()
                )
                if (resolved.isEmpty()) {
                    null
                } else {
                    if (deduplicateAcrossGroups) {
                        seenTrackIds.addAll(resolved.map { it.id })
                    }
                    GroupSection(group = group, tracks = resolved)
                }
            }
    }
}
