package com.miaudioplay.data

import com.miaudioplay.data.models.LyricLine
import java.io.File
import java.util.regex.Pattern

object LrcParser {
    private val TIME_PATTERN = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})]")
    
    fun parse(lrcContent: String): List<LyricLine> {
        val lyrics = mutableListOf<LyricLine>()
        
        lrcContent.lines().forEach { line ->
            val matcher = TIME_PATTERN.matcher(line)
            var lastEnd = 0
            
            while (matcher.find()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0
                val millisStr = matcher.group(3) ?: "0"
                val millis = if (millisStr.length == 2) {
                    millisStr.toLongOrNull()?.times(10) ?: 0
                } else {
                    millisStr.toLongOrNull() ?: 0
                }
                
                val timestamp = minutes * 60 * 1000 + seconds * 1000 + millis
                lastEnd = matcher.end()
                
                val text = line.substring(lastEnd).trim()
                if (text.isNotEmpty()) {
                    lyrics.add(LyricLine(timestamp, text))
                }
            }
        }
        
        return lyrics.sortedBy { it.timestamp }
    }
    
    fun parseFromFile(file: File): List<LyricLine> {
        return if (file.exists() && file.canRead()) {
            parse(file.readText())
        } else {
            emptyList()
        }
    }
    
    fun findLrcFile(audioPath: String): File? {
        val audioFile = File(audioPath)
        val baseName = audioFile.nameWithoutExtension
        val parentDir = audioFile.parentFile ?: return null
        
        // Try different LRC file name patterns
        val lrcPatterns = listOf(
            "$baseName.lrc",
            "$baseName.LRC",
            "${baseName.lowercase()}.lrc"
        )
        
        for (pattern in lrcPatterns) {
            val lrcFile = File(parentDir, pattern)
            if (lrcFile.exists()) {
                return lrcFile
            }
        }
        
        return null
    }
}
