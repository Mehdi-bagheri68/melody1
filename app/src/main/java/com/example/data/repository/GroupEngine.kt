package com.example.data.repository

import com.example.data.model.Artist
import com.example.data.model.GroupKind
import com.example.data.model.GroupSection
import com.example.data.model.GroupSort
import com.example.data.model.Track
import com.example.data.model.TrackGroup

/**
 * Fixes N-03 & U-03:
 * Pure evaluation engine for [TrackGroup] (`CURATED` and `SMART`).
 * Supports country/genre/year filtering, sorting, `perArtistCap` (preventing consecutive tracks
 * by the same artist from flooding New Releases), and cross-shelf deduplication.
 */
object GroupEngine {

    fun evaluate(
        group: TrackGroup,
        tracks: List<Track>,
        artists: Map<String, Artist> = emptyMap(),
        excludeTrackIds: Set<Long> = emptySet()
    ): List<Track> {
        if (!group.enabled) return emptyList()

        val availableTracks = if (excludeTrackIds.isEmpty()) {
            tracks
        } else {
            tracks.filter { it.id !in excludeTrackIds }
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
                        r.minYear == null || (t.releaseYear.toIntOrNull() ?: 0) >= r.minYear
                    }
                    .toList()

                val sorted = when (r.sort) {
                    GroupSort.PLAY_COUNT_DESC -> filtered.sortedByDescending { it.playCount }
                    GroupSort.RELEASE_YEAR_DESC -> filtered.sortedWith(
                        compareByDescending<Track> { it.releaseYear.toIntOrNull() ?: 0 }
                            .thenByDescending { it.playCount }
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
     * Evaluates multiple groups in order, optionally deduplicating tracks across adjacent shelves (U-03).
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
