package com.robbdeeze.nuviotv.data.local

import android.content.Context
import android.content.SharedPreferences
class TeleNutzStorage(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "telenutz"
        private const val KEY_BOOKMARKS = "telenutz_bookmarks"
        private const val KEY_DOWNLOADS = "telenutz_downloads"
        private const val KEY_RECENT_SEARCHES = "telenutz_recent_searches"
    }

    private val prefs: SharedPreferences
        get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadBookmarksPayload(): String? = prefs.getString(KEY_BOOKMARKS, null)

    fun saveBookmarksPayload(payload: String) {
        prefs.edit().putString(KEY_BOOKMARKS, payload).apply()
    }

    fun loadDownloadsPayload(): String? = prefs.getString(KEY_DOWNLOADS, null)

    fun saveDownloadsPayload(payload: String) {
        prefs.edit().putString(KEY_DOWNLOADS, payload).apply()
    }

    fun loadRecentSearchesPayload(): String? = prefs.getString(KEY_RECENT_SEARCHES, null)

    fun saveRecentSearchesPayload(payload: String) {
        prefs.edit().putString(KEY_RECENT_SEARCHES, payload).apply()
    }
}
