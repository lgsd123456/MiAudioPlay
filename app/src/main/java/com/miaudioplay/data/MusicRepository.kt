package com.miaudioplay.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.miaudioplay.data.models.Playlist
import com.miaudioplay.data.models.PlaylistSong
import com.miaudioplay.data.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class MusicRepository(context: Context) {
    private val contentResolver: ContentResolver = context.contentResolver
    private val database = MusicDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()
    private val playlistSongDao = database.playlistSongDao()
    
    suspend fun loadSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"
        
        contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn) ?: "Unknown Album"
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val path = cursor.getString(dataColumn) ?: ""
                
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                
                val albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
                
                songs.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        duration = duration,
                        uri = contentUri,
                        albumArtUri = albumArtUri,
                        path = path
                    )
                )
            }
        }
        
        songs
    }
    
    // Playlist operations
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    
    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(Playlist(name = name))
    }
    
    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }
    
    suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist)
    }
    
    // Playlist songs operations
    fun getSongsForPlaylist(playlistId: Long): Flow<List<PlaylistSong>> {
        return playlistSongDao.getSongsForPlaylist(playlistId)
    }
    
    suspend fun getSongUrisForPlaylist(playlistId: Long): List<String> {
        return playlistSongDao.getSongUrisForPlaylist(playlistId)
    }

    fun getSongsForPlaylistDetailed(playlistId: Long): Flow<List<PlaylistSong>> {
        return playlistSongDao.getSongsForPlaylist(playlistId)
    }
    
    suspend fun addSongToPlaylist(playlistId: Long, song: Song) {
        if (!playlistSongDao.isSongInPlaylist(playlistId, song.id)) {
            playlistSongDao.insertPlaylistSong(
                PlaylistSong(
                    playlistId = playlistId,
                    songId = song.id,
                    songUri = song.uri.toString()
                )
            )
        }
    }
    
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistSongDao.removeSongFromPlaylist(playlistId, songId)
    }
    
    suspend fun getPlaylistSongCount(playlistId: Long): Int {
        return playlistSongDao.getSongCount(playlistId)
    }
}
