package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Favorites
    @Query("SELECT * FROM favorite_tracks ORDER BY addedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteTrackEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_tracks WHERE id = :trackId)")
    fun isFavorite(trackId: Long): Flow<Boolean>

    @Query("SELECT id FROM favorite_tracks")
    fun getAllFavoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(track: FavoriteTrackEntity)

    @Query("DELETE FROM favorite_tracks WHERE id = :trackId")
    suspend fun deleteFavorite(trackId: Long)

    // History
    @Query("SELECT * FROM listening_history ORDER BY playedAt DESC LIMIT 50")
    fun getListeningHistory(): Flow<List<HistoryTrackEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(track: HistoryTrackEntity)

    @Query("DELETE FROM listening_history")
    suspend fun clearHistory()

    // Playlists
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun deletePlaylistTracks(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrackToPlaylist(track: PlaylistTrackCrossRefEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: Long, trackId: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getTracksForPlaylist(playlistId: Long): Flow<List<PlaylistTrackCrossRefEntity>>

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun getTrackCountForPlaylist(playlistId: Long): Int

    @Transaction
    suspend fun deletePlaylistAndTracks(playlistId: Long) {
        deletePlaylistTracks(playlistId)
        deletePlaylist(playlistId)
    }
}
