package io.github.leonidius20.recorder.ui.recordings_list.viewmodel

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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

@ViewModelScoped
class CalculatorStoreFactory @Inject constructor(
    //private val storeFactory: StoreFactory,
    private val repository: RecordingsListRepository,
) {

    // todo inject
    private val storeFactory: StoreFactory = DefaultStoreFactory()

    fun create(): RecordingsListStore = object : RecordingsListStore, Store<Intent, State, Nothing> by storeFactory.create(
        name = "RecordingsListStore",
        initialState = State(),
        bootstrapper = SimpleBootstrapper(Action.SubscribeToRecordingsList),
        executorFactory = {
            Timber.d("called executor factory")
            ExecutorImpl(repository)
        },
        reducer = { msg: Msg ->
            when(msg) {
                is Msg.UpdateList -> copy(recordings = msg.newList) // todo: arrow-kt
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

        init {
            Timber.d("created executor")
        }

        override fun executeIntent(intent: Intent) {
            when(intent) {
                is Intent.ClearSelection -> {
                    // dispatch msg to clear selection
                }
            }
        }

        override fun executeAction(action: Action) {
            Timber.d("Executing action $action")
            when(action) {
                is Action.SubscribeToRecordingsList -> {
                    // todo: find out how to tie the lifecycle of this
                    //  to viewmodel lifecycle

                    Timber.d("we start listening in scope active = ${scope.isActive}")
                    scope.launch {
                        Timber.d("Subscribing to recordings list")
                        repository.recordings.collect {
                            Timber.d("Updating recordings: $it")
                            dispatch(Msg.UpdateList(it))
                        }
                    }
                }
            }
        }
    }

}

