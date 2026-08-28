package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.data.recorder.observers.ControlObserver
import io.github.leonidius20.recorder.data.recorder.observers.IncomingCallObserver
import io.github.leonidius20.recorder.data.recorder.observers.LowBatteryObserver
import io.github.leonidius20.recorder.data.recorder.observers.LowStorageObserver
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

class UnitedSystemEventObserver @Inject constructor(
    val controlObserver: ControlObserver,
    val lowBatteryObserver: LowBatteryObserver,
    val lowStorageObserver: LowStorageObserver,
    val incomingCallObserver: IncomingCallObserver,
) : SystemEventObserver {

    val observers = listOf(
        controlObserver,
        lowBatteryObserver,
        lowStorageObserver,
        incomingCallObserver,
    )

    override val eventsFlow: Flow<SystemEvent>
        get() = observers.map { it.eventsFlow }.merge()

}
