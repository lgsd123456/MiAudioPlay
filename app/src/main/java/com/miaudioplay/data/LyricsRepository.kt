package com.miaudioplay.data

import android.content.Context
import android.util.Log
import com.miaudioplay.data.api.CanaradoApi
import com.miaudioplay.data.api.ChartLyricsApi
import com.miaudioplay.data.api.HappiApi
import com.miaudioplay.data.api.LrcLibApi
import com.miaudioplay.data.api.LyricsOvhApi
import com.miaudioplay.data.api.NeteaseApi
import com.miaudioplay.data.api.QQMusicApi
import com.miaudioplay.data.api.SimpleLyricsApi
import com.miaudioplay.data.models.LyricsSearchResult
import com.miaudioplay.utils.NetworkUtils
import com.miaudioplay.data.models.LyricsSource
import com.miaudioplay.utils.LrcFileWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 歌词获取仓库
 * 协调本地文件、缓存和在线搜索
 */
class LyricsRepository(private val context: Context) {
    private val TAG = "LyricsRepository"
    
    /**
     * 获取歌词（尝试所有来源）
     * 
     * 优先级：
     * 1. 本地LRC文件
     * 2. 缓存
     * 3. 在线搜索（LRCLIB -> QQ音乐 -> NetEase -> Lyrics.ovh -> ChartLyrics -> Happi -> SimpleLyrics -> Canarado）
     * 
     * 在线歌词来源优先级：
     * 1. LRCLIB (免费，开源，无限制，支持同步歌词)
     * 2. QQ音乐 (中文歌曲丰富，优先级高)
     * 3. NetEase (网易云，中文歌曲丰富)
     * 4. Lyrics.ovh (免费，国际歌曲)
     * 5. ChartLyrics (稳定的SOAP API)
     * 6. Happi.dev (综合音乐API)
     * 7. SimpleLyrics (多网站爬虫聚合)
     * 8. Canarado (Lyrist聚合API)
     */
    suspend fun getLyrics(
        audioPath: String,
        title: String,
        artist: String,
        album: String = "",
        duration: Long = 0
    ): LyricsSearchResult? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Getting lyrics for: $artist - $title")
            Log.d(TAG, "Album: $album, Duration: ${duration}ms")
            
            // 1. 尝试本地LRC文件
            val localLrc = LrcParser.findLrcFile(audioPath)
            if (localLrc != null && localLrc.exists()) {
                Log.d(TAG, "✓ Found local LRC file: ${localLrc.absolutePath}")
                val content = localLrc.readText(Charsets.UTF_8)
                return@withContext LyricsSearchResult(
                    content = content,
                    source = LyricsSource.LOCAL_FILE,
                    cached = false
                )
            }
            
            // 2. 尝试缓存
            val cachedLyrics = LyricsCache.loadLyrics(context, artist, title)
            if (cachedLyrics != null) {
                Log.d(TAG, "✓ Found cached lyrics (${cachedLyrics.length} chars)")
                return@withContext LyricsSearchResult(
                    content = cachedLyrics,
                    source = LyricsSource.CACHE,
                    cached = true
                )
            }
            
            // 3. 检查网络连接
            val networkStatus = NetworkUtils.getNetworkStatusDescription(context)
            Log.d(TAG, "Network status: $networkStatus")
            
            if (!NetworkUtils.isNetworkAvailable(context)) {
                Log.w(TAG, "✗ No network connection available")
                return@withContext null
            }
            
            // 4. 在线搜索
            Log.d(TAG, "Searching online APIs...")
            val onlineResult = searchOnline(title, artist, album, duration)
            if (onlineResult != null) {
                // 缓存下载的歌词到应用私有目录
                LyricsCache.saveLyrics(context, artist, title, onlineResult.lyrics)
                
                // 尝试保存为LRC文件到音乐文件同目录
                val lrcSaved = LrcFileWriter.saveLrcFile(audioPath, onlineResult.lyrics)
                if (lrcSaved) {
                    Log.d(TAG, "✓ LRC file created in music directory")
                } else {
                    Log.d(TAG, "✗ Could not create LRC file (permission or directory issue)")
                }
                
                return@withContext LyricsSearchResult(
                    content = onlineResult.lyrics,
                    source = onlineResult.source,
                    cached = false
                )
            }
            
