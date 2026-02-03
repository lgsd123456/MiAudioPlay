package com.miaudioplay.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * LRCLIB API - Free lyrics database
 * https://lrclib.net/
 */
object LrcLibApi {
    private const val TAG = "MiAudioPlay LrcLibApi"
    private const val BASE_URL = "https://lrclib.net/api"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * 搜索歌词
     * @param trackName 歌曲名
     * @param artistName 歌手名
     * @param albumName 专辑名（可选）
     * @param duration 时长（秒，可选）
     * @return 歌词内容
     */
    suspend fun searchLyrics(
        trackName: String,
        artistName: String,
        albumName: String? = null,
        duration: Int? = null
    ): String? = withContext(Dispatchers.IO) {
        try {
            val encodedTrack = URLEncoder.encode(trackName, "UTF-8")
            val encodedArtist = URLEncoder.encode(artistName, "UTF-8")
            
            var url = "$BASE_URL/search?track_name=$encodedTrack&artist_name=$encodedArtist"
            
            if (!albumName.isNullOrBlank()) {
                val encodedAlbum = URLEncoder.encode(albumName, "UTF-8")
                url += "&album_name=$encodedAlbum"
            }
            
            if (duration != null && duration > 0) {
                url += "&duration=$duration"
            }
            
            Log.d(TAG, "Searching lyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MiAudioPlay/1.0")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Search failed: ${response.code}")
                return@withContext null
            }
            
            // Parse response as array
            val results = gson.fromJson(responseBody, Array<LrcLibResult>::class.java)
            
            if (results.isEmpty()) {
                Log.d(TAG, "No lyrics found")
                return@withContext null
            }
            
            // Use the first result
            val firstResult = results[0]
            
            // Prefer synced lyrics, fallback to plain lyrics
            val lyrics = if (!firstResult.syncedLyrics.isNullOrBlank()) {
                Log.d(TAG, "Found synced lyrics")
                firstResult.syncedLyrics
            } else if (!firstResult.plainLyrics.isNullOrBlank()) {
                Log.d(TAG, "Found plain lyrics (converting to LRC format)")
                // Convert plain lyrics to LRC format
                convertPlainToLrc(firstResult.plainLyrics)
            } else {
                Log.d(TAG, "No lyrics in result")
                null
            }
            
            lyrics
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            null
        }
    }
    
    /**
     * 将普通歌词转换为LRC格式（无时间轴）
     */
    private fun convertPlainToLrc(plainLyrics: String): String {
        return plainLyrics.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "[00:00.00]$it" }
    }
    
    /**
     * LRCLIB API 响应数据类
     */
    data class LrcLibResult(
        @SerializedName("id")
        val id: Long,
        
        @SerializedName("trackName")
        val trackName: String,
        
        @SerializedName("artistName")
        val artistName: String,
        
        @SerializedName("albumName")
        val albumName: String?,
        
        @SerializedName("duration")
        val duration: Double,  // Changed from Int to Double to handle decimal values like 235.2848896
        
        @SerializedName("plainLyrics")
        val plainLyrics: String?,
        
        @SerializedName("syncedLyrics")
        val syncedLyrics: String?
    )
}
