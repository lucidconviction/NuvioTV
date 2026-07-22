package com.robbdeeze.nuviotv.ui.screens.multi

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.robbdeeze.nuviotv.domain.model.IptvChannel

data class WindowStream(
    val id: String,
    val channel: IptvChannel,
    val slotIndex: Int,
    val isPlaying: Boolean = true
)

object MultiWindowStore {
    const val MAX_PLAYERS = 9

    val streams: MutableList<WindowStream> = mutableStateListOf()
    val volumes: MutableMap<String, Float> = mutableStateMapOf()
    val playerHandleIds: MutableMap<String, Int> = mutableStateMapOf()
    val resizeModes: MutableMap<String, Int> = mutableStateMapOf()
    var audioFocusId: String? by mutableStateOf(null)
    var currentLayout: MultiWindowLayout? by mutableStateOf(null)
    var layoutLocked: Boolean by mutableStateOf(false)
    var fullScreenStreamId: String? by mutableStateOf(null)

    private var nextHandleId = 1

    fun addToSlot(channel: IptvChannel, slotIndex: Int) {
        val existing = streams.indexOfFirst { it.slotIndex == slotIndex }
        if (existing >= 0) {
            val old = streams[existing]
            playerHandleIds.remove(old.id)
            volumes.remove(old.id)
            resizeModes.remove(old.id)
            if (audioFocusId == old.id) audioFocusId = null
            streams[existing] = WindowStream(
                id = "stream_${channel.id}_$slotIndex",
                channel = channel,
                slotIndex = slotIndex
            )
        } else {
            if (streams.size >= MAX_PLAYERS) return
            streams.add(
                WindowStream(
                    id = "stream_${channel.id}_$slotIndex",
                    channel = channel,
                    slotIndex = slotIndex
                )
            )
        }
        resizeModes["stream_${channel.id}_$slotIndex"] = RESIZE_FIT
        if (streams.size == 1) {
            setAudioFocus("stream_${channel.id}_$slotIndex")
        }
    }

    // addToSlot overload for cell options (hot-swap)
    fun addToSlot(channel: IptvChannel, slotIndex: Int, existingStreamId: String) {
        val existing = streams.indexOfFirst { it.slotIndex == slotIndex }
        if (existing >= 0) {
            val old = streams[existing]
            playerHandleIds.remove(old.id)
            volumes.remove(old.id)
            resizeModes.remove(old.id)
            if (audioFocusId == old.id) audioFocusId = null
            streams[existing] = WindowStream(
                id = existingStreamId,
                channel = channel,
                slotIndex = slotIndex
            )
        }
        resizeModes[existingStreamId] = RESIZE_FIT
    }

    fun remove(streamId: String) {
        val idx = streams.indexOfFirst { it.id == streamId }
        if (idx >= 0) {
            playerHandleIds.remove(streamId)
            volumes.remove(streamId)
            resizeModes.remove(streamId)
            if (audioFocusId == streamId) audioFocusId = null
            streams.removeAt(idx)
        }
    }

    fun swap(indexA: Int, indexB: Int) {
        if (indexA !in streams.indices || indexB !in streams.indices) return
        val a = streams[indexA]
        val b = streams[indexB]
        streams[indexA] = a.copy(slotIndex = b.slotIndex)
        streams[indexB] = b.copy(slotIndex = a.slotIndex)
    }

    fun muteAll() {
        volumes.clear()
        audioFocusId = null
        playerHandleIds.forEach { (streamId, handleId) ->
            MultiWindowPlayerManager.setVolume(handleId, 0f)
        }
    }

    fun pauseAll() {
        streams.forEach { stream ->
            val handleId = playerHandleIds[stream.id]
            if (handleId != null) MultiWindowPlayerManager.pause(handleId)
        }
    }

    fun playAll() {
        streams.forEach { stream ->
            val handleId = playerHandleIds[stream.id]
            if (handleId != null) MultiWindowPlayerManager.play(handleId)
        }
    }

    fun closeAll() {
        MultiWindowPlayerManager.releaseAll()
        streams.clear()
        volumes.clear()
        playerHandleIds.clear()
        resizeModes.clear()
        audioFocusId = null
        currentLayout = null
        layoutLocked = false
    }

    fun refreshStream(streamId: String) {
        val stream = streams.find { it.id == streamId } ?: return
        MultiWindowPlayerManager.releasePlayerForStream(streamId)
        playerHandleIds.remove(streamId)
        val handle = MultiWindowPlayerManager.createPlayer(streamId, stream.channel.url)
        playerHandleIds[streamId] = handle.id
        val volume = volumes[streamId] ?: if (audioFocusId == streamId) 1f else 0f
        MultiWindowPlayerManager.setVolume(handle.id, volume)
    }

    fun setVolume(streamId: String, volume: Float) {
        volumes[streamId] = volume.coerceIn(0f, 1f)
        if (volume > 0f) {
            setAudioFocus(streamId)
        }
    }

    fun setAudioFocus(streamId: String) {
        audioFocusId = streamId
        playerHandleIds.forEach { (sid, handleId) ->
            MultiWindowPlayerManager.setVolume(
                handleId,
                if (sid == streamId) (volumes[sid] ?: 1f) else 0f
            )
        }
    }

    fun setResizeMode(streamId: String, mode: Int) {
        resizeModes[streamId] = mode
    }

    fun setLayout(layout: MultiWindowLayout?) {
        currentLayout = layout
        layoutLocked = layout != null
    }

    fun togglePlayPause(streamId: String) {
        val idx = streams.indexOfFirst { it.id == streamId }
        if (idx >= 0) {
            val updated = streams[idx].copy(isPlaying = !streams[idx].isPlaying)
            streams[idx] = updated
        }
    }

    fun toggleFullScreen(streamId: String?) {
        fullScreenStreamId = if (fullScreenStreamId == streamId) null else streamId
    }

    fun allocateHandleId(): Int = nextHandleId++

    fun clear() {
        streams.clear()
        volumes.clear()
        playerHandleIds.clear()
        resizeModes.clear()
        audioFocusId = null
        currentLayout = null
        layoutLocked = false
        fullScreenStreamId = null
    }
}
