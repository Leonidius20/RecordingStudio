package io.github.leonidius20.recorder.ui.home.store

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import io.github.leonidius20.recorder.domain.recorder.OutputFileImpl
import io.github.leonidius20.recorder.domain.recorder.RecordAudioUseCase
import io.github.leonidius20.recorder.domain.recorder.RecordingState
import io.github.leonidius20.recorder.ui.common.secondsToStopwatchString
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Intent
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Label
import io.github.leonidius20.recorder.ui.home.store.HomeStore.State
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

class HomeStoreFactory @Inject constructor(
    private val storeFactory: StoreFactory,
    private val executorProvider: Provider<HomeExecutorImpl>,
    private val recorderServiceLauncher: RecordAudioUseCase,
) {

    sealed interface Action {

        data object SubscribeToRecordingState : Action

    }

    sealed interface Msg {

        data class UpdateRecordingState(
            val newState: RecordingState,
        ) : Msg

    }

    fun create(): HomeStore = object : HomeStore, Store<Intent, State, Label> by storeFactory.create(
        name = "HomeStore",
        initialState = State(),
        executorFactory = { executorProvider.get() },
        bootstrapper = SimpleBootstrapper(Action.SubscribeToRecordingState),
        reducer = { msg ->
            when(msg) {
                is Msg.UpdateRecordingState -> {
                    copy(recordingState = msg.newState)
                }
            }
        }
    ) {

        // todo: this breaks mvi - how can it be changed?
        /**
         * time elapsed since the start of the recording
         */
        override val timerText = recorderServiceLauncher.timer
            .distinctUntilChangedBy { millis -> millis / 1000 }
            .map { millis ->
                secondsToStopwatchString(millis / 1000)
            }

        /**
         * for audio visualization
         */
        override val amplitudes = recorderServiceLauncher.amplitudes

    }

    class HomeExecutorImpl @Inject constructor(
        private val recorderServiceLauncher: RecordAudioUseCase,
    ) : CoroutineExecutor<Intent, Action, State, Msg, Label>() {

        override fun executeIntent(intent: Intent) {
            when(intent) {
                is Intent.ToggleRecPause -> {
                    when(state().recordingState) {
                        is RecordingState.Idle -> {
                            // test permissions before starting recording
                            publish(Label.CheckRecordingPermissions)
                        }
                        is RecordingState.Recording, RecordingState.Paused -> {
                            recorderServiceLauncher.toggleRecPause()
                        }
                        is RecordingState.Error, RecordingState.Stopping, RecordingState.Preparing -> {
                            // ignore
                        }
                    }
                }

                is Intent.NotifyRecordingPermissionsConfirmed -> {
                    recorderServiceLauncher.start()
                }

                is Intent.StopRecording -> {
                    when(state().recordingState) {
                        is RecordingState.Recording, RecordingState.Paused, RecordingState.Preparing -> {
                            scope.launch {
                                recorderServiceLauncher.stop().join()

                                publish(Label.RecreateVisualizer)

                                // todo: no casting
                                val uri = (recorderServiceLauncher.file as OutputFileImpl).fileUri
                                publish(Label.NotifyRecordingDone(uri))
                            }
                        }
                        is RecordingState.Error, RecordingState.Stopping, RecordingState.Idle -> {
                            // ignore
                        }
                     }
                }

                is Intent.OpenAudioSettings -> {
                    publish(Label.OpenAudioSettings)
                }
            }
        }

        override fun executeAction(action: Action) {
            when(action) {
                is Action.SubscribeToRecordingState -> {
                    scope.launch {
                        recorderServiceLauncher.state.collect {
                            dispatch(Msg.UpdateRecordingState(it))
                        }
                    }
                }
            }
        }

    }

}
