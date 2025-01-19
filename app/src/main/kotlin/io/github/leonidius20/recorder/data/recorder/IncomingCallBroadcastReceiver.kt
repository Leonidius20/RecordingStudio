package io.github.leonidius20.recorder.data.recorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat


class IncomingCallBroadcastReceiver(
    private val callback: () -> Unit,
): BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val state = intent!!.getStringExtra(TelephonyManager.EXTRA_STATE)

        if (state == TelephonyManager.EXTRA_STATE_RINGING) {
            callback()
        }
    }

    fun registerInContext(context: Context) {
        val intentFilter = IntentFilter(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        ContextCompat.registerReceiver(
            context, this,
            intentFilter, ContextCompat.RECEIVER_EXPORTED)
    }

}