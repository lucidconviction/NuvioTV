package com.robbdeeze.nuviotv.data.iptv

import com.robbdeeze.nuviotv.domain.model.IptvChannel

class StalkerClient(
    private val portalUrl: String,
    private val macAddress: String
) {
    fun getChannels(): List<IptvChannel> {
        // Stub implementation for Stalker portal integration
        return emptyList()
    }
}
