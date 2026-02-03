package com.miaudioplay.data

import androidx.room.*
import com.miaudioplay.data.models.Playlist
import com.miaudioplay.data.models.PlaylistSong
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<Playlist>>
    
    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun getPlaylistById(id: Long): Playlist?
    
    @Insert
    suspend fun insertPlaylist(playlist: Playlist): Long
    
    @Update
    suspend fun updatePlaylist(playlist: Playlist)
    
    @Delete
    suspend fun deletePlaylist(playlist: Playlist)
    
    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: Long)
}

@Dao
interface PlaylistSongDao {
    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt")
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>>
    
    @Query("SELECT songUri FROM playlist_songs WHERE playlistId = :playlistId ORDER BY addedAt")
    suspend fun getSongUrisForPlaylist(playlistId: Long): List<String>
    
    @Insert
    suspend fun insertPlaylistSong(playlistSong: PlaylistSong)
    
    @Delete
    suspend fun deletePlaylistSong(playlistSong: PlaylistSong)
    
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    
    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int
}
