package io.github.leonidius20.recorder.ui.audio_settings.store

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Intent
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Label
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.State
import javax.inject.Inject
import javax.inject.Provider

interface AudioSettingsStore : Store<Intent, State, Label> {

    sealed interface Intent {

        data object Test : Intent

    }

    data class State(
        val text: String = "",
    )

    sealed interface Label {

    }

}

class AudioSettingsStoreFactory @Inject constructor(
    private val storeFactory: StoreFactory,
    private val executorProvider: Provider<ExecutorImpl>,
) {

    sealed interface Action {

        data object SubscribeToUpdates : Action

    }

    sealed interface Msg {

    }


    class ExecutorImpl @Inject constructor(

    ): CoroutineExecutor<Intent, Action, State, Msg, Label>() {

    }

    fun create(): AudioSettingsStore = object : AudioSettingsStore, Store<Intent, State, Label> by storeFactory.create(
        name = "AudioSettingsStore",
        initialState = State(),
        bootstrapper = SimpleBootstrapper(Action.SubscribeToUpdates),
        executorFactory = { executorProvider.get() },
        reducer = { msg ->
            copy() // todo: when {}
        }
    ) {}

}

