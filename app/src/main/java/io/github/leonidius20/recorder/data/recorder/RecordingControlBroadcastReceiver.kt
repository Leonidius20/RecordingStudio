package io.github.leonidius20.recorder.data.recorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class RecordingControlBroadcastReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val service = context as RecorderService

        if (intent.action == ACTION_PAUSE_OR_RESUME) {
            service.toggleRecPause()
        } else if (intent.action == ACTION_STOP) {
            service.stop()
        } else {
            Log.d("RecControlBReceiver", "Unknown action: ${intent.action}")
        }
    }

    companion object {
        const val ACTION_PAUSE_OR_RESUME = "io.github.leonidius20.recorder.action_pause_or_resume"
        const val ACTION_STOP = "io.github.leonidius20.recorder.action_stop"
    }

}