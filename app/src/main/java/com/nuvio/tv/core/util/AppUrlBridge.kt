package com.robbdeeze.nuviotv.core.util

import android.content.Intent
import android.net.Uri

sealed class DeepLink {
    data class MagnetLink(val uri: String) : DeepLink()
}

object AppUrlBridge {

    fun parseDeepLink(intent: Intent): DeepLink? {
        val uri = intent.data ?: return null
        return when (uri.scheme?.lowercase()) {
            "magnet" -> DeepLink.MagnetLink(uri.toString())
            else -> null
        }
    }

    fun isMagnetIntent(intent: Intent): Boolean {
        return intent.data?.scheme?.lowercase() == "magnet"
    }

    fun extractInfoHash(magnetUri: String): String? {
        val btihMatch = Regex("btih:([a-fA-F0-9]{40})").find(magnetUri)
        return btihMatch?.groupValues?.get(1)
    }

    fun extractDisplayName(magnetUri: String): String? {
        val dnMatch = Regex("[&?]dn=([^&]+)").find(magnetUri)
        return dnMatch?.groupValues?.get(1)
            ?.replace("+", " ")
            ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
    }
}
