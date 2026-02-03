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
 * 网易云音乐API（非官方接口）
 */
object NeteaseApi {
    private const val TAG = "MiAudioPlay NeteaseApi"
    
    // 使用公开的网易云音乐API镜像
    private const val BASE_URL = "https://netease-cloud-music-api-psi-six.vercel.app"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * 搜索歌曲
     * @return 歌曲ID列表
     */
    suspend fun searchSong(title: String, artist: String? = null): List<SongSearchResult> = withContext(Dispatchers.IO) {
        try {
            val keywords = if (artist != null) {
                "$artist $title"
            } else {
                title
            }
            
            val encodedKeywords = URLEncoder.encode(keywords, "UTF-8")
            val url = "$BASE_URL/search?keywords=$encodedKeywords&limit=5"
            
            Log.d(TAG, "Searching song: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Search failed: ${response.code}")
                return@withContext emptyList()
            }
            
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            val result = jsonObject.getAsJsonObject("result")
            val songs = result?.getAsJsonArray("songs")
            
            if (songs == null || songs.size() == 0) {
                Log.d(TAG, "No songs found")
                return@withContext emptyList()
            }
            
            val results = mutableListOf<SongSearchResult>()
            for (i in 0 until songs.size()) {
                val song = songs[i].asJsonObject
                val id = song.get("id")?.asLong ?: continue
                val name = song.get("name")?.asString ?: ""
                val artistsArray = song.getAsJsonArray("artists")
                val artistName = if (artistsArray.size() > 0) {
                    artistsArray[0].asJsonObject.get("name")?.asString ?: ""
                } else {
                    ""
                }
                
                results.add(SongSearchResult(id, name, artistName))
            }
            
            Log.d(TAG, "Found ${results.size} songs")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            emptyList()
        }
    }
    
    /**
     * 获取歌词
     */
    suspend fun getLyrics(songId: Long): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/lyric?id=$songId"
            
            Log.d(TAG, "Fetching lyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Lyrics fetch failed: ${response.code}")
                return@withContext null
            }
            
            val jsonObject = gson.fromJson(responseBody, JsonObject::class.java)
            
            // 尝试获取lrc（普通歌词）
            val lrc = jsonObject.getAsJsonObject("lrc")?.get("lyric")?.asString
            
            if (lrc.isNullOrBlank()) {
                Log.d(TAG, "No lyrics available for song: $songId")
                return@withContext null
            }
            
            Log.d(TAG, "Lyrics fetched successfully")
            lrc
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics fetch error", e)
            null
        }
    }
    
    /**
     * 搜索并获取歌词（组合方法）
     */
    suspend fun searchAndGetLyrics(title: String, artist: String? = null): String? {
        val searchResults = searchSong(title, artist)
        
        if (searchResults.isEmpty()) {
            return null
        }
        
        // 尝试第一个结果
        for (result in searchResults) {
            val lyrics = getLyrics(result.id)
            if (!lyrics.isNullOrBlank()) {
                return lyrics
            }
        }
        
        return null
    }
    
    data class SongSearchResult(
        val id: Long,
        val name: String,
        val artist: String
    )
}
