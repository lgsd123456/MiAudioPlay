package com.miaudioplay.utils

import com.miaudioplay.data.models.Song

/**
 * Utility object for fuzzy matching songs based on search queries
 */
object FuzzyMatcher {
    
    /**
     * Filters songs based on a search query
     * Searches across title, artist, and album fields with case-insensitive matching
     * 
     * @param songs List of songs to filter
     * @param query Search query string
     * @return Filtered list of songs matching the query
     */
    fun filterSongs(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) {
            return songs
        }
        
        val normalizedQuery = query.trim().lowercase()
        
        return songs.filter { song ->
            matchesSong(song, normalizedQuery)
        }
    }
    
    /**
     * Checks if a song matches the given query
     * Query is matched against title, artist, and album
     * 
     * @param song Song to check
     * @param normalizedQuery Normalized (lowercase, trimmed) query string
     * @return true if song matches query
     */
    private fun matchesSong(song: Song, normalizedQuery: String): Boolean {
        val title = song.title.lowercase()
        val artist = song.artist.lowercase()
        val album = song.album.lowercase()
        
        return title.contains(normalizedQuery) ||
               artist.contains(normalizedQuery) ||
               album.contains(normalizedQuery)
    }
    
    /**
     * Calculates a relevance score for a song based on the query
     * Higher scores indicate better matches
     * 
     * @param song Song to score
     * @param query Search query
     * @return Relevance score (higher is better)
     */
    fun calculateRelevanceScore(song: Song, query: String): Int {
        if (query.isBlank()) return 0
        
        val normalizedQuery = query.trim().lowercase()
        val title = song.title.lowercase()
        val artist = song.artist.lowercase()
        val album = song.album.lowercase()
        
        var score = 0
        
        // Exact matches get highest score
        if (title == normalizedQuery) score += 100
        if (artist == normalizedQuery) score += 80
        if (album == normalizedQuery) score += 60
        
        // Starts with query gets high score
        if (title.startsWith(normalizedQuery)) score += 50
        if (artist.startsWith(normalizedQuery)) score += 40
        if (album.startsWith(normalizedQuery)) score += 30
        
        // Contains query gets base score
        if (title.contains(normalizedQuery)) score += 10
        if (artist.contains(normalizedQuery)) score += 8
        if (album.contains(normalizedQuery)) score += 5
        
        return score
    }
    
    /**
     * Filters and sorts songs by relevance to the query
     * 
     * @param songs List of songs to filter and sort
     * @param query Search query string
     * @return Filtered and sorted list of songs
     */
    fun filterAndSortByRelevance(songs: List<Song>, query: String): List<Song> {
        if (query.isBlank()) {
            return songs
        }
        
        val normalizedQuery = query.trim().lowercase()
        
        return songs
            .filter { matchesSong(it, normalizedQuery) }
            .sortedByDescending { calculateRelevanceScore(it, query) }
    }
}
