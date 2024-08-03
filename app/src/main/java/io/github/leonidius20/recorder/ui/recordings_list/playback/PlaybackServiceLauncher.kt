package io.github.leonidius20.recorder.ui.recordings_list.playback

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackServiceLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
): ServiceConnection {

    enum class State {
        IDLE, // player was not activated, no PlaybackService exists, UI should be hidden
        PLAYING,
    }


    // binder

    fun playFile() {
        // if binder is not null (i.e. service exists, run a method through the binder
        // otherwise start a new service
    }


    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        TODO("Not yet implemented")
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        TODO("Not yet implemented")
    }


}