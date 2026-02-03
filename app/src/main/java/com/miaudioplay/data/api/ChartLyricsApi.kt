package com.miaudioplay.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

/**
 * ChartLyrics API - SOAP-based lyrics API
 * http://www.chartlyrics.com/api.aspx
 */
object ChartLyricsApi {
    private const val TAG = "MiAudioPlay ChartLyricsApi"
    private const val BASE_URL = "http://api.chartlyrics.com/apiv1.asmx"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    
    /**
     * 搜索歌词
     */
    suspend fun searchLyrics(artist: String, title: String): String? = withContext(Dispatchers.IO) {
        try {
            // First search for the song
            val searchUrl = "$BASE_URL/SearchLyricDirect?artist=${urlEncode(artist)}&song=${urlEncode(title)}"
            
            Log.d(TAG, "Searching: $searchUrl")
            
            val request = Request.Builder()
                .url(searchUrl)
                .header("User-Agent", "MiAudioPlay/1.1")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (!response.isSuccessful || responseBody == null) {
                Log.e(TAG, "Search failed: ${response.code}")
                return@withContext null
            }
            
            // Parse XML response
            val lyrics = parseXmlLyrics(responseBody)
            
            if (lyrics.isNullOrBlank()) {
                Log.d(TAG, "No lyrics found")
                return@withContext null
            }
            
            Log.d(TAG, "Lyrics found (${lyrics.length} chars)")
            convertPlainToLrc(lyrics)
        } catch (e: Exception) {
            Log.e(TAG, "Search error", e)
            null
        }
    }
    
    private fun parseXmlLyrics(xml: String): String? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray()))
            
            val lyricsNodes = doc.getElementsByTagName("Lyric")
            if (lyricsNodes.length > 0) {
                lyricsNodes.item(0).textContent
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "XML parsing error", e)
            null
        }
    }
    
    private fun urlEncode(str: String): String {
        return java.net.URLEncoder.encode(str, "UTF-8")
    }
    
    private fun convertPlainToLrc(plainLyrics: String): String {
        return plainLyrics.lines()
            .filter { it.isNotBlank() }
            .joinToString("\n") { "[00:00.00]$it" }
    }
}
