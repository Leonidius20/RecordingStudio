package io.github.leonidius20.recorder.data.recorder.observers

import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.github.leonidius20.recorder.data.recorder.RecorderService
import io.github.leonidius20.recorder.data.recorder.RecordingControlBroadcastReceiver
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class ControlObserver(
    val service: RecorderService, // should be recorder service context
) : SystemEventObserver {

    private val context get() = service

    override val events: Channel<SystemEvent> = Channel()

    private lateinit var recControlBroadcastReceiver: RecordingControlBroadcastReceiver

    override fun register() {
        recControlBroadcastReceiver = RecordingControlBroadcastReceiver {
            service.lifecycleScope.launch {
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
