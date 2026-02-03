package com.miaudioplay.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Canarado Lyrics API - Free lyrics scraper
 */
object CanaradoApi {
    private const val TAG = "MiAudioPlay CanaradoApi"
    private const val BASE_URL = "https://lyrist.vercel.app/api"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    /**
     * 搜索歌词
     * @param title 歌曲名
     * @param artist 歌手名
     * @return 歌词内容（纯文本）
     */
    suspend fun searchLyrics(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$artist $title"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            
            val url = "$BASE_URL/$encodedQuery"
            
            Log.d(TAG, "Searching lyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MiAudioPlay/1.0")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val lyrics = response.body?.string()
            
            if (!response.isSuccessful || lyrics.isNullOrBlank()) {
                Log.e(TAG, "Search failed: ${response.code}")
                return@withContext null
            }
            
            Log.d(TAG, "Lyrics found")
            // Convert to LRC format
            convertPlainToLrc(lyrics)
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            null
        }
    }
    
    /**
     * 将普通歌词转换为LRC格式
     */
    private fun convertPlainToLrc(plainLyrics: String): String {
        return plainLyrics.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "[00:00.00]$it" }
    }
}
