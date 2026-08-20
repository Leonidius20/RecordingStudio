package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.github.leonidius20.recorder.data.recorder.BroadcastReceiverWithCallback
import io.github.leonidius20.recorder.data.recorder.RecorderService
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class LowBatteryObserver(
    val service: RecorderService, // should be recorder service context
) : SystemEventObserver {
    private val context get() = service

    private lateinit var lowBatteryBroadcastReceiver: BroadcastReceiverWithCallback

    override val events: Channel<SystemEvent> = Channel()

    override fun register() {
        lowBatteryBroadcastReceiver = BroadcastReceiverWithCallback(
            callback = {
                service.lifecycleScope.launch {
                    events.send(SystemEvent.LOW_BATTERY)
                }
            }
        ).apply {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_LOW)
            ContextCompat.registerReceiver(
                context, this,
                intentFilter, ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun unregister() {
        context.unregisterReceiver(lowBatteryBroadcastReceiver)
    }

}
