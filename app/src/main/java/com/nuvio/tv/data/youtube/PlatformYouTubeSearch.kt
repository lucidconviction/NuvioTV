package com.robbdeeze.nuviotv.data.youtube

import com.robbdeeze.nuviotv.core.network.HttpClient
import com.robbdeeze.nuviotv.core.network.NewPipeDownloader
import com.robbdeeze.nuviotv.domain.model.VidNutzVideo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object PlatformYouTubeSearch {
    @Volatile
    private var isInitialized = false

    fun initNewPipeIfNeeded() {
        if (!isInitialized) {
            synchronized(this) {
                if (!isInitialized) {
                    NewPipe.init(NewPipeDownloader(HttpClient.client))
                    isInitialized = true
                }
            }
        }
    }

    suspend fun search(query: String): List<VidNutzVideo> = withContext(Dispatchers.IO) {
        try {
            initNewPipeIfNeeded()
            val service = ServiceList.YouTube
            val searchExtractor = service.getSearchExtractor(query)
            searchExtractor.fetchPage()
            
            @Suppress("UNCHECKED_CAST")
            val items = searchExtractor.initialPage!!.items as List<org.schabi.newpipe.extractor.InfoItem>
            val videos = mutableListOf<VidNutzVideo>()
            
            for (item in items) {
                if (item is StreamInfoItem) {
                    val durationSec = item.duration.toInt()
                    val views = item.viewCount
                    val videoId = item.url.substringAfter("v=", "").ifEmpty {
                        item.url.substringAfter("/watch?v=", "")
                    }.ifEmpty {
                        item.url.substringAfter("youtu.be/", "")
                    }
                    if (videoId.isEmpty()) continue

                    videos.add(
                        VidNutzVideo(
                            videoId = videoId,
                            title = item.name ?: "No Title",
                            thumbnailUrl = item.thumbnails?.firstOrNull()?.url ?: "",
                            channelName = item.uploaderName ?: "Unknown",
                            durationSeconds = durationSec,
                            viewCount = views,
                            uploadDate = item.textualUploadDate ?: ""
                        )
                    )
                }
            }
            videos
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%d:%02d:%02d", h, m, s)
        } else {
            String.format("%d:%02d", m, s)
        }
    }
}
