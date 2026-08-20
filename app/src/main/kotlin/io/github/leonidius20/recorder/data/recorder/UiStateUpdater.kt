package io.github.leonidius20.recorder.data.recorder

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.leonidius20.recorder.domain.recorder.RecorderState

class UiStateUpdater(
    private val callback: (RecorderState) -> Unit
): DefaultLifecycleObserver {

    override fun onDestroy(owner: LifecycleOwner) {
        val service = owner as RecorderService
        if (service.state.value == RecorderService.State.ERROR) {
            callback(RecorderState.ERROR)
        } else {
            callback(RecorderState.IDLE)
        }
    }

}