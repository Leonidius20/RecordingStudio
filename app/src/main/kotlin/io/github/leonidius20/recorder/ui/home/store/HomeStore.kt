package io.github.leonidius20.recorder.ui.home.store

import com.arkivanov.mvikotlin.core.store.Store
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Intent
import io.github.leonidius20.recorder.ui.home.store.HomeStore.State
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Label
import kotlinx.coroutines.flow.StateFlow

interface HomeStore : Store<Intent, State, Label> {

    // todo: shared flow with buffer
    val amplitudes: StateFlow<Int>

    // todo: make sure it only updates every 1 sec
    //  and add to main state??
    val timerText: StateFlow<String>

    sealed interface Intent {

    }

    sealed interface Label {

    }

    data class State(
        val dummy: String,
    )

}
