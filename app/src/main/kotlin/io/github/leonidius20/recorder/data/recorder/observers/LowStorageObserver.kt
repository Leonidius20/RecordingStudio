package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recorder.BroadcastReceiverWithCallback
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class LowStorageObserver @Inject constructor(
    @param:ApplicationContext val context: Context,
) : SystemEventObserver {

    override val eventsFlow = callbackFlow {
        val receiver = BroadcastReceiverWithCallback {
            trySend(SystemEvent.LOW_STORAGE)
        }.apply {
            val intentFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            ContextCompat.registerReceiver(
                context, this,
                intentFilter, ContextCompat.RECEIVER_EXPORTED
            )
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

}