            Log.d(TAG, "No lyrics found")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting lyrics", e)
            null
        }
    }
    
    /**
     * 在线搜索歌词（尝试多个API，按优先级）
     */
    private suspend fun searchOnline(
        title: String,
        artist: String,
        album: String,
        duration: Long
    ): OnlineSearchResult? {
        var lyrics: String?
        
        // 1. 优先尝试 LRCLIB（免费、稳定、无需密钥、支持同步歌词）
        try {
            Log.d(TAG, "Trying LRCLIB API...")
            val durationSeconds = if (duration > 0) (duration / 1000).toInt() else null
            lyrics = LrcLibApi.searchLyrics(
                trackName = title,
                artistName = artist,
                albumName = album.ifBlank { null },
                duration = durationSeconds
            )
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Found lyrics from LRCLIB")
                return OnlineSearchResult(lyrics, LyricsSource.LRCLIB)
            }
        } catch (e: Exception) {
            Log.e(TAG, "LRCLIB search error", e)
        }
        
        // 2. 尝试 QQ音乐 (中文歌曲优先)
        try {
            Log.d(TAG, "Trying QQ Music API...")
            lyrics = QQMusicApi.searchAndGetLyrics(title, artist)
            if (lyrics != null) {
                Log.d(TAG, "✓ Found lyrics from QQ Music")
                return OnlineSearchResult(lyrics, LyricsSource.QQMUSIC)
            }
        } catch (e: Exception) {
            Log.e(TAG, "QQ Music search error", e)
        }
        
        // 3. 尝试网易云音乐（中文歌词丰富）
        try {
            Log.d(TAG, "Trying NetEase API...")
            lyrics = NeteaseApi.searchAndGetLyrics(title, artist)
            if (lyrics != null) {
                Log.d(TAG, "✓ Found lyrics from NetEase")
                return OnlineSearchResult(lyrics, LyricsSource.NETEASE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "NetEase search error", e)
        }
        
        // 4. 尝试 Lyrics.ovh（国际歌词，简单API）
        try {
            Log.d(TAG, "Trying Lyrics.ovh API...")
            lyrics = LyricsOvhApi.getLyrics(artist, title)
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Found lyrics from Lyrics.ovh")
                return OnlineSearchResult(lyrics, LyricsSource.LYRICS_OVH)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics.ovh search error", e)
        }
        
        // 5. 尝试 ChartLyrics（稳定的SOAP API）
        try {
            Log.d(TAG, "Trying ChartLyrics API...")
            lyrics = ChartLyricsApi.searchLyrics(artist, title)
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Found lyrics from ChartLyrics")
                return OnlineSearchResult(lyrics, LyricsSource.CHARTLYRICS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "ChartLyrics search error", e)
        }
        
        // 6. 尝试 Happi.dev（音乐信息API）
        try {
            Log.d(TAG, "Trying Happi API...")
            lyrics = HappiApi.searchLyrics(artist, title)
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Found lyrics from Happi")
                return OnlineSearchResult(lyrics, LyricsSource.HAPPI)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Happi search error", e)
        }
        
        // 7. 尝试 SimpleLyrics（爬虫方案）
        try {
            Log.d(TAG, "Trying SimpleLyrics (scraper)...")
            lyrics = SimpleLyricsApi.searchLyrics(artist, title)
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Found lyrics from SimpleLyrics")
                return OnlineSearchResult(lyrics, LyricsSource.SIMPLE_LYRICS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "SimpleLyrics search error", e)
        }
        
        // 8. 最后尝试 Canarado/Lyrist（最终兜底）
        try {
            Log.d(TAG, "Trying Canarado API...")
            lyrics = CanaradoApi.searchLyrics(title, artist)
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Found lyrics from Canarado")
                return OnlineSearchResult(lyrics, LyricsSource.CANARADO)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Canarado search error", e)
        }
        
        Log.d(TAG, "All online sources failed")
        return null
    }
    
    /**
     * 在线搜索结果
     */
    private data class OnlineSearchResult(
        val lyrics: String,
        val source: LyricsSource
    )
    
    /**
     * 清除歌词缓存
     */
    fun clearCache(): Boolean {
        return LyricsCache.clearCache(context)
    }
    
    /**
     * 获取缓存大小
     */
    fun getCacheSize(): Long {
        return LyricsCache.getCacheSize(context)
    }
}
