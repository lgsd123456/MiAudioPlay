package com.miaudioplay

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miaudioplay.ui.components.MiniPlayer
import com.miaudioplay.ui.screens.NowPlayingScreen
import com.miaudioplay.ui.screens.PlaylistsScreen
import com.miaudioplay.ui.screens.SongsScreen
import com.miaudioplay.ui.theme.MiAudioPlayTheme
import com.miaudioplay.data.models.Playlist
import com.miaudioplay.ui.screens.PlaylistDetailScreen
import com.miaudioplay.viewmodel.MusicViewModel

class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            // Permissions granted, reload songs
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MiAudioPlayTheme {
                val viewModel: MusicViewModel = viewModel()
                
                // Check and request permissions
                LaunchedEffect(Unit) {
                    android.util.Log.d("MiAudioPlay MainActivity", "LaunchedEffect started")
                    
                    if (hasRequiredPermissions()) {
                        android.util.Log.d("MiAudioPlay MainActivity", "Permissions granted, loading songs")
                        viewModel.loadSongs()
                    } else {
                        android.util.Log.d("MiAudioPlay MainActivity", "Requesting permissions")
                        requestPermissions()
                    }
                }
                
                // 延迟测试API（确保在权限授予后执行）
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(3000) // 等待3秒
                    android.util.Log.d("MiAudioPlay MainActivity", "Starting API test after delay")
                    viewModel.testLyricsApis()
                }
                
                MainScreen(viewModel = viewModel)
            }
        }
    }
    
    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        requestPermissionLauncher.launch(permissions)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MusicViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var showNowPlaying by remember { mutableStateOf(false) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    
    val songs by viewModel.songs.collectAsState()
    val filteredSongs by viewModel.filteredSongs.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleMode by viewModel.shuffleMode.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyricIndex by viewModel.currentLyricIndex.collectAsState()
    val lyricsLoading by viewModel.lyricsLoading.collectAsState()
    val lyricsSource by viewModel.lyricsSource.collectAsState()
    val playlists by viewModel.playlists.collectAsState(initial = emptyList())
    val playlistSongs by viewModel.playlistSongs.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Main content with bottom navigation
        Scaffold(
            bottomBar = {
                Column {
                    // Mini player
                    AnimatedVisibility(
                        visible = currentSong != null && !showNowPlaying,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it })
                    ) {
                        MiniPlayer(
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onNextClick = { viewModel.seekToNext() },
                            onClick = { showNowPlaying = true }
                        )
                    }
                    
                    // Bottom navigation
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                            label = { Text("音乐") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.QueueMusic, contentDescription = null) },
                            label = { Text("歌单") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedTab) {
                    0 -> SongsScreen(
                        songs = filteredSongs,
                        currentSong = currentSong,
                        isLoading = isLoading,
                        playlists = playlists,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { query -> viewModel.updateSearchQuery(query) },
                        onSongClick = { song -> viewModel.playSong(song, filteredSongs) },
                        onAddToPlaylist = { song, playlist ->
                            viewModel.addSongToPlaylist(playlist.id, song)
                        }
                    )
                    1 -> PlaylistsScreen(
                        playlists = playlists,
                        onPlaylistClick = { playlist -> 
                            selectedPlaylist = playlist
                            viewModel.loadPlaylistSongs(playlist.id)
                        },
                        onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                        onDeletePlaylist = { id -> viewModel.deletePlaylist(id) },
                        onPlayPlaylist = { id -> viewModel.playPlaylist(id) }
                    )
                }
            }
        }
        
        // Playlist details
        val currentPlaylist = selectedPlaylist
        AnimatedVisibility(
            visible = currentPlaylist != null,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit = slideOutHorizontally(targetOffsetX = { it })
        ) {
            if (currentPlaylist != null) {
                PlaylistDetailScreen(
                    playlist = currentPlaylist,
                    songs = playlistSongs,
                    currentSong = currentSong,
                    onBackClick = { selectedPlaylist = null },
                    onSongClick = { song -> viewModel.playSong(song, playlistSongs) },
                    onPlayAllClick = { viewModel.playPlaylist(currentPlaylist.id) },
                    onRemoveFromPlaylist = { songId: Long -> viewModel.removeSongFromPlaylist(currentPlaylist.id, songId) }
                )
            }
        }
        
        // Now playing full screen
        AnimatedVisibility(
            visible = showNowPlaying,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            NowPlayingScreen(
                currentSong = currentSong,
                isPlaying = isPlaying,
                shuffleMode = shuffleMode,
                repeatMode = repeatMode,
                currentPosition = currentPosition,
                duration = duration,
                lyrics = lyrics,
                currentLyricIndex = currentLyricIndex,
                lyricsLoading = lyricsLoading,
                lyricsSource = lyricsSource,
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onPreviousClick = { viewModel.seekToPrevious() },
                onNextClick = { viewModel.seekToNext() },
                onShuffleClick = { viewModel.toggleShuffle() },
                onRepeatClick = { viewModel.toggleRepeatMode() },
                onSeek = { position -> viewModel.seekTo(position) },
                onBackClick = { showNowPlaying = false }
            )
        }
    }
}
