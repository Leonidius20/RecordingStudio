package io.github.leonidius20.recorder.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.recorder.RecorderServiceLauncher
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recorderServiceLauncher: RecorderServiceLauncher,
) : ViewModel() {

    sealed class UiState(
        val isRecPauseBtnVisible: Boolean,
        val recPauseBtnIcon: RecPauseBtnIcon,
        val isStopButtonVisible: Boolean,
    ) {

        enum class RecPauseBtnIcon {
            RECORD,
            PAUSE,
        }

        data object Idle: UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnIcon = RecPauseBtnIcon.RECORD,
            isStopButtonVisible = false,
        )

        data object Recording: UiState(
            isRecPauseBtnVisible =
            // pausing MediaRecorder is only available in Nougat
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            recPauseBtnIcon = RecPauseBtnIcon.PAUSE,
            isStopButtonVisible = true,
        )

        data object Paused: UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnIcon = RecPauseBtnIcon.RECORD,
            isStopButtonVisible = true,
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


}