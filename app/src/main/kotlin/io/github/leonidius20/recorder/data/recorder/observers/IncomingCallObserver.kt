package io.github.leonidius20.recorder.data.recorder.observers

import androidx.lifecycle.lifecycleScope
import io.github.leonidius20.recorder.data.recorder.IncomingCallBroadcastReceiver
import io.github.leonidius20.recorder.data.recorder.RecorderService
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class IncomingCallObserver(
    private val service: RecorderService,
) : SystemEventObserver {

    override val events: Channel<SystemEvent> = Channel()

    private lateinit var callBroadcastReceiver: IncomingCallBroadcastReceiver

    override fun register() {
        callBroadcastReceiver = IncomingCallBroadcastReceiver {
            service.lifecycleScope.launch {
                events.send(SystemEvent.INCOMING_CALL)
            }
        }.apply {
            registerInContext(service)
        }
    }

    override fun unregister() {
        service.unregisterReceiver(callBroadcastReceiver)
    }

}
