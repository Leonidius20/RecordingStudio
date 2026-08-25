package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.leonidius20.recorder.data.recorder.RecordingControlBroadcastReceiver
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.launch

class ControlObserver(
    val context: Context,
    val scope: CoroutineScope,
) : SystemEventObserver {

    private val events: Channel<SystemEvent> = Channel()

    override val eventsFlow: Flow<SystemEvent>
        get() = events.consumeAsFlow()

    private lateinit var recControlBroadcastReceiver: RecordingControlBroadcastReceiver

    override fun register() {
        recControlBroadcastReceiver = RecordingControlBroadcastReceiver {
            scope.launch {
                events.send(it)
            }
        }.apply {
            val intentFilter = IntentFilter().apply {
                addAction(RecordingControlBroadcastReceiver.ACTION_PAUSE_OR_RESUME)
                addAction(RecordingControlBroadcastReceiver.ACTION_STOP)
            }

            ContextCompat.registerReceiver(
                context, this,
                intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun unregister() {
        context.unregisterReceiver(recControlBroadcastReceiver)
    }

}
