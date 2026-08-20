package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import io.github.leonidius20.recorder.data.recorder.BroadcastReceiverWithCallback
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

class LowStorageObserver(
    val context: Context,
    val scope: CoroutineScope,
) : SystemEventObserver {

    override val events: Channel<SystemEvent> = Channel()

    private lateinit var lowStorageBroadcastReceiver: BroadcastReceiverWithCallback

    override fun register() {
        lowStorageBroadcastReceiver = BroadcastReceiverWithCallback {
            scope.launch {
                events.send(SystemEvent.LOW_STORAGE)
            }
        }.apply {
            val intentFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            ContextCompat.registerReceiver(
                context, this,
                intentFilter, ContextCompat.RECEIVER_EXPORTED
            )
        }
    }

    override fun unregister() {
        context.unregisterReceiver(lowStorageBroadcastReceiver)
    }


}
