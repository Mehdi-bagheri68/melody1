package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.data.model.Artist
import com.example.data.model.ArtistType
import com.example.data.model.Track
import com.example.data.repository.ArtworkResolver

@Entity(
    tableName = "artists",
    indices = [Index(value = ["countryCode"])]
)
data class ArtistEntity(
    @PrimaryKey val id: String,
    val nameFa: String,
    val nameEn: String,
    val type: String = "SOLO",
    val countryCode: String = "IR",
    val imageUrl: String? = null
) {
    fun toArtist(): Artist = Artist(
        id = id,
        nameFa = nameFa,
        nameEn = nameEn,
        type = try {
            ArtistType.valueOf(type)
        } catch (_: Exception) {
            ArtistType.SOLO
        },
        countryCode = countryCode,
        imageUrl = imageUrl
    )
}

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["email"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey val uid: String,
    val email: String,
    val passwordHash: String,
    val passwordSalt: String,
    val displayName: String = "",
    val emailVerified: Boolean = false,
    val verificationCode: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "favorite_tracks",
    indices = [
        Index(value = ["artistId"]),
        Index(value = ["userId"])
    ]
)
data class FavoriteTrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val previewUrl: String,
    val genre: String,
    val durationMs: Long,
    val releaseYear: String,
    val countryCode: String,
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "") val artistId: String = Track.slugifyArtistId(artist),
    @ColumnInfo(defaultValue = "") val ownerArtistId: String = artistId,
    @ColumnInfo(defaultValue = "local") val userId: String = "local"
) {
    fun toTrack(): Track {
        val effectiveArtistId = artistId.ifBlank { Track.slugifyArtistId(artist) }
        val ownedArt = if (ownerArtistId.isBlank() || ownerArtistId == effectiveArtistId) {
            ArtworkResolver.buildOwnedArtworkOrNull(artworkUrl, effectiveArtistId)
        } else null
        return Track(
            id = id,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = ownedArt?.url ?: "",
            previewUrl = previewUrl,
            genre = genre,
            durationMs = durationMs,
            releaseYear = releaseYear,
            countryCode = countryCode,
            isFavorite = true,
            artistId = effectiveArtistId,
            artwork = ownedArt
        )
    }
}

fun Track.toFavoriteEntity(userId: String = "local"): FavoriteTrackEntity {
    val effectiveArtistId = artistId.ifBlank { Track.slugifyArtistId(artist) }
    val ownedArt = artwork ?: ArtworkResolver.buildOwnedArtworkOrNull(artworkUrl, effectiveArtistId)
    return FavoriteTrackEntity(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = ownedArt?.url ?: "",
        previewUrl = previewUrl,
        genre = genre,
        durationMs = durationMs,
        releaseYear = releaseYear,
        countryCode = countryCode,
        artistId = effectiveArtistId,
        ownerArtistId = ownedArt?.ownerArtistId ?: effectiveArtistId,
        userId = userId
    )
}

@Entity(
    tableName = "listening_history",
    indices = [
        Index(value = ["artistId"]),
        Index(value = ["userId"])
    ]
)
data class HistoryTrackEntity(
    @PrimaryKey(autoGenerate = true) val entryId: Long = 0,
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val previewUrl: String,
    val genre: String,
    val durationMs: Long,
    val releaseYear: String,
    val countryCode: String,
    val playedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "") val artistId: String = Track.slugifyArtistId(artist),
    @ColumnInfo(defaultValue = "") val ownerArtistId: String = artistId,
    @ColumnInfo(defaultValue = "local") val userId: String = "local"
) {
    fun toTrack(isFavorite: Boolean = false): Track {
        val effectiveArtistId = artistId.ifBlank { Track.slugifyArtistId(artist) }
        val ownedArt = if (ownerArtistId.isBlank() || ownerArtistId == effectiveArtistId) {
            ArtworkResolver.buildOwnedArtworkOrNull(artworkUrl, effectiveArtistId)
        } else null
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = ownedArt?.url ?: "",
            previewUrl = previewUrl,
            genre = genre,
            durationMs = durationMs,
            releaseYear = releaseYear,
            countryCode = countryCode,
            isFavorite = isFavorite,
            artistId = effectiveArtistId,
            artwork = ownedArt
        )
    }
}

fun Track.toHistoryEntity(userId: String = "local"): HistoryTrackEntity {
    val effectiveArtistId = artistId.ifBlank { Track.slugifyArtistId(artist) }
    val ownedArt = artwork ?: ArtworkResolver.buildOwnedArtworkOrNull(artworkUrl, effectiveArtistId)
    return HistoryTrackEntity(
        trackId = id,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = ownedArt?.url ?: "",
        previewUrl = previewUrl,
        genre = genre,
        durationMs = durationMs,
        releaseYear = releaseYear,
        countryCode = countryCode,
        playedAt = System.currentTimeMillis(),
        artistId = effectiveArtistId,
        ownerArtistId = ownedArt?.ownerArtistId ?: effectiveArtistId,
        userId = userId
    )
}

@Entity(
    tableName = "playlists",
    indices = [Index(value = ["userId"])]
)
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "local") val userId: String = "local"
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index(value = ["artistId"])]
)
data class PlaylistTrackCrossRefEntity(
    val playlistId: Long,
    val trackId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val previewUrl: String,
    val genre: String,
    val durationMs: Long,
    val releaseYear: String,
    val countryCode: String,
    val addedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "") val artistId: String = Track.slugifyArtistId(artist),
    @ColumnInfo(defaultValue = "") val ownerArtistId: String = artistId
) {
    fun toTrack(isFavorite: Boolean = false): Track {
        val effectiveArtistId = artistId.ifBlank { Track.slugifyArtistId(artist) }
        val ownedArt = if (ownerArtistId.isBlank() || ownerArtistId == effectiveArtistId) {
            ArtworkResolver.buildOwnedArtworkOrNull(artworkUrl, effectiveArtistId)
        } else null
        return Track(
            id = trackId,
            title = title,
            artist = artist,
            album = album,
            artworkUrl = ownedArt?.url ?: "",
            previewUrl = previewUrl,
            genre = genre,
            durationMs = durationMs,
            releaseYear = releaseYear,
            countryCode = countryCode,
            isFavorite = isFavorite,
            artistId = effectiveArtistId,
            artwork = ownedArt
        )
    }
}
