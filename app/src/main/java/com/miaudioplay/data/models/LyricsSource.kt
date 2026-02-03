package com.miaudioplay.data.models

enum class LyricsSource {
    LOCAL_FILE,      // 本地LRC文件
    CACHE,           // 缓存
    LRCLIB,          // LRCLIB API
    NETEASE,         // 网易云音乐
    QQMUSIC,         // QQ音乐
    LYRICS_OVH,      // Lyrics.ovh
    CHARTLYRICS,     // ChartLyrics
    HAPPI,           // Happi.dev
    SIMPLE_LYRICS,   // 网页爬虫聚合
    CANARADO         // Canarado/Lyrist
}
