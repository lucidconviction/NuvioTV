package com.robbdeeze.nuviotv.ui.screens.player

import com.robbdeeze.nuviotv.domain.model.IptvChannel

object IptvPlayerStore {
    var channels: List<IptvChannel> = emptyList()
    var currentIndex: Int = 0
    var returnToSubScreen: String = "Iptv"
    var launchedFromSlotIndex: Int = -1

    fun currentChannel(): IptvChannel? = channels.getOrNull(currentIndex)

    fun setChannels(list: List<IptvChannel>, startIndex: Int) {
        channels = list
        currentIndex = startIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    }

    fun clear() {
        channels = emptyList()
        currentIndex = 0
        // Preserve returnToSubScreen — it's set before navigation and should persist
        // so the hub knows where to return after the player exits
        launchedFromSlotIndex = -1
    }
}
