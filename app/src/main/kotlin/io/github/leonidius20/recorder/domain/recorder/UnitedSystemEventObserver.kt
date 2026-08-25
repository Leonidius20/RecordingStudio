package io.github.leonidius20.recorder.domain.recorder

import android.content.Context
import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.data.recorder.observers.ControlObserver
import io.github.leonidius20.recorder.data.recorder.observers.IncomingCallObserver
import io.github.leonidius20.recorder.data.recorder.observers.LowBatteryObserver
import io.github.leonidius20.recorder.data.recorder.observers.LowStorageObserver
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.merge
import javax.inject.Inject

@ServiceScoped
class UnitedSystemEventObserver @Inject constructor(
    val context: Context,
    val scope: CoroutineScope // service context and scope
) {

    val events get() = observers.map { it.events.consumeAsFlow() }.merge()

    val observers = listOf(
        ControlObserver(context, scope),
        LowBatteryObserver(context, scope),
        LowStorageObserver(context, scope),
        IncomingCallObserver(context, scope),
    )

    fun register() {
        observers.forEach(SystemEventObserver::register)
    }

    fun unregister() {
        observers.forEach(SystemEventObserver::unregister)
    }

}
