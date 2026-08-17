package io.github.leonidius20.recorder.data.recorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BroadcastReceiverWithCallback(
    private val callback: () -> Unit,
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        callback()
    }

}