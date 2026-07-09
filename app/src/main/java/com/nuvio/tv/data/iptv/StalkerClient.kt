package com.nuvio.tv.data.iptv

import com.nuvio.tv.domain.model.IptvChannel

class StalkerClient(
    private val portalUrl: String,
    private val macAddress: String
) {
    fun getChannels(): List<IptvChannel> {
        // Stub implementation for Stalker portal integration
        return emptyList()
    }
}
