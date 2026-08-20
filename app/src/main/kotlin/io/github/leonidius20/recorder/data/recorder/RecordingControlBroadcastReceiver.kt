package io.github.leonidius20.recorder.data.recorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.leonidius20.recorder.domain.recorder.SystemEvent

class RecordingControlBroadcastReceiver(
    private val callback: (SystemEvent) -> Unit,
): BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PAUSE_OR_RESUME) {
            callback(SystemEvent.TOGGLE_REC_PAUSE)
        } else if (intent.action == ACTION_STOP) {
            callback(SystemEvent.STOP)
        } else {
            Log.d("RecControlBReceiver", "Unknown action: ${intent.action}")
        }
    }

    companion object {
        const val ACTION_PAUSE_OR_RESUME = "io.github.leonidius20.recorder.action_pause_or_resume"
        const val ACTION_STOP = "io.github.leonidius20.recorder.action_stop"
    }

}