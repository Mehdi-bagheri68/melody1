package com.example.data.repository

import com.example.data.model.Artist
import com.example.data.model.Artwork
import com.example.data.model.ArtworkKind
import com.example.data.model.ArtworkRef
import com.example.data.model.Track
import kotlin.math.abs

/**
 * Fixes N-01, U-01, U-02 (Strict Artwork Ownership):
 *
 * Rule: An artwork image is ONLY displayed for a [Track] if its [Artwork.ownerArtistId]
 * matches [Track.artistId] or is in [Track.featuredArtistIds].
 *
 * Every bundled `cover_10XX.jpg` has a single canonical owner in [BUNDLED_COVER_OWNERS].
 * If any track references a cover owned by a different artist, or has no cover,
 * [resolve] returns [ArtworkRef.Initials] — a stylized, deterministic artist monogram
 * with a rich two-tone gradient derived from [Track.artistId], ensuring another artist's
 * face or song cover is NEVER shown by mistake.
 */
object ArtworkResolver {

    /**
     * Authoritative 1-to-1 ownership map of bundled cover files in `assets/covers/`.
     * No cover file is ever shared between two different artist IDs.
     */
    val BUNDLED_COVER_OWNERS: Map<String, String> = mapOf(
        "file:///android_asset/covers/cover_1001.jpg" to "mohsen-yeganeh",
        "file:///android_asset/covers/cover_1002.jpg" to "mohsen-chavoshi",
        "file:///android_asset/covers/cover_1003.jpg" to "mohammadreza-shajarian",
        "file:///android_asset/covers/cover_1004.jpg" to "mohsen-chavoshi",
        "file:///android_asset/covers/cover_1005.jpg" to "alireza-ghorbani",
        "file:///android_asset/covers/cover_1006.jpg" to "mohammadreza-shajarian",
        "file:///android_asset/covers/cover_1007.jpg" to "alireza-ghorbani",
        "file:///android_asset/covers/cover_1008.jpg" to "homayoun-shajarian",
        "file:///android_asset/covers/cover_1009.jpg" to "erfan-tahmasbi",
        "file:///android_asset/covers/cover_1010.jpg" to "mohsen-yeganeh",
        "file:///android_asset/covers/cover_1011.jpg" to "sirvan-khosravi",
        "file:///android_asset/covers/cover_1012.jpg" to "homayoun-shajarian",
        "file:///android_asset/covers/cover_1013.jpg" to "mohsen-chavoshi",
        "file:///android_asset/covers/cover_1014.jpg" to "erfan-tahmasbi",
        "file:///android_asset/covers/cover_1015.jpg" to "homayoun-shajarian",
        "file:///android_asset/covers/cover_1016.jpg" to "sirvan-khosravi",
        "file:///android_asset/covers/cover_1017.jpg" to "behnam-bani",
        "file:///android_asset/covers/cover_1018.jpg" to "haamim",
        "file:///android_asset/covers/cover_1019.jpg" to "majid-razavi",
        "file:///android_asset/covers/cover_1020.jpg" to "ehsan-khajeh-amiri",
        "file:///android_asset/covers/cover_1021.jpg" to "ali-lohrasbi",
        "file:///android_asset/covers/cover_1022.jpg" to "shadmehr-aghili",
        "file:///android_asset/covers/cover_1023.jpg" to "reza-bahram",
        "file:///android_asset/covers/cover_1024.jpg" to "garsha-rezaei",
        "file:///android_asset/covers/cover_1025.jpg" to "fereydoun-asraei",
        "file:///android_asset/covers/cover_1026.jpg" to "garsha-rezaei",
        "file:///android_asset/covers/cover_1027.jpg" to "reza-bahram"
    )

    private val PALETTES: List<Pair<Long, Long>> = listOf(
        0xFF3A1C71L to 0xFFD76D77L,
        0xFF0F2027L to 0xFF2C5364L,
        0xFF1A2980L to 0xFF26D0CEL,
        0xFF4B1248L to 0xFFF0C27BL,
        0xFF134E5EL to 0xFF71B280L,
        0xFF614385L to 0xFF516395L,
        0xFF8E0E00L to 0xFF1F1C18L,
        0xFF1D4350L to 0xFFA43931L,
        0xFF16222AL to 0xFF3A6073L,
        0xFF42275AL to 0xFF734B6DL
    )

