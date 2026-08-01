package io.github.leonidius20.recorder.ui.recordings_list.viewmodel

import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineBootstrapper
import com.arkivanov.mvikotlin.extensions.coroutines.coroutineExecutorFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.domain.recordings_list.Recording
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.Intent
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.State
import kotlinx.coroutines.launch

interface RecordingsListStore : Store<Intent, State, Nothing> {

    sealed interface Intent {

        data object ClearSelection : Intent

    }

    data class State(
        // todo: lift out
        val recordings: List<Recording> = emptyList(),
        val selectedItems: Set<Recording> = emptySet(),
    )

}



internal sealed interface Label {
    // ...
}

class CalculatorStoreFactory(
    private val storeFactory: StoreFactory,
    private val repository: RecordingsListRepository,
) {

    fun create(): RecordingsListStore = object : RecordingsListStore, Store<Intent, State, Nothing> by storeFactory.create(
        name = "RecordingsListStore",
        initialState = State(),
        bootstrapper = coroutineBootstrapper {
            dispatch(Action.SubscribeToRecordingsList)
        },
        executorFactory = coroutineExecutorFactory {
            ExecutorImpl(repository)
        },
        reducer = { msg ->
            when(msg) {
                is Msg.UpdateList -> copy(recordings = msg.newList) // todo: arrow-kt
                else -> copy() // todo: why? it's sealed??
            }
        }
    ) {}

    private sealed interface Action {
        object SubscribeToRecordingsList : Action
    }

    private sealed interface Msg {

        data class UpdateList(
            val newList: List<Recording>
        ): Msg

    }

    private class ExecutorImpl(
        private val repository: RecordingsListRepository,
    ) : CoroutineExecutor<Intent, Action, State, Msg, Nothing>() {

        override fun executeIntent(intent: Intent) {
            when(intent) {
                is Intent.ClearSelection -> {
                    // dispatch msg to clear selection
                }
            }
        }

        override fun executeAction(action: Action) {
            when(action) {
                is Action.SubscribeToRecordingsList -> {
                    // todo: find out how to tie the lifecycle of this
                    //  to viewmodel lifecycle
                    scope.launch {
                        repository.recordings.collect {
                            dispatch(Msg.UpdateList(it))
                        }
                    }
                }
            }
        }
    }

}

