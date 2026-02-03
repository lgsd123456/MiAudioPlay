package com.miaudioplay.data.api

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Lyrics.ovh API - Free lyrics API
 * https://lyricsovh.docs.apiary.io/
 */
object LyricsOvhApi {
    private const val TAG = "MiAudioPlay LyricsOvhApi"
    private const val BASE_URL = "https://api.lyrics.ovh/v1"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * 获取歌词
     * @param artist 歌手名
     * @param title 歌曲名
     * @return 歌词内容（纯文本）
     */
    suspend fun getLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            val encodedArtist = URLEncoder.encode(artist, "UTF-8")
            val encodedTitle = URLEncoder.encode(title, "UTF-8")
            
            val url = "$BASE_URL/$encodedArtist/$encodedTitle"
            
            Log.d(TAG, "Fetching lyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "MiAudioPlay/1.0")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Fetch failed: ${response.code}")
                return@withContext null
            }
            
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            val lyrics = jsonObject.get("lyrics")?.asString
            
            if (lyrics.isNullOrBlank()) {
                Log.d(TAG, "No lyrics available")
                return@withContext null
            }
            
            Log.d(TAG, "Lyrics fetched successfully")
            // Convert plain text to LRC format
            convertPlainToLrc(lyrics)
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error", e)
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
}
