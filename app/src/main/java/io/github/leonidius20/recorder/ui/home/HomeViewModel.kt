package io.github.leonidius20.recorder.ui.home

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.recorder.RecorderServiceLauncher
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recorderServiceLauncher: RecorderServiceLauncher,
) : ViewModel() {

    sealed class UiState(
        val isRecPauseBtnVisible: Boolean,
        val recPauseBtnIcon: RecPauseBtnIcon,
        val isStopButtonVisible: Boolean,
        val isTimerVisible: Boolean,
    ) {

        enum class RecPauseBtnIcon {
            RECORD,
            PAUSE,
        }

        data object Idle: UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnIcon = RecPauseBtnIcon.RECORD,
            isStopButtonVisible = false,
            isTimerVisible = false,
        )

        data object Recording: UiState(
            isRecPauseBtnVisible =
            // pausing MediaRecorder is only available in Nougat
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            recPauseBtnIcon = RecPauseBtnIcon.PAUSE,
            isStopButtonVisible = true,
            isTimerVisible = true,
        )

        data object Paused: UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnIcon = RecPauseBtnIcon.RECORD,
            isStopButtonVisible = true,
            isTimerVisible = true,
        )

    }

    val uiState: LiveData<UiState> = recorderServiceLauncher.state.map {
        when (it) {
            RecorderServiceLauncher.State.IDLE -> UiState.Idle
            RecorderServiceLauncher.State.RECORDING -> UiState.Recording
            RecorderServiceLauncher.State.PAUSED -> UiState.Paused
            RecorderServiceLauncher.State.ERROR -> UiState.Idle // todo: error UI state
        }
    }.asLiveData()

    /**
     * time elapsed since the start of the recording
     */
    val timerText = recorderServiceLauncher.timer.map { milliseconds ->
        millisecondsToStopwatchString(milliseconds)
    }.asLiveData()

    /**
     * for audio visualization
     */
    val amplitudes = recorderServiceLauncher.amplitudes

    fun onStartRecording() {
        recorderServiceLauncher.launchRecording()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun onPauseOrResumeRecording() {
        recorderServiceLauncher.toggleRecPause()
    }

    fun onStopRecording() {
        recorderServiceLauncher.stopRecording()
    }

    fun getUri() = recorderServiceLauncher.getUri()

}