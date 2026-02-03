package com.miaudioplay.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Happi.dev Lyrics API (备用免费API)
 * https://happi.dev/docs/music
 */
object HappiApi {
    private const val TAG = "MiAudioPlay HappiApi"
    // 使用公开的端点（无需API key的版本）
    private const val BASE_URL = "https://api.happi.dev/v1/music"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * 搜索歌词
     */
    suspend fun searchLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$artist $title"
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "$BASE_URL?q=$encodedQuery&limit=1&type=track"
            
            Log.d(TAG, "Searching: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MiAudioPlay/1.1")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Search failed: ${response.code}")
                return@withContext null
            }
            
            // Try to parse and extract lyrics if available
            val lyrics = parseLyricsFromResponse(responseBody)
            
            if (lyrics.isNullOrBlank()) {
                Log.d(TAG, "No lyrics in response")
                return@withContext null
            }
            
            Log.d(TAG, "Lyrics found")
            convertPlainToLrc(lyrics)
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            null
        }
    }
    
    private fun parseLyricsFromResponse(json: String): String? {
        return try {
            val jsonObject = gson.fromJson(json, com.google.gson.JsonObject::class.java)
            val result = jsonObject.getAsJsonArray("result")
            
            if (result != null && result.size() > 0) {
                val firstResult = result[0].asJsonObject
                firstResult.get("lyrics")?.asString
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun convertPlainToLrc(plainLyrics: String): String {
        return plainLyrics.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "[00:00.00]$it" }
    }
}
