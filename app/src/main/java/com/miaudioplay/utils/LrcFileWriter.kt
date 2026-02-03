package com.miaudioplay.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

/**
 * LRC文件写入工具
 */
object LrcFileWriter {
    private const val TAG = "LrcFileWriter"
    
    /**
     * 将歌词保存为LRC文件到音频文件同目录
     * @param audioPath 音频文件路径
     * @param lrcContent LRC格式的歌词内容
     * @return 是否保存成功
     */
    fun saveLrcFile(audioPath: String, lrcContent: String): Boolean {
        return try {
            val audioFile = File(audioPath)
            if (!audioFile.exists()) {
                Log.w(TAG, "Audio file does not exist: $audioPath")
                return false
            }
            
            val parentDir = audioFile.parentFile
            if (parentDir == null || !parentDir.exists()) {
                Log.w(TAG, "Parent directory does not exist")
                return false
            }
            
            // 检查目录是否可写
            if (!parentDir.canWrite()) {
                Log.w(TAG, "Parent directory is not writable: ${parentDir.absolutePath}")
                return false
            }
            
            // 生成LRC文件名
            val baseName = audioFile.nameWithoutExtension
            val lrcFile = File(parentDir, "$baseName.lrc")
            
            // 如果文件已存在，不覆盖
            if (lrcFile.exists()) {
                Log.d(TAG, "LRC file already exists, skipping: ${lrcFile.absolutePath}")
                return false
            }
            
            // 写入文件
            lrcFile.writeText(lrcContent, Charsets.UTF_8)
            Log.d(TAG, "✓ LRC file saved successfully: ${lrcFile.absolutePath}")
            
            true
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied to write LRC file", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save LRC file", e)
            false
        }
    }
    
    /**
     * 检查是否可以写入LRC文件
     */
    fun canWriteLrcFile(audioPath: String): Boolean {
        return try {
            val audioFile = File(audioPath)
            val parentDir = audioFile.parentFile
            parentDir?.canWrite() == true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 获取LRC文件路径（如果存在）
     */
    fun getLrcFilePath(audioPath: String): String? {
        return try {
            val audioFile = File(audioPath)
            val baseName = audioFile.nameWithoutExtension
            val parentDir = audioFile.parentFile ?: return null
            val lrcFile = File(parentDir, "$baseName.lrc")
            
            if (lrcFile.exists()) {
                lrcFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
