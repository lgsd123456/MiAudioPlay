package com.miaudioplay.utils

import android.util.Log
import com.miaudioplay.data.api.CanaradoApi
import com.miaudioplay.data.api.ChartLyricsApi
import com.miaudioplay.data.api.HappiApi
import com.miaudioplay.data.api.LrcLibApi
import com.miaudioplay.data.api.LyricsOvhApi
import com.miaudioplay.data.api.NeteaseApi
import com.miaudioplay.data.api.QQMusicApi
import com.miaudioplay.data.api.SimpleLyricsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 歌词API测试工具
 */
object LyricsApiTester {
    private const val TAG = "LyricsApiTester"
    
    // 测试用的歌曲信息
    private const val TEST_ARTIST = "Taylor Swift"
    private const val TEST_TITLE = "Love Story"
    private const val TEST_ARTIST_CN = "周杰伦"
    private const val TEST_TITLE_CN = "晴天"
    
    /**
     * 测试所有歌词API
     */
    suspend fun testAllApis(): Map<String, ApiTestResult> = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, ApiTestResult>()
        
        Log.d(TAG, "=".repeat(60))
        Log.d(TAG, "开始测试所有歌词API")
        Log.d(TAG, "=".repeat(60))
        Log.d(TAG, "测试歌曲1: $TEST_ARTIST - $TEST_TITLE")
        Log.d(TAG, "测试歌曲2: $TEST_ARTIST_CN - $TEST_TITLE_CN")
        Log.d(TAG, "=".repeat(60))
        
        // Test LRCLIB
        results["LRCLIB"] = testLrcLib()
        
        // Test QQ Music
        results["QQ Music"] = testQQMusic()
        
        // Test NetEase
        results["NetEase"] = testNetEase()
        
        // Test Lyrics.ovh
        results["Lyrics.ovh"] = testLyricsOvh()
        
        // Test ChartLyrics
        results["ChartLyrics"] = testChartLyrics()
        
        // Test Happi
        results["Happi"] = testHappi()
        
        // Test SimpleLyrics
        results["SimpleLyrics"] = testSimpleLyrics()
        
        // Test Canarado
        results["Canarado"] = testCanarado()
        
        Log.d(TAG, "=".repeat(60))
        Log.d(TAG, "测试完成")
        Log.d(TAG, "=".repeat(60))
        logTestResults(results)
        Log.d(TAG, "=".repeat(60))
        
        results
    }
    
    private suspend fun testLrcLib(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing LRCLIB API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = LrcLibApi.searchLyrics(
                trackName = TEST_TITLE,
                artistName = TEST_ARTIST
            )
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ LRCLIB: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ LRCLIB: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ LRCLIB: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    private suspend fun testQQMusic(): ApiTestResult {
        val startTime = System.currentTimeMillis()
        return try {
            Log.d(TAG, "Testing QQ Music API...")
            val lyrics = QQMusicApi.searchAndGetLyrics(TEST_TITLE_CN, TEST_ARTIST_CN)
            val duration = System.currentTimeMillis() - startTime
            
            if (lyrics != null) {
                Log.d(TAG, "✓ QQ Music: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(
                    success = true,
                    message = "成功",
                    durationMs = duration,
                    lyricsPreview = lyrics.take(100)
                )
            } else {
                Log.d(TAG, "✗ QQ Music: No lyrics found")
                ApiTestResult(
                    success = false,
                    message = "未找到歌词",
                    durationMs = duration,
                    lyricsPreview = null
                )
            }
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - startTime
            Log.e(TAG, "✗ QQ Music: Error", e)
            ApiTestResult(
                success = false,
                message = "请求失败: ${e.message}",
                durationMs = duration,
                lyricsPreview = null
            )
        }
    }
    
    private suspend fun testNetEase(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing NetEase API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = NeteaseApi.searchAndGetLyrics(TEST_TITLE_CN, TEST_ARTIST_CN)
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ NetEase: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ NetEase: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ NetEase: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    private suspend fun testLyricsOvh(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing Lyrics.ovh API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = LyricsOvhApi.getLyrics(TEST_ARTIST, TEST_TITLE)
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Lyrics.ovh: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ Lyrics.ovh: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Lyrics.ovh: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    
    private suspend fun testChartLyrics(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing ChartLyrics API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = ChartLyricsApi.searchLyrics(TEST_ARTIST, TEST_TITLE)
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ ChartLyrics: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ ChartLyrics: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ ChartLyrics: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    private suspend fun testHappi(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing Happi API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = HappiApi.searchLyrics(TEST_ARTIST, TEST_TITLE)
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Happi: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ Happi: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Happi: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    private suspend fun testSimpleLyrics(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing SimpleLyrics API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = SimpleLyricsApi.searchLyrics(TEST_ARTIST, TEST_TITLE)
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ SimpleLyrics: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ SimpleLyrics: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ SimpleLyrics: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    private suspend fun testCanarado(): ApiTestResult {
        return try {
            Log.d(TAG, "Testing Canarado API...")
            val startTime = System.currentTimeMillis()
            
            val lyrics = CanaradoApi.searchLyrics(TEST_TITLE, TEST_ARTIST)
            
            val duration = System.currentTimeMillis() - startTime
            
            if (!lyrics.isNullOrBlank()) {
                Log.d(TAG, "✓ Canarado: Success (${duration}ms, ${lyrics.length} chars)")
                ApiTestResult(true, "成功", duration, lyrics.take(100))
            } else {
                Log.w(TAG, "✗ Canarado: No lyrics found")
                ApiTestResult(false, "未找到歌词", duration, null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Canarado: Failed - ${e.message}", e)
            ApiTestResult(false, "请求失败: ${e.message}", 0, null)
        }
    }
    
    private fun logTestResults(results: Map<String, ApiTestResult>) {
        Log.d(TAG, "")
        Log.d(TAG, "测试结果汇总:")
        Log.d(TAG, "-".repeat(60))
        
        var successCount = 0
        var failCount = 0
        
        results.forEach { (api, result) ->
            val status = if (result.success) {
                successCount++
                "✓ 成功"
            } else {
                failCount++
                "✗ 失败"
            }
            
            Log.d(TAG, String.format("%-20s %s (%dms)", api, status, result.durationMs))
            Log.d(TAG, "  消息: ${result.message}")
            
            if (result.lyricsPreview != null) {
                Log.d(TAG, "  预览: ${result.lyricsPreview.take(50)}...")
            }
            Log.d(TAG, "")
        }
        
        Log.d(TAG, "-".repeat(60))
        Log.d(TAG, "总计: $successCount 成功, $failCount 失败")
        Log.d(TAG, "")
    }
    
    data class ApiTestResult(
        val success: Boolean,
        val message: String,
        val durationMs: Long,
        val lyricsPreview: String?
    )
}
