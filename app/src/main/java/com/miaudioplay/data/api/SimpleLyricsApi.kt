package com.miaudioplay.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * 多来源歌词爬虫
 * 从各大歌词网站抓取歌词
 */
object SimpleLyricsApi {
    private const val TAG = "MiAudioPlay SimpleLyricsApi"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
    
    /**
     * 尝试从多个网站抓取歌词
     */
    suspend fun searchLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        // 按优先级依次尝试不同的网站
        
        // 1. AZLyrics
        try {
            val lyrics = tryAZLyrics(artist, title)
            if (lyrics != null) return@withContext lyrics
        } catch (e: Exception) {
            Log.e(TAG, "AZLyrics failed, trying next source", e)
        }
        
        // 2. Lyrics.com
        try {
            val lyrics = tryLyricsCom(artist, title)
            if (lyrics != null) return@withContext lyrics
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics.com failed, trying next source", e)
        }
        
        // 3. SongLyrics
        try {
            val lyrics = trySongLyrics(artist, title)
            if (lyrics != null) return@withContext lyrics
        } catch (e: Exception) {
            Log.e(TAG, "SongLyrics failed, trying next source", e)
        }
        
        // 4. Genius
        try {
            val lyrics = tryGenius(artist, title)
            if (lyrics != null) return@withContext lyrics
        } catch (e: Exception) {
            Log.e(TAG, "Genius failed, trying next source", e)
        }
        
        // 5. MetroLyrics
        try {
            val lyrics = tryMetroLyrics(artist, title)
            if (lyrics != null) return@withContext lyrics
        } catch (e: Exception) {
            Log.e(TAG, "MetroLyrics failed, trying next source", e)
        }
        
