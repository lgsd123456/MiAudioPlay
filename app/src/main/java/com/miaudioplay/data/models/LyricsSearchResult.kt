package com.miaudioplay.data.models

data class LyricsSearchResult(
    val content: String,        // LRC格式歌词内容
    val source: LyricsSource,   // 来源
    val cached: Boolean = false // 是否来自缓存
)
