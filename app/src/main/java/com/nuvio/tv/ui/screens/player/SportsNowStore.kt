package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.IptvChannel
import com.nuvio.tv.domain.model.SportEvent

object SportsNowStore {
    var liveEvents: List<SportEvent> = emptyList()
    var onSwitchToEvent: ((SportEvent) -> Unit)? = null
}
