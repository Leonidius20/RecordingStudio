package io.github.leonidius20.recorder.ui.recordings_list.viewmodel

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.domain.recordings_list.Recording
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.Intent
import io.github.leonidius20.recorder.ui.recordings_list.viewmodel.RecordingsListStore.State
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

        data object ConnectPlayer : Intent

        data object DisconnectPlayer : Intent

        data class OnPlayingRecordingChanged(
            val id: Long
        ) : Intent

        data object OnRecordingsPlaybackFinished : Intent

        data object TrashSelected : Intent

        data object DeleteSelected : Intent

        data object RenameSelected : Intent

        data object ShareSelected : Intent

        data object EditSelected : Intent

        data object NotifyFileDeletionFailed : Intent

    }

    data class State(
        val recordings: List<Recording> = emptyList(),
        val selectedItems: Set<Long> = emptySet(), // use LongSet or SparseBoolArray??
        val currentlyPlaying: Long? = null,
        val playerConnected: Boolean = false,
        val isLoading: Boolean = true,
    ) {

        val inSelectionMode get() = selectedItems.isNotEmpty()

    }

}

sealed interface Label {

    data class UpdatePlayerItems(val recordings: List<Recording>) : Label

    data class Play(val position: Int) : Label

    data class Trash(val recs: List<Recording>) : Label

    data class Delete(val recs: List<Recording>) : Label

    data class Rename(val rec: Recording) : Label

    data class Share(val recs: List<Recording>) : Label

    data class Edit(val rec: Recording) : Label

    sealed interface ShowMessage : Label {

        data object FileDeletionFailed : ShowMessage

    }

}

class RecordingsListStoreFactory @Inject constructor(
    private val storeFactory: StoreFactory,
    private val executorProvider: Provider<ExecutorImpl>,
    // todo: maybe inject provider<Executor> and then @Bind ExecutorImpl
) {

    fun create(): RecordingsListStore = object : RecordingsListStore, Store<Intent, State, Label> by storeFactory.create(
        name = "RecordingsListStore",
        initialState = State(),
        bootstrapper = SimpleBootstrapper(Action.SubscribeToRecordingsList),
        executorFactory = {
            executorProvider.get()
        },
        reducer = { msg: Msg ->
            Timber.d("Got message $msg")
            when(msg) {
                is Msg.ListUpdated -> copy(
                    recordings = msg.newList,
                    isLoading = false,
                ) // todo: arrow-kt
                is Msg.ItemSelected -> copy(
                    selectedItems = selectedItems + msg.id
                )
                is Msg.ItemDeselected -> copy(
                    selectedItems = selectedItems - msg.id
                )
                is Msg.SelectionCleared -> copy(
                    selectedItems = emptySet()
                )
                is Msg.PlayerConnected -> copy(playerConnected = true)
                is Msg.PlayerDisconnected -> copy(playerConnected = false)
                is Msg.NowPlaying -> copy(
                    currentlyPlaying = msg.id
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

        data object PlayerConnected : Msg

        data object PlayerDisconnected : Msg

        data class NowPlaying(val id: Long?) : Msg

    }

    class ExecutorImpl @Inject constructor(
        private val repository: RecordingsListRepository,
    ) : CoroutineExecutor<Intent, Action, State, Msg, Label>() {

        override fun executeIntent(intent: Intent) {
            when(intent) {
                is Intent.ClearSelection -> {
                    dispatch(Msg.SelectionCleared)
                }
                is Intent.PlayOrToggleSelection -> {
                    if (state().inSelectionMode) {
                        executeAction(Action.ToggleSelection(intent.id))
                    } else {
                        dispatch(Msg.NowPlaying(intent.id))
                        // todo: more optimal lookup?
                        val index = state().recordings
                            .indexOfFirst { it.id == intent.id }
                        publish(Label.Play(index))
                    }
                }
                is Intent.ToggleSelection -> {
                    executeAction(Action.ToggleSelection(intent.id))
                }
                is Intent.ConnectPlayer -> {
                    dispatch(Msg.PlayerConnected)
                    Timber.d("Player was connected")
                    publish(Label.UpdatePlayerItems(state().recordings))
                }
                is Intent.DisconnectPlayer -> {
                    Timber.d("Player was disconnected")
                    dispatch(Msg.PlayerDisconnected)
                }
                is Intent.OnPlayingRecordingChanged -> {
                    dispatch(Msg.NowPlaying(intent.id))
                }
                is Intent.OnRecordingsPlaybackFinished -> {
                    dispatch(Msg.NowPlaying(null))
                }
                is Intent.TrashSelected -> {
                    publish(Label.Trash(currentlySelectedRecordings()))
                }
                is Intent.DeleteSelected -> {
                    publish(Label.Delete(currentlySelectedRecordings()))
                }
                is Intent.RenameSelected -> {
                    publish(Label.Rename(singleSelectedRecording()))
                }
                is Intent.ShareSelected -> {
                    publish(Label.Share(currentlySelectedRecordings()))
                }
                is Intent.NotifyFileDeletionFailed -> {
                    publish(Label.ShowMessage.FileDeletionFailed)
                }
                is Intent.EditSelected -> {
                    publish(Label.Edit(singleSelectedRecording()))
                }
            }
        }

        private fun currentlySelectedRecordings() = state().run {
            recordings.filter { selectedItems.contains(it.id) }
        }

        private fun singleSelectedRecording() = state().run {
            recordings.first { selectedItems.contains(it.id) }
        }

        override fun executeAction(action: Action) {
            when(action) {
                is Action.SubscribeToRecordingsList -> {
                    scope.launch {
                        repository.recordings.collect {
                            dispatch(Msg.ListUpdated(it))

                            val playerConnected = state().playerConnected
                            Timber.d("Updated rec list. Player connected = $playerConnected")
                            if (playerConnected) {
                                publish(Label.UpdatePlayerItems(it))
                            }
                        }
                    }
                }

                is Action.ToggleSelection -> {
                    val wasSelected = state().selectedItems.contains(action.id)
                    val newValue = !wasSelected

                    dispatch(
                        if (newValue) Msg.ItemSelected(action.id)
                        else Msg.ItemDeselected(action.id)
                    )
                }
            }
        }
    }

}
