package com.miaudioplay.data

import android.content.Context
import android.util.Log
import java.io.File

object LyricsCache {
    private const val TAG = "LyricsCache"
    private const val CACHE_DIR_NAME = "lyrics"
    
    /**
     * 获取歌词缓存目录
     */
    private fun getCacheDir(context: Context): File {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * 生成缓存文件名
     * 格式: {artist} - {title}.lrc
     * 移除特殊字符以确保文件名合法
     */
    private fun generateCacheFileName(artist: String, title: String): String {
        val sanitizedArtist = artist.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s-]"), "").trim()
        val sanitizedTitle = title.replace(Regex("[^a-zA-Z0-9\\u4e00-\\u9fa5\\s-]"), "").trim()
        return "$sanitizedArtist - $sanitizedTitle.lrc"
    }
    
    /**
     * 保存歌词到缓存
     */
    fun saveLyrics(context: Context, artist: String, title: String, lrcContent: String): Boolean {
        return try {
            val cacheFile = File(getCacheDir(context), generateCacheFileName(artist, title))
            cacheFile.writeText(lrcContent, Charsets.UTF_8)
            Log.d(TAG, "Lyrics cached: ${cacheFile.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache lyrics", e)
            false
        }
    }
    
    /**
     * 从缓存加载歌词
     */
    fun loadLyrics(context: Context, artist: String, title: String): String? {
        return try {
            val cacheFile = File(getCacheDir(context), generateCacheFileName(artist, title))
            if (cacheFile.exists() && cacheFile.canRead()) {
                Log.d(TAG, "Lyrics loaded from cache: ${cacheFile.name}")
                cacheFile.readText(Charsets.UTF_8)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cached lyrics", e)
            null
        }
    }
    
    /**
     * 清除所有缓存
     */
    fun clearCache(context: Context): Boolean {
        return try {
            val cacheDir = getCacheDir(context)
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cache cleared")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
            false
        }
    }
    
    /**
     * 获取缓存大小（字节）
     */
    fun getCacheSize(context: Context): Long {
        return try {
            val cacheDir = getCacheDir(context)
            cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}
