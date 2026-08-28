package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recorder.RecordingControlBroadcastReceiver
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class ControlObserver @Inject constructor(
    @param:ApplicationContext val context: Context,
) : SystemEventObserver {

    override val eventsFlow = callbackFlow {
        val receiver = RecordingControlBroadcastReceiver {
            trySend(it)
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

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

}