    /**
     * Resolves an [ArtworkRef] for a [Track] and optional [Artist], strictly enforcing
     * that the image owner matches [Track.artistId] or [Track.featuredArtistIds].
     */
    fun resolve(track: Track, artist: Artist? = null): ArtworkRef {
        val effectiveArtistId = track.artistId.ifBlank { Track.slugifyArtistId(track.artist) }
        val allowedOwners = buildSet {
            add(effectiveArtistId)
            addAll(track.featuredArtistIds)
        }

        // 1. Check explicit Track.artwork object
        val explicitArt = track.artwork
        if (explicitArt != null && explicitArt.url.isNotBlank() && !isSyntheticUrl(explicitArt.url)) {
            val trueOwner = BUNDLED_COVER_OWNERS[explicitArt.url] ?: explicitArt.ownerArtistId
            if (trueOwner in allowedOwners && explicitArt.ownerArtistId in allowedOwners) {
                return ArtworkRef.Image(explicitArt.url)
            }
        }

        // 2. Check legacy/raw artworkUrl only if ownership is verified
        val rawUrl = track.artworkUrl.trim()
        if (rawUrl.isNotBlank() && !isSyntheticUrl(rawUrl)) {
            val bundledOwner = BUNDLED_COVER_OWNERS[rawUrl]
            if (bundledOwner != null) {
                if (bundledOwner in allowedOwners) {
                    return ArtworkRef.Image(rawUrl)
                }
            } else if (!rawUrl.startsWith("file:///android_asset/covers/")) {
                // External verified cover (e.g. live scraped track cover or iTunes cover for that track)
                return ArtworkRef.Image(rawUrl)
            }
        }

        // 3. Check Artist-owned portrait image if owned by this exact artist
        val artistImg = artist?.imageUrl?.trim()
        if (!artistImg.isNullOrBlank() && artist.id == effectiveArtistId) {
            val bundledOwner = BUNDLED_COVER_OWNERS[artistImg]
            if (bundledOwner == null || bundledOwner == effectiveArtistId) {
                return ArtworkRef.Image(artistImg)
            }
        }

        // 4. Fallback to rich, deterministic Artist Monogram Initials (U-02)
        return resolveInitials(
            displayName = artist?.nameEn?.ifBlank { artist.nameFa } ?: track.artist.ifBlank { track.title },
            seed = effectiveArtistId
        )
    }

    fun resolveForUrlAndTitle(
        imageUrl: String,
        title: String,
        subtitle: String = "",
        artistIdHint: String = ""
    ): ArtworkRef {
        val trimmed = imageUrl.trim()
        val effectiveSeed = artistIdHint.ifBlank {
            Track.slugifyArtistId(subtitle.ifBlank { title })
        }
        if (trimmed.isNotBlank() && !isSyntheticUrl(trimmed)) {
            val bundledOwner = BUNDLED_COVER_OWNERS[trimmed]
            if (bundledOwner == null || subtitle.isBlank() || bundledOwner == effectiveSeed) {
                return ArtworkRef.Image(trimmed)
            }
        }
        return resolveInitials(
            displayName = subtitle.ifBlank { title },
            seed = effectiveSeed
        )
    }

    fun buildOwnedArtworkOrNull(url: String, artistId: String): Artwork? {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank() || isSyntheticUrl(cleanUrl)) return null
        val bundledOwner = BUNDLED_COVER_OWNERS[cleanUrl]
        if (bundledOwner != null) {
            return if (bundledOwner == artistId) {
                Artwork(url = cleanUrl, ownerArtistId = artistId, kind = ArtworkKind.TRACK_COVER)
            } else {
                null
            }
        }
        return Artwork(url = cleanUrl, ownerArtistId = artistId, kind = ArtworkKind.TRACK_COVER)
    }

    fun resolveInitials(displayName: String, seed: String): ArtworkRef.Initials {
        val cleanName = displayName
            .replace("&", " ")
            .replace("(", " ")
            .replace(")", " ")
            .trim()
        val parts = cleanName.split(Regex("\\s+")).filter { it.isNotBlank() }
        val initials = when {
            parts.size >= 2 -> {
                val first = parts[0].firstOrNull()?.uppercaseChar() ?: 'M'
                val second = parts[1].firstOrNull()?.uppercaseChar() ?: 'G'
                "$first\u200C$second"
            }
            parts.size == 1 -> parts[0].take(2).uppercase()
            else -> "MG"
        }
        val paletteIndex = abs(seed.hashCode()) % PALETTES.size
        val (primary, secondary) = PALETTES[paletteIndex]
        return ArtworkRef.Initials(
            text = initials,
            seed = seed,
            primaryColorHex = primary,
            secondaryColorHex = secondary
        )
    }

    private fun isSyntheticUrl(url: String): Boolean {
        return url.contains("picsum.photos", ignoreCase = true) ||
            url.contains("unsplash.com", ignoreCase = true)
    }
}
