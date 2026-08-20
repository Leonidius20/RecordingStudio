package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import io.github.leonidius20.recorder.data.recorder.BroadcastReceiverWithCallback
import io.github.leonidius20.recorder.data.recorder.RecorderService
import io.github.leonidius20.recorder.domain.recorder.SystemEvent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class LowBatteryObserver(
    val service: RecorderService, // should be recorder service context
) {
    private val context get() = service

    private lateinit var lowBatteryBroadcastReceiver: BroadcastReceiverWithCallback

    val events: Channel<SystemEvent> = Channel()

    fun register() {
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

    fun unregister() {
        context.unregisterReceiver(lowBatteryBroadcastReceiver)
    }

}
