package io.github.leonidius20.recorder.data.recorder.observers

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.data.recorder.IncomingCallBroadcastReceiver
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class IncomingCallObserver @Inject constructor(
    @param:ApplicationContext val context: Context,
) : SystemEventObserver {

    override val eventsFlow = callbackFlow {
        val receiver = IncomingCallBroadcastReceiver {
            // todo: load test to make sure nothing is dropped?
            trySend(SystemEvent.INCOMING_CALL)
        }.apply {
            registerInContext(context)
        }

        awaitClose {
            context.unregisterReceiver(receiver)
        }
    }

}
