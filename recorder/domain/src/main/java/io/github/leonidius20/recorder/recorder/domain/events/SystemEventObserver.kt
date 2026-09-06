package io.github.leonidius20.recorder.recorder.domain.events

import kotlinx.coroutines.flow.Flow

interface SystemEventObserver {

    val eventsFlow: Flow<SystemEvent>

}
