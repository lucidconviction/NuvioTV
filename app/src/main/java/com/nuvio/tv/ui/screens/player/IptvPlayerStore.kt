package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.IptvChannel

object IptvPlayerStore {
    var channels: List<IptvChannel> = emptyList()
    var currentIndex: Int = 0
    var returnToSubScreen: String = "Iptv"

    fun currentChannel(): IptvChannel? = channels.getOrNull(currentIndex)

    fun setChannels(list: List<IptvChannel>, startIndex: Int) {
        channels = list
        currentIndex = startIndex.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    }

    fun clear() {
        channels = emptyList()
        currentIndex = 0
        returnToSubScreen = "Iptv"
    }
}
