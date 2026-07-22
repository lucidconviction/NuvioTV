package com.robbdeeze.nuviotv.ui.screens.multi

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class PlayerHandle(val id: Int)

const val RESIZE_FILL = 0
const val RESIZE_FIT = 1
const val RESIZE_FIXED_WIDTH = 2
const val RESIZE_FIXED_HEIGHT = 3
const val RESIZE_ZOOM = 4

enum class StreamHealth { Playing, Buffering, Error, Idle }

object MultiWindowPlayerManager {
    private val players = mutableMapOf<Int, ExoPlayer>()
    private var context: Context? = null
    private val streamToHandle = mutableMapOf<String, Int>()
    private var lastUsed = mutableMapOf<Int, Long>()
    private val healthStates = mutableMapOf<String, MutableStateFlow<StreamHealth>>()

    fun init(ctx: Context) {
        context = ctx
    }

    fun getHealthFlow(streamId: String): StateFlow<StreamHealth> {
        return healthStates.getOrPut(streamId) { MutableStateFlow(StreamHealth.Idle) }
    }

    fun createPlayer(streamId: String, url: String): PlayerHandle {
        val ctx = context ?: throw IllegalStateException("MultiWindowPlayerManager not initialized")
        val handleId = MultiWindowStore.allocateHandleId()

        if (players.size >= MultiWindowStore.MAX_PLAYERS) {
            val oldest = lastUsed.minByOrNull { it.value }?.key
            if (oldest != null) releasePlayer(oldest)
        }

        val player = ExoPlayer.Builder(ctx)
            .build()
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.playWhenReady = true
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                val health = healthStates.getOrPut(streamId) { MutableStateFlow(StreamHealth.Idle) }
                health.value = when (playbackState) {
                    Player.STATE_READY -> StreamHealth.Playing
                    Player.STATE_BUFFERING -> StreamHealth.Buffering
                    Player.STATE_ENDED -> StreamHealth.Idle
                    else -> StreamHealth.Error
                }
            }
        })

        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        players[handleId] = player
        streamToHandle[streamId] = handleId
        lastUsed[handleId] = System.currentTimeMillis()

        return PlayerHandle(handleId)
    }

    fun setVolume(handleId: Int, volume: Float) {
        players[handleId]?.volume = volume.coerceIn(0f, 1f)
    }

    fun setAudioFocus(focusHandleId: Int) {
        players.forEach { (id, player) ->
            player.volume = if (id == focusHandleId) 1f else 0f
        }
    }

    fun play(handleId: Int) {
        players[handleId]?.play()
    }

    fun pause(handleId: Int) {
        players[handleId]?.pause()
    }

    fun pauseAll() {
        players.values.forEach { it.pause() }
    }

    fun playAll() {
        players.values.forEach { it.play() }
    }

    fun getPlayer(handleId: Int): ExoPlayer? = players[handleId]

    fun refreshPlayer(streamId: String, url: String) {
        val handleId = streamToHandle[streamId] ?: return
        val player = players[handleId] ?: return
        val mediaItem = MediaItem.fromUri(url)
        player.stop()
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }

    fun releasePlayer(handleId: Int) {
        players[handleId]?.release()
        players.remove(handleId)
        streamToHandle.entries.removeAll { it.value == handleId }
        lastUsed.remove(handleId)
    }

    fun releasePlayerForStream(streamId: String) {
        val handleId = streamToHandle[streamId] ?: return
        releasePlayer(handleId)
        streamToHandle.remove(streamId)
        healthStates.remove(streamId)
    }

    fun releaseAll() {
        players.values.forEach { it.release() }
        players.clear()
        streamToHandle.clear()
        lastUsed.clear()
        healthStates.clear()
    }
}

@Composable
fun MultiWindowVideoSurface(
    playerHandle: PlayerHandle?,
    resizeMode: Int,
    modifier: Modifier = Modifier
) {
    val player = playerHandle?.let { MultiWindowPlayerManager.getPlayer(it.id) }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                useController = false
                this.resizeMode = when (resizeMode) {
                    RESIZE_FILL -> 1
                    RESIZE_FIT -> 0
                    RESIZE_FIXED_WIDTH -> 3
                    RESIZE_FIXED_HEIGHT -> 4
                    RESIZE_ZOOM -> 2
                    else -> 0
                }
                setBackgroundColor(android.graphics.Color.BLACK)
            }
        },
        update = { view ->
            view.player = player
            view.resizeMode = when (resizeMode) {
                RESIZE_FILL -> 1
                RESIZE_FIT -> 0
                RESIZE_FIXED_WIDTH -> 3
                RESIZE_FIXED_HEIGHT -> 4
                RESIZE_ZOOM -> 2
                else -> 0
            }
        },
        modifier = modifier
    )
}
