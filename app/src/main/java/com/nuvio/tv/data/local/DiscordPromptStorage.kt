package com.robbdeeze.nuviotv.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordPromptStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isDismissed(): Boolean = prefs.getBoolean(KEY_DISMISSED, false)

    fun setDismissed() {
        prefs.edit().putBoolean(KEY_DISMISSED, true).apply()
    }

    companion object {
        private const val PREFS_NAME = "nuvio_discord_prompt"
        private const val KEY_DISMISSED = "discord_prompt_dismissed"
    }
}
