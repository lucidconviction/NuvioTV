package com.robbdeeze.nuviotv.ui.screens.player

import com.robbdeeze.nuviotv.domain.model.IptvChannel
import com.robbdeeze.nuviotv.domain.model.SportEvent

object SportsNowStore {
    var liveEvents: List<SportEvent> = emptyList()
    var onSwitchToEvent: ((SportEvent) -> Unit)? = null
}
