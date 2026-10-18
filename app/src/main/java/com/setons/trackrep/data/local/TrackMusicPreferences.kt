package com.setons.trackrep.data.local

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Persisted user preferences for YouTube Music integration, including the default
 * workout song, artist, station, or playlist to stream when music playback is triggered.
 */
object TrackMusicPreferences {
    private const val PREFS_NAME = "trackrep_music_prefs"
    private const val KEY_DEFAULT_MUSIC_QUERY = "default_music_query"
    private const val KEY_DEFAULT_IS_PLAYLIST = "default_music_is_playlist"

    const val DEFAULT_FALLBACK_QUERY = "Workout Music"

    // Observable states synchronized with SharedPreferences
    var defaultMusicQueryState by mutableStateOf(DEFAULT_FALLBACK_QUERY)
        private set

    var isDefaultPlaylistState by mutableStateOf(true)
        private set

    private var isInitialized = false

    fun init(context: Context) {
        if (!isInitialized) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            defaultMusicQueryState = prefs.getString(KEY_DEFAULT_MUSIC_QUERY, DEFAULT_FALLBACK_QUERY) ?: DEFAULT_FALLBACK_QUERY
            isDefaultPlaylistState = prefs.getBoolean(KEY_DEFAULT_IS_PLAYLIST, true)
            isInitialized = true
        }
    }

    fun getDefaultMusicQuery(context: Context): String {
        init(context)
        return defaultMusicQueryState
    }

    fun isDefaultPlaylist(context: Context): Boolean {
        init(context)
        return isDefaultPlaylistState
    }

    fun setDefaultMusic(context: Context, query: String, isPlaylist: Boolean = false) {
        val trimmed = query.trim()
        val finalQuery = if (trimmed.isBlank()) DEFAULT_FALLBACK_QUERY else trimmed
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_DEFAULT_MUSIC_QUERY, finalQuery)
            .putBoolean(KEY_DEFAULT_IS_PLAYLIST, isPlaylist)
            .apply()

        defaultMusicQueryState = finalQuery
        isDefaultPlaylistState = isPlaylist
    }
}
