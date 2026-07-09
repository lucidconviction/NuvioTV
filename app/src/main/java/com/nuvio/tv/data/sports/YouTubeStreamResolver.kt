package com.robbdeeze.nuviotv.data.sports

import com.robbdeeze.nuviotv.data.youtube.PlatformYouTubeSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList

object YouTubeStreamResolver {
    suspend fun resolveStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            PlatformYouTubeSearch.initNewPipeIfNeeded()
            val service = ServiceList.YouTube
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            
            val streamExtractor = service.getStreamExtractor(videoUrl)
            streamExtractor.fetchPage()

            val videoStreams = (streamExtractor.videoStreams as? List<*>)?.filterIsInstance<org.schabi.newpipe.extractor.stream.VideoStream>() ?: emptyList()
            val audioStreams = (streamExtractor.audioStreams as? List<*>)?.filterIsInstance<org.schabi.newpipe.extractor.stream.AudioStream>() ?: emptyList()

            // Find first progressive video stream or fallback to audio stream
            val stream = videoStreams.firstOrNull { !it.url.isNullOrEmpty() }
                ?: audioStreams.firstOrNull { !it.url.isNullOrEmpty() }
            
            stream?.url
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
