package com.miaudioplay.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.miaudioplay.data.LrcParser
import com.miaudioplay.data.LyricsRepository
import com.miaudioplay.data.MusicRepository
import com.miaudioplay.data.models.LyricLine
import com.miaudioplay.data.models.LyricsSource
import com.miaudioplay.data.models.Playlist
import com.miaudioplay.data.models.Song
import com.miaudioplay.service.MusicService
import com.miaudioplay.utils.FuzzyMatcher
import com.miaudioplay.utils.LyricsApiTester
import com.miaudioplay.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = MusicRepository(application)
    private val lyricsRepository = LyricsRepository(application)
    
    // Songs state
    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _filteredSongs = MutableStateFlow<List<Song>>(emptyList())
    val filteredSongs: StateFlow<List<Song>> = _filteredSongs.asStateFlow()
    
    // Current playback state
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()
    
    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()
    
    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    
    // Lyrics
    private val _lyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val lyrics: StateFlow<List<LyricLine>> = _lyrics.asStateFlow()
    
    private val _currentLyricIndex = MutableStateFlow(-1)
    val currentLyricIndex: StateFlow<Int> = _currentLyricIndex.asStateFlow()
    
    private val _lyricsLoading = MutableStateFlow(false)
    val lyricsLoading: StateFlow<Boolean> = _lyricsLoading.asStateFlow()
    
    private val _lyricsSource = MutableStateFlow<LyricsSource?>(null)
    val lyricsSource: StateFlow<LyricsSource?> = _lyricsSource.asStateFlow()
    
    // Playlists
    val playlists = repository.getAllPlaylists()
    
    private val _playlistSongs = MutableStateFlow<List<Song>>(emptyList())
    val playlistSongs: StateFlow<List<Song>> = _playlistSongs.asStateFlow()
    
    // Queue
    val queue = mutableStateListOf<Song>()
    
    // MediaController
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    
    init {
        initializeController()
        observeSearchQuery()
    }
    
    private fun initializeController() {
        val sessionToken = SessionToken(
            getApplication(),
            android.content.ComponentName(getApplication(), MusicService::class.java)
        )
        
        controllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            setupPlayerListener()
            startPositionUpdates()
        }, MoreExecutors.directExecutor())
    }
    
    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }
            
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItem?.let { item ->
                    val songId = item.mediaId.toLongOrNull() ?: return
                    _currentSong.value = _songs.value.find { it.id == songId }
                    loadLyricsForCurrentSong()
                }
            }
            
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    _duration.value = mediaController?.duration ?: 0L
                }
            }
            
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleMode.value = shuffleModeEnabled
            }
            
            override fun onRepeatModeChanged(repeatMode: Int) {
                _repeatMode.value = repeatMode
            }
        })
    }
    
    private fun startPositionUpdates() {
        viewModelScope.launch {
            while (true) {
                mediaController?.let { controller ->
                    _currentPosition.value = controller.currentPosition
                    updateCurrentLyricIndex()
                }
                delay(100)
            }
        }
    }
    
    private fun updateCurrentLyricIndex() {
        val currentPos = _currentPosition.value
        val lyricsList = _lyrics.value
        
        if (lyricsList.isEmpty()) {
            _currentLyricIndex.value = -1
            return
        }
        
        var index = lyricsList.indexOfLast { it.timestamp <= currentPos }
        if (index < 0) index = 0
        _currentLyricIndex.value = index
    }
    
    private fun loadLyricsForCurrentSong() {
        viewModelScope.launch {
            val song = _currentSong.value ?: return@launch
            
            try {
                _lyricsLoading.value = true
                _lyrics.value = emptyList()
                _lyricsSource.value = null
                
                // 尝试获取歌词（本地 -> 缓存 -> 在线）
                val result = lyricsRepository.getLyrics(
                    audioPath = song.path,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    duration = song.duration
                )
                
                if (result != null) {
                    // 解析歌词
                    _lyrics.value = LrcParser.parse(result.content)
                    _lyricsSource.value = result.source
                    Log.d("MusicViewModel", "Lyrics loaded from: ${result.source}")
                } else {
                    _lyrics.value = emptyList()
                    _lyricsSource.value = null
                    Log.d("MusicViewModel", "No lyrics found")
                }
            } catch (e: Exception) {
                Log.e("MusicViewModel", "Error loading lyrics", e)
                _lyrics.value = emptyList()
                _lyricsSource.value = null
            } finally {
                _lyricsLoading.value = false
            }
        }
    }
    
    fun loadSongs() {
        viewModelScope.launch {
            _isLoading.value = true
            _songs.value = repository.loadSongs()
            _filteredSongs.value = _songs.value
            _isLoading.value = false
        }
    }
    
    /**
     * Updates the search query and filters songs accordingly
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    /**
     * Observes search query changes and filters songs with debouncing
     */
    private fun observeSearchQuery() {
        viewModelScope.launch {
            searchQuery.collect { query ->
                // Debounce search for better performance
                delay(300)
                filterSongs(query)
            }
        }
    }
    
    /**
     * Filters songs based on search query using fuzzy matching
     */
    private fun filterSongs(query: String) {
        _filteredSongs.value = if (query.isBlank()) {
            _songs.value
        } else {
            FuzzyMatcher.filterAndSortByRelevance(_songs.value, query)
        }
    }
    
    fun playSong(song: Song, songList: List<Song> = _songs.value) {
        queue.clear()
        queue.addAll(songList)
        
        val mediaItems = songList.map { s ->
            MediaItem.Builder()
                .setMediaId(s.id.toString())
                .setUri(s.uri)
                .setRequestMetadata(
                    MediaItem.RequestMetadata.Builder()
                        .setMediaUri(s.uri)
                        .build()
                )
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(s.title)
                        .setArtist(s.artist)
                        .setAlbumTitle(s.album)
                        .setArtworkUri(s.albumArtUri)
                        .build()
                )
                .build()
        }
        
        val startIndex = songList.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
        
        _currentSong.value = song
        loadLyricsForCurrentSong()
    }
    
    fun togglePlayPause() {
        mediaController?.let { controller ->
            if (controller.isPlaying) {
                controller.pause()
            } else {
                controller.play()
            }
        }
    }
    
    fun seekTo(position: Long) {
        mediaController?.seekTo(position)
    }
    
    fun seekToNext() {
        mediaController?.seekToNextMediaItem()
    }
    
    fun seekToPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }
    
    fun toggleShuffle() {
        mediaController?.let { controller ->
            controller.shuffleModeEnabled = !controller.shuffleModeEnabled
        }
    }
    
    fun toggleRepeatMode() {
        mediaController?.let { controller ->
            controller.repeatMode = when (controller.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }
    
    // Playlist operations
    fun createPlaylist(name: String) {
        viewModelScope.launch {
            repository.createPlaylist(name)
        }
    }
    
    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
        }
    }
    
    fun addSongToPlaylist(playlistId: Long, song: Song) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, song)
        }
    }
    
    fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    suspend fun getPlaylistSongUris(playlistId: Long): List<String> {
        return repository.getSongUrisForPlaylist(playlistId)
    }
    
    fun loadPlaylistSongs(playlistId: Long) {
        viewModelScope.launch {
            repository.getSongsForPlaylist(playlistId).collect { playlistSongsList ->
                val songUris = playlistSongsList.map { it.songUri }
                val songsInPlaylist = _songs.value.filter { song ->
                    songUris.contains(song.uri.toString())
                }
                // Order according to playlistSongsList order if necessary. 
                // Currently just filtering.
                _playlistSongs.value = songsInPlaylist
            }
        }
    }
    
    fun playPlaylist(playlistId: Long) {
        viewModelScope.launch {
            val songUris = repository.getSongUrisForPlaylist(playlistId)
            val songsToPlay = _songs.value.filter { song ->
                songUris.contains(song.uri.toString())
            }
            if (songsToPlay.isNotEmpty()) {
                playSong(songsToPlay.first(), songsToPlay)
            }
        }
    }
    
    /**
     * 测试所有歌词API（用于调试）
     */
    fun testLyricsApis() {
        viewModelScope.launch {
            Log.d("MiAudioPlay MusicViewModel", "Starting lyrics API test...")
            
            // Check network
            val networkStatus = NetworkUtils.getNetworkStatusDescription(getApplication())
            Log.d("MiAudioPlay MusicViewModel", "Network: $networkStatus")
            
            // Test APIs
            val results = LyricsApiTester.testAllApis()
            
            Log.d("MiAudioPlay MusicViewModel", "Test completed. Results:")
            results.forEach { (api, result) ->
                Log.d("MiAudioPlay MusicViewModel", "$api: ${result.message}")
            }
        }
    }
    
    override fun onCleared() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        super.onCleared()
    }
}
