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
 * QQ音乐API - 中文歌词获取
 * 使用第三方镜像服务
 */
object QQMusicApi {
    private const val TAG = "MiAudioPlay QQMusicApi"
    
    // 使用公开的QQ音乐API镜像
    private const val BASE_URL = "https://c.y.qq.com"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    
    /**
     * 搜索并获取歌词
     */
    suspend fun searchAndGetLyrics(
        songName: String,
        artistName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Step 1: 搜索歌曲
            Log.d(TAG, "Searching song: $artistName - $songName")
            val songId = searchSong(songName, artistName)
            
            if (songId == null) {
                Log.d(TAG, "Song not found")
                return@withContext null
            }
            
            Log.d(TAG, "Found song ID: $songId")
            
            // Step 2: 获取歌词
            val lyrics = getLyrics(songId)
            
            if (lyrics != null) {
                Log.d(TAG, "✓ Lyrics fetched successfully")
            } else {
                Log.d(TAG, "No lyrics available for this song")
            }
            
            lyrics
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lyrics", e)
            null
        }
    }
    
    /**
     * 搜索歌曲，返回歌曲ID
     */
    private suspend fun searchSong(songName: String, artistName: String): String? = withContext(Dispatchers.IO) {
        try {
            val keyword = "$artistName $songName"
            val encodedKeyword = URLEncoder.encode(keyword, "UTF-8")
            
            val url = "$BASE_URL/soso/fcgi-bin/client_search_cp?ct=24&qqmusic_ver=1298&new_json=1&remoteplace=txt.yqq.song&" +
                    "searchid=&t=0&aggr=1&cr=1&catZhida=1&lossless=0&flag_qc=0&p=1&n=10&w=$encodedKeyword&" +
                    "g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=utf8&outCharset=utf-8&notice=0&" +
                    "platform=yqq.json&needNewCode=0"
            
            Log.d(TAG, "Search URL: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://y.qq.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Search failed: ${response.code}")
                return@withContext null
            }
            
            // 解析搜索结果
            val searchResult = gson.fromJson(responseBody, QQSearchResponse::class.java)
            val songs = searchResult.data?.song?.list
            
            if (songs.isNullOrEmpty()) {
                Log.d(TAG, "No songs found in search results")
                return@withContext null
            }
            
            // 返回第一个结果的song mid
            val firstSong = songs[0]
            Log.d(TAG, "Found: ${firstSong.name} - ${firstSong.singer?.firstOrNull()?.name}")
            firstSong.mid
            
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            null
        }
    }
    
    /**
     * 获取歌词
     */
    private suspend fun getLyrics(songMid: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = "$BASE_URL/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=$songMid&" +
                    "g_tk=5381&loginUin=0&hostUin=0&format=json&inCharset=utf8&outCharset=utf-8&" +
                    "notice=0&platform=yqq.json&needNewCode=0"
            
            Log.d(TAG, "Fetching lyrics: $url")
            
            val request = Request.Builder()
                .url(url)
                .header("Referer", "https://y.qq.com")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Lyrics fetch failed: ${response.code}")
                return@withContext null
            }
            
            // 解析歌词
            val lyricsResult = gson.fromJson(responseBody, QQLyricsResponse::class.java)
            val base64Lyrics = lyricsResult.lyric
            
            if (base64Lyrics.isNullOrBlank()) {
                Log.d(TAG, "No lyrics in response")
                return@withContext null
            }
            
            // Base64解码
            val decodedLyrics = String(android.util.Base64.decode(base64Lyrics, android.util.Base64.DEFAULT))
            
            if (decodedLyrics.isBlank()) {
                Log.d(TAG, "Decoded lyrics is empty")
                return@withContext null
            }
            
            decodedLyrics
            
        } catch (e: Exception) {
            Log.e(TAG, "Lyrics fetch error", e)
            null
        }
    }
    
    /**
     * 搜索响应数据类
     */
    private data class QQSearchResponse(
        @SerializedName("code")
        val code: Int?,
        
        @SerializedName("data")
        val data: SearchData?
    )
    
    private data class SearchData(
        @SerializedName("song")
        val song: SongData?
    )
    
    private data class SongData(
        @SerializedName("list")
        val list: List<QQSong>?
    )
    
    private data class QQSong(
        @SerializedName("mid")
        val mid: String?,
        
        @SerializedName("name")
        val name: String?,
        
        @SerializedName("singer")
        val singer: List<QQSinger>?
    )
    
    private data class QQSinger(
        @SerializedName("name")
        val name: String?
    )
    
    /**
     * 歌词响应数据类
     */
    private data class QQLyricsResponse(
        @SerializedName("retcode")
        val retcode: Int?,
        
        @SerializedName("lyric")
        val lyric: String?,
        
        @SerializedName("trans")
        val trans: String?
    )
}