        null
    }
    
    /**
     * AZLyrics - https://www.azlyrics.com
     */
    private suspend fun tryAZLyrics(artist: String, title: String): String? {
        return try {
            val cleanArtist = artist.lowercase()
                .replace(Regex("[^a-z0-9]"), "")
                .replace("the", "")
            val cleanTitle = title.lowercase()
                .replace(Regex("[^a-z0-9]"), "")
            
            val url = "https://www.azlyrics.com/lyrics/$cleanArtist/$cleanTitle.html"
            
            Log.d(TAG, "Trying AZLyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            
            if (response.isSuccessful && html != null) {
                val lyrics = extractAZLyrics(html)
                if (!lyrics.isNullOrBlank()) {
                    Log.d(TAG, "✓ Found lyrics from AZLyrics")
                    return convertPlainToLrc(lyrics)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "AZLyrics error", e)
            null
        }
    }
    
    private fun extractAZLyrics(html: String): String? {
        return try {
            // AZLyrics的歌词在注释标记之后的div中
            val startMarker = "<!-- Usage of azlyrics.com content"
            val endMarker = "</div>"
            
            var startIndex = html.indexOf(startMarker)
            if (startIndex == -1) return null
            
            startIndex = html.indexOf("-->", startIndex) + 3
            val endIndex = html.indexOf(endMarker, startIndex)
            
            if (startIndex > 0 && endIndex > startIndex) {
                html.substring(startIndex, endIndex)
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .trim()
                    .takeIf { it.length > 50 }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Lyrics.com - https://www.lyrics.com
     */
    private suspend fun tryLyricsCom(artist: String, title: String): String? {
        return try {
            val query = URLEncoder.encode("$artist $title", "UTF-8")
            val searchUrl = "https://www.lyrics.com/lyrics/$query"
            
            Log.d(TAG, "Trying Lyrics.com: $searchUrl")
            
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            
            if (response.isSuccessful && html != null) {
                val lyrics = extractLyricsCom(html)
                if (!lyrics.isNullOrBlank()) {
                    Log.d(TAG, "✓ Found lyrics from Lyrics.com")
                    return convertPlainToLrc(lyrics)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics.com error", e)
            null
        }
    }
    
    private fun extractLyricsCom(html: String): String? {
        return try {
            // Lyrics.com的歌词在id="lyric-body-text"的pre标签中
            val startMarker = "id=\"lyric-body-text\""
            val endMarker = "</pre>"
            
            var startIndex = html.indexOf(startMarker)
            if (startIndex == -1) return null
            
            startIndex = html.indexOf(">", startIndex) + 1
            val endIndex = html.indexOf(endMarker, startIndex)
            
            if (startIndex > 0 && endIndex > startIndex) {
                html.substring(startIndex, endIndex)
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .trim()
                    .takeIf { it.length > 50 }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * SongLyrics.com - http://www.songlyrics.com
     */
    private suspend fun trySongLyrics(artist: String, title: String): String? {
        return try {
            val cleanArtist = artist.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()
            val url = "http://www.songlyrics.com/$cleanArtist/$cleanTitle-lyrics/"
            
            Log.d(TAG, "Trying SongLyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            
            if (response.isSuccessful && html != null) {
                val lyrics = extractSongLyrics(html)
                if (!lyrics.isNullOrBlank()) {
                    Log.d(TAG, "✓ Found lyrics from SongLyrics")
                    return convertPlainToLrc(lyrics)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "SongLyrics error", e)
            null
        }
    }
    
    private fun extractSongLyrics(html: String): String? {
        return try {
            val startMarker = "id=\"songLyricsDiv\""
            val endMarker = "</p>"
            
            var startIndex = html.indexOf(startMarker)
            if (startIndex == -1) return null
            
            startIndex = html.indexOf(">", startIndex) + 1
            val endIndex = html.indexOf(endMarker, startIndex)
            
            if (startIndex > 0 && endIndex > startIndex) {
                html.substring(startIndex, endIndex)
                    .replace(Regex("<[^>]*>"), "\n")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .lines()
                    .filter { it.isNotBlank() }
                    .joinToString("\n")
                    .trim()
                    .takeIf { it.length > 50 }
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Genius - https://genius.com
     */
    private suspend fun tryGenius(artist: String, title: String): String? {
        return try {
            val cleanArtist = artist.replace(" ", "-")
            val cleanTitle = title.replace(" ", "-")
            val url = "https://genius.com/$cleanArtist-$cleanTitle-lyrics"
            
            Log.d(TAG, "Trying Genius: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            
            if (response.isSuccessful && html != null) {
                val lyrics = extractGenius(html)
                if (!lyrics.isNullOrBlank()) {
                    Log.d(TAG, "✓ Found lyrics from Genius")
                    return convertPlainToLrc(lyrics)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Genius error", e)
            null
        }
    }
    
    private fun extractGenius(html: String): String? {
        return try {
            // Genius使用data-lyrics-container属性
            val pattern = Regex("data-lyrics-container=\"true\"[^>]*>(.*?)</div>", RegexOption.DOT_MATCHES_ALL)
            val matches = pattern.findAll(html)
            
            val lyricsBuilder = StringBuilder()
            for (match in matches) {
                val content = match.groupValues[1]
                    .replace(Regex("<br[^>]*>"), "\n")
                    .replace(Regex("<[^>]*>"), "")
                    .replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .trim()
                
                if (content.isNotBlank()) {
                    lyricsBuilder.append(content).append("\n")
                }
            }
            
            lyricsBuilder.toString().trim().takeIf { it.length > 50 }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * MetroLyrics (可能已关闭，但尝试)
     */
    private suspend fun tryMetroLyrics(artist: String, title: String): String? {
        return try {
            val cleanArtist = artist.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9]"), "-").lowercase()
            val url = "http://www.metrolyrics.com/$cleanTitle-lyrics-$cleanArtist.html"
            
            Log.d(TAG, "Trying MetroLyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val html = response.body?.string()
            
            if (response.isSuccessful && html != null) {
                val lyrics = extractMetroLyrics(html)
                if (!lyrics.isNullOrBlank()) {
                    Log.d(TAG, "✓ Found lyrics from MetroLyrics")
                    return convertPlainToLrc(lyrics)
                }
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "MetroLyrics error", e)
            null
        }
    }
    
    private fun extractMetroLyrics(html: String): String? {
        return try {
            val startMarker = "class=\"verse\""
            val lyrics = StringBuilder()
            var searchIndex = 0
            
            while (true) {
                val verseStart = html.indexOf(startMarker, searchIndex)
                if (verseStart == -1) break
                
                val contentStart = html.indexOf(">", verseStart) + 1
                val contentEnd = html.indexOf("</p>", contentStart)
                
                if (contentStart > 0 && contentEnd > contentStart) {
                    val verse = html.substring(contentStart, contentEnd)
                        .replace(Regex("<[^>]*>"), "\n")
                        .replace("&quot;", "\"")
                        .replace("&amp;", "&")
                        .trim()
                    
                    lyrics.append(verse).append("\n\n")
                }
                
                searchIndex = contentEnd + 4
            }
            
            lyrics.toString().trim().takeIf { it.length > 50 }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * 将纯文本歌词转换为LRC格式
     */
    private fun convertPlainToLrc(plainLyrics: String): String {
        return plainLyrics.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "[00:00.00]$it" }
    }
}
