package io.github.leonidius20.recorder.ui.home.view

import com.arkivanov.mvikotlin.core.view.MviView
import io.github.leonidius20.recorder.domain.recorder.RecordingState
import io.github.leonidius20.recorder.ui.home.HomeViewModel.UiState.RecPauseBtnState
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Intent
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Label
import io.github.leonidius20.recorder.ui.home.store.HomeStore.State
import io.github.leonidius20.recorder.ui.home.view.HomeView.Model

interface HomeView : MviView<Model, Intent> {

    data class Model(
        val isRecPauseBtnVisible: Boolean,
        val recPauseBtnState: RecPauseBtnState,
        val isStopButtonVisible: Boolean,
        val isTimerVisible: Boolean,
        val audioSettingsButtonVisible: Boolean,
        val loadingIndicatorVisible: Boolean,
    )

    fun handleLabel(label: Label)

    fun handleTimerTick(text: String)

    fun handleAmplitudeUpdate(amp: Int)

}

internal val stateToModel: State.() -> Model = {
    when (this.recordingState) {
        is RecordingState.Idle, RecordingState.Error -> Model(
            isRecPauseBtnVisible = true,
            recPauseBtnState = RecPauseBtnState.RECORD,
            isStopButtonVisible = false,
            isTimerVisible = false,
            audioSettingsButtonVisible = true,
            loadingIndicatorVisible = false,
        )

        is RecordingState.Recording -> Model(
            isRecPauseBtnVisible = this.recordingState.supportsPausing,
            recPauseBtnState = RecPauseBtnState.PAUSE,
            isStopButtonVisible = true,
            isTimerVisible = true,
            audioSettingsButtonVisible = false,
            loadingIndicatorVisible = false,
        )

        // todo: better ui for error - or maybe do a label with toast / snakbar
        is RecordingState.Stopping, RecordingState.Preparing -> Model(
            isRecPauseBtnVisible = false,
            recPauseBtnState = RecPauseBtnState.PAUSE,
            isStopButtonVisible = false,
            isTimerVisible = false,
            audioSettingsButtonVisible = false,
            loadingIndicatorVisible = true,
        )

        is RecordingState.Paused -> Model(
            isRecPauseBtnVisible = true,
            recPauseBtnState = RecPauseBtnState.RECORD,
            isStopButtonVisible = true,
            isTimerVisible = true,
            audioSettingsButtonVisible = false,
            loadingIndicatorVisible = false,
        )
    }
}
