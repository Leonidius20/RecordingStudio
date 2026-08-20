package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Context
import io.github.leonidius20.recorder.data.recorder.IncomingCallBroadcastReceiver
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class IncomingCallObserver(
    val context: Context,
    val scope: CoroutineScope,
) : SystemEventObserver {

    override val events: Channel<SystemEvent> = Channel()

    private lateinit var callBroadcastReceiver: IncomingCallBroadcastReceiver

    override fun register() {
        callBroadcastReceiver = IncomingCallBroadcastReceiver {
            scope.launch {
                events.send(SystemEvent.INCOMING_CALL)
            }
        }.apply {
            registerInContext(context)
        }
    }

    override fun unregister() {
        context.unregisterReceiver(callBroadcastReceiver)
    }

}
