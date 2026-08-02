package io.github.leonidius20.recorder.ui.recordings_list.viewmodel

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.domain.recordings_list.Recording
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.Intent
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.State
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Provider

interface RecordingsListStore : Store<Intent, State, Label> {

    sealed interface Intent {

        data object ClearSelection : Intent

        // if none selected (and selection mode is off)
        // turn it on and select. else if this is last selected
        // turn selector mode off, else just deselect
        data class ToggleSelection(
            val id: Long
        ) : Intent

        // if selection mode is off, play recording, else toggle selection
        data class PlayOrToggleSelection(
            val id: Long,
        ) : Intent

    }

    data class State(
        // todo: lift out
        val recordings: List<Recording> = emptyList(),
        val selectedItems: Set<Long> = emptySet(), // use LongSet or SparseBoolArray??
    ) {

        val inSelectionMode get() = selectedItems.isNotEmpty()

    }

}



sealed interface Label {

    // todo: can be removed once we migrate to compose
    /*data object EnableSelectionMode : Label

    data object DisableSelectionMode : Label

    data object SwitchToMultipleSelectionMenu : Label

    data object SwitchToSingleSelectionMenu : Label*/

}

class RecordingsListStoreFactory @Inject constructor(
    //private val storeFactory: StoreFactory,
    private val executorProvider: Provider<ExecutorImpl>,
    // todo: maybe inject provider<Executor> and then @Bind ExecutorImpl
) {

    // todo inject
    private val storeFactory: StoreFactory = DefaultStoreFactory()

    fun create(): RecordingsListStore = object : RecordingsListStore, Store<Intent, State, Label> by storeFactory.create(
        name = "RecordingsListStore",
        initialState = State(),
        bootstrapper = SimpleBootstrapper(Action.SubscribeToRecordingsList),
        executorFactory = {
            executorProvider.get()
        },
        reducer = { msg: Msg ->
            when(msg) {
                is Msg.ListUpdated -> copy(recordings = msg.newList) // todo: arrow-kt
                is Msg.ItemSelected -> copy(
                    selectedItems = selectedItems + msg.id
                )
                is Msg.ItemDeselected -> copy(
                    selectedItems = selectedItems - msg.id
                )
                is Msg.SelectionCleared -> copy(
                    selectedItems = emptySet()
                )
            }
        }
    ) {}

    sealed interface Action {
        data object SubscribeToRecordingsList : Action

        data class ToggleSelection(val id: Long) : Action
    }

    sealed interface Msg {

        data class ListUpdated(
            val newList: List<Recording>
        ): Msg

        data class ItemSelected(
            val id: Long,
        ) : Msg

        data class ItemDeselected(
            val id: Long,
        ) : Msg

        data object SelectionCleared : Msg

    }

    class ExecutorImpl @Inject constructor(
        private val repository: RecordingsListRepository,
    ) : CoroutineExecutor<Intent, Action, State, Msg, Label>() {

        init {
            Timber.d("created executor")

        }

        override fun executeIntent(intent: Intent) {
            when(intent) {
                is Intent.ClearSelection -> {
                    dispatch(Msg.SelectionCleared)
                }
                is Intent.PlayOrToggleSelection -> {
                    if (state().inSelectionMode) {
                        executeAction(Action.ToggleSelection(intent.id))
                    } else {
                        // todo: set currently played, side-effect to call player
                    }
                }
                is Intent.ToggleSelection -> {
                    executeAction(Action.ToggleSelection(intent.id))
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
                            dispatch(Msg.ListUpdated(it))
                        }
                    }
                }

                is Action.ToggleSelection -> {
                    //val selectionModeBefore = state().inSelectionMode
                    //val numSelectedBefore = state().selectedItems.size

                    val wasSelected = state().selectedItems.contains(action.id)
                    val newValue = !wasSelected

                    dispatch(
                        if (newValue) Msg.ItemSelected(action.id)
                        else Msg.ItemDeselected(action.id)
                    )

                    /*val selectionModeAfter = if (selectionModeBefore) {
                        !(numSelectedBefore == 1 && !newValue)
                    } else if (newValue) true else false*/

                    /*if (selectionModeBefore != selectionModeAfter) {
                        publish(
                            if (selectionModeAfter)
                                Label.EnableSelectionMode else Label.DisableSelectionMode
                        )
                    }*/

                    // todo: update count and switch menu
                }
            }
        }
    }

}

