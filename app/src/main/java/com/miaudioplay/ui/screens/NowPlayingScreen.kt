package com.miaudioplay.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.miaudioplay.data.models.LyricLine
import com.miaudioplay.data.models.LyricsSource
import com.miaudioplay.data.models.Song
import com.miaudioplay.ui.components.PlaybackControls
import com.miaudioplay.ui.theme.Primary
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    currentSong: Song?,
    isPlaying: Boolean,
    shuffleMode: Boolean,
    repeatMode: Int,
    currentPosition: Long,
    duration: Long,
    lyrics: List<LyricLine>,
    currentLyricIndex: Int,
    lyricsLoading: Boolean,
    lyricsSource: LyricsSource?,
    onPlayPauseClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll lyrics
    LaunchedEffect(currentLyricIndex) {
        if (currentLyricIndex >= 0 && lyrics.isNotEmpty()) {
            coroutineScope.launch {
                lazyListState.animateScrollToItem(
                    index = currentLyricIndex.coerceIn(0, lyrics.lastIndex),
                    scrollOffset = -200
                )
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Primary.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                Text(
                    text = "正在播放",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Placeholder for symmetry
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            if (currentSong != null) {
                // Album art
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 48.dp)
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (currentSong.albumArtUri != null) {
                        AsyncImage(
                            model = currentSong.albumArtUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Song info
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentSong.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentSong.artist,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // Lyrics area
                if (lyricsLoading) {
                    // Loading state
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在搜索歌词...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (lyrics.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 48.dp)
                        ) {
                            itemsIndexed(lyrics) { index, lyric ->
                                val isCurrentLine = index == currentLyricIndex
                                val textColor by animateColorAsState(
                                    targetValue = if (isCurrentLine) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    },
                                    label = "lyric_color"
                                )
                                
                                Text(
                                    text = lyric.text,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontSize = if (isCurrentLine) 18.sp else 16.sp,
                                    fontWeight = if (isCurrentLine) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )
                            }
                            
                            // Source badge at bottom
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                lyricsSource?.let { source ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(
                                            text = when(source) {
                                                LyricsSource.LOCAL_FILE -> "本地本地"
                                                LyricsSource.CACHE -> "缓存"
                                                LyricsSource.LRCLIB -> "LRCLIB"
                                                LyricsSource.NETEASE -> "网易云音乐"
                                                LyricsSource.QQMUSIC -> "QQ音乐"
                                                LyricsSource.LYRICS_OVH -> "Lyrics.ovh"
                                                LyricsSource.CHARTLYRICS -> "ChartLyrics"
                                                LyricsSource.HAPPI -> "Happi.dev"
                                                LyricsSource.SIMPLE_LYRICS -> "网页爬虫"
                                                LyricsSource.CANARADO -> "Canarado"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无歌词",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
                
                // Playback controls
                PlaybackControls(
                    isPlaying = isPlaying,
                    shuffleMode = shuffleMode,
                    repeatMode = repeatMode,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPauseClick = onPlayPauseClick,
                    onPreviousClick = onPreviousClick,
                    onNextClick = onNextClick,
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onSeek = onSeek,
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } else {
                // No song playing
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有正在播放的音乐",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
