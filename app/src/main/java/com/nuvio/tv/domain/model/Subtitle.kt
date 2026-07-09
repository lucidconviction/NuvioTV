package com.robbdeeze.nuviotv.domain.model

import androidx.compose.runtime.Immutable
import com.robbdeeze.nuviotv.ui.util.languageCodeToName

@Immutable
data class Subtitle(
    val id: String,
    val url: String,
    val lang: String,
    val addonName: String,
    val addonLogo: String?
) {
    fun getDisplayLanguage(): String = languageCodeToName(lang)

    companion object {
        fun languageCodeToName(code: String): String = com.robbdeeze.nuviotv.ui.util.languageCodeToName(code)
    }
}
