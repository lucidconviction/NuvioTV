package com.robbdeeze.nuviotv.domain.model

import kotlinx.serialization.Serializable

// ── Telegram Auth State ─────────────────────────────────────────────────

enum class TeleNutzTab { SEARCH, BOOKMARKS, DOWNLOADS }

enum class TelegramAuthState {
    None,
    WaitPhoneNumber,
    WaitQrCode,
    WaitCode,
    WaitPassword,
    Ready,
    Closed,
}

data class TelegramAuthInfo(
    val state: TelegramAuthState = TelegramAuthState.None,
    val qrCodeUrl: String? = null,
    val error: String? = null,
)

// ── TDLib data types ─────────────────────────────────────────────────────

data class TdChannel(
    val id: Long,
    val username: String,
    val title: String,
    val description: String,
)

data class FileDownloadState(
    val path: String,
    val totalSize: Long = 0L,
    val downloadedSize: Long = 0L,
    val isComplete: Boolean = false,
)

data class TdMessage(
    val id: Long,
    val chatId: Long,
    val chatUsername: String,
    val chatTitle: String,
    val text: String,
    val hasVideo: Boolean,
    val thumbnailUrl: String?,
    val date: String,
    val fileId: Int = 0,
    val localPath: String? = null,
    val fileSize: Long = 0L,
)

data class FileUploadResult(
    val fileId: Int,
    val fileUniqueId: String,
    val chatId: Long,
    val messageId: Long,
    val fileSize: Long,
)

// ── TeleNutz video model ─────────────────────────────────────────────────

@Serializable
data class TeleNutzVideo(
    val id: Long,
    val chatId: Long,
    val chatTitle: String,
    val text: String,
    val date: String,
    val thumbnailUrl: String?,
    val fileId: Int = 0,
    val localPath: String? = null,
    val fileSize: Long = 0L,
    val isDownloaded: Boolean = false,
    val isBookmarked: Boolean = false,
    val downloadProgress: Float = 0f,
)

// ── TeleNutz UI state ────────────────────────────────────────────────────

data class TeleNutzUiState(
    val selectedTab: TeleNutzTab = TeleNutzTab.SEARCH,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchResults: List<TeleNutzVideo> = emptyList(),
    val bookmarkedVideos: List<TeleNutzVideo> = emptyList(),
    val downloadedVideos: List<TeleNutzVideo> = emptyList(),
    val activeDownloads: Map<Int, Float> = emptyMap(),
    val error: String? = null,
    val authState: TelegramAuthState = TelegramAuthState.None,
    val authQrUrl: String? = null,
    val authError: String? = null,
    val phoneInput: String = "",
    val codeInput: String = "",
) {
    val needsAuth: Boolean
        get() = authState == TelegramAuthState.WaitPhoneNumber ||
                authState == TelegramAuthState.WaitQrCode ||
                authState == TelegramAuthState.WaitCode ||
                authState == TelegramAuthState.WaitPassword
}

// ── Simplified player launch model for TeleNutz ──────────────────────────
// Callers should convert this to the TV project's own player launch mechanism.

data class TeleNutzPlayerLaunch(
    val profileId: Int = 0,
    val title: String,
    val sourceUrl: String,
    val streamTitle: String,
    val providerName: String = "Telegram",
    val parentMetaId: String = "telenutz",
    val parentMetaType: String = "telenutz",
    val autoPlayQueueUrls: List<String> = emptyList(),
    val autoPlayQueueTitles: List<String> = emptyList(),
    val autoPlayQueueIndex: Int = 0,
)
