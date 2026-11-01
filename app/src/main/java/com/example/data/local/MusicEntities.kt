package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Track

@Entity(tableName = "favorite_tracks")
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
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        previewUrl = previewUrl,
        genre = genre,
        durationMs = durationMs,
        releaseYear = releaseYear,
        countryCode = countryCode,
        isFavorite = true
    )
}

fun Track.toFavoriteEntity(): FavoriteTrackEntity = FavoriteTrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    previewUrl = previewUrl,
    genre = genre,
    durationMs = durationMs,
    releaseYear = releaseYear,
    countryCode = countryCode
)

@Entity(tableName = "listening_history")
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
    val playedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(isFavorite: Boolean = false): Track = Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        previewUrl = previewUrl,
        genre = genre,
        durationMs = durationMs,
        releaseYear = releaseYear,
        countryCode = countryCode,
        isFavorite = isFavorite
    )
}

fun Track.toHistoryEntity(): HistoryTrackEntity = HistoryTrackEntity(
    trackId = id,
    title = title,
    artist = artist,
    album = album,
    artworkUrl = artworkUrl,
    previewUrl = previewUrl,
    genre = genre,
    durationMs = durationMs,
    releaseYear = releaseYear,
    countryCode = countryCode,
    playedAt = System.currentTimeMillis()
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"]
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
    val addedAt: Long = System.currentTimeMillis()
) {
    fun toTrack(isFavorite: Boolean = false): Track = Track(
        id = trackId,
        title = title,
        artist = artist,
        album = album,
        artworkUrl = artworkUrl,
        previewUrl = previewUrl,
        genre = genre,
        durationMs = durationMs,
        releaseYear = releaseYear,
        countryCode = countryCode,
        isFavorite = isFavorite
    )
}
