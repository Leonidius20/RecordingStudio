package io.github.leonidius20.recorder.domain.events

import kotlinx.coroutines.flow.Flow

interface SystemEventObserver {

    val eventsFlow: Flow<SystemEvent>

}
