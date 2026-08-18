package io.github.leonidius20.recorder.data.recorder

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class UiStateUpdater(
    private val callback: (RecorderServiceLauncher.State) -> Unit
): DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        val service = owner as RecorderService
        if (service.state.value == RecorderService.State.ERROR) {
            callback(RecorderServiceLauncher.State.ERROR)
        } else {
            callback(RecorderServiceLauncher.State.IDLE)
        }
    }

}