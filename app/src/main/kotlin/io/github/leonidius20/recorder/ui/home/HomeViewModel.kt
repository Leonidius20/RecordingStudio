package io.github.leonidius20.recorder.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.domain.recorder.OutputFileAbstraction
import io.github.leonidius20.recorder.domain.recorder.RecordAudioUseCase
import io.github.leonidius20.recorder.ui.common.secondsToStopwatchString
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recorderServiceLauncher: RecordAudioUseCase,
) : ViewModel() {

    sealed class UiState(
        val isRecPauseBtnVisible: Boolean,
        val recPauseBtnState: RecPauseBtnState,
        val isStopButtonVisible: Boolean,
        val isTimerVisible: Boolean,
        val audioSettingsButtonVisible: Boolean,
    ) {

        enum class RecPauseBtnState {
            RECORD,
            PAUSE,
        }

        data object Idle : UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnState = RecPauseBtnState.RECORD,
            isStopButtonVisible = false,
            isTimerVisible = false,
            audioSettingsButtonVisible = true,
        )

        data class Recording(
            private val isPausingSupported: Boolean,
        ) : UiState(
            isRecPauseBtnVisible = isPausingSupported,
            recPauseBtnState = RecPauseBtnState.PAUSE,
            isStopButtonVisible = true,
            isTimerVisible = true,
            audioSettingsButtonVisible = false,
        )

        data object Paused : UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnState = RecPauseBtnState.RECORD,
            isStopButtonVisible = true,
            isTimerVisible = true,
            audioSettingsButtonVisible = false,
        )

    }

    val uiState: LiveData<UiState> = recorderServiceLauncher.state.map {
        when (it) {
            is RecordAudioUseCase.State.Idle -> UiState.Idle
            is RecordAudioUseCase.State.Recording -> UiState.Recording(isPausingSupported = it.supportsPausing)
            is RecordAudioUseCase.State.Paused -> UiState.Paused
            is RecordAudioUseCase.State.Error -> UiState.Idle // todo: error UI state
            is RecordAudioUseCase.State.Stopping, RecordAudioUseCase.State.Preparing -> UiState.Idle // todo: loading state
        }
    }.asLiveData()

    /**
     * time elapsed since the start of the recording
     */
    val timerText = recorderServiceLauncher.timer
        .distinctUntilChangedBy { millis -> millis / 1000 }
        .map { millis ->
            secondsToStopwatchString(millis / 1000)
        }
        .asLiveData() // todo: put into main state?

    /**
     * for audio visualization
     */
    // todo: SharedFlow with buffer - make it replay last N samples to new
    //  subscriber (i.e. recreated screen)
    val amplitudes = recorderServiceLauncher.amplitudes

    fun onStartRecording() {
        recorderServiceLauncher.start()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onPauseOrResumeRecording() {
        recorderServiceLauncher.toggleRecPause()
    }

    fun onStopRecording() {
        recorderServiceLauncher.stop()
    }

    // todo: no casting
    fun getUri() = (recorderServiceLauncher.file as OutputFileAbstraction).fileUri

}
