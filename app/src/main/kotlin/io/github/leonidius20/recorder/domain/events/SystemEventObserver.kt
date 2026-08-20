package io.github.leonidius20.recorder.domain.events

import kotlinx.coroutines.channels.Channel

interface SystemEventObserver {

    val events: Channel<SystemEvent>

    fun register()

    fun unregister()

}
