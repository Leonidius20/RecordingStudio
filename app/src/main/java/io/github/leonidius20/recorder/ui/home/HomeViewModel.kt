package io.github.leonidius20.recorder.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.recorder.RecorderServiceLauncher
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.ui.common.millisecondsToStopwatchString
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val recorderServiceLauncher: RecorderServiceLauncher,
    private val settings: Settings,
) : ViewModel() {

    sealed class UiState(
        val isRecPauseBtnVisible: Boolean,
        val recPauseBtnIcon: RecPauseBtnIcon,
        val isStopButtonVisible: Boolean,
        val isTimerVisible: Boolean,
        val audioSettingsButtonVisible: Boolean,
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
            audioSettingsButtonVisible = true,
        )

        data object Recording: UiState(
            isRecPauseBtnVisible =
            // pausing MediaRecorder is only available in Nougat
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N,
            recPauseBtnIcon = RecPauseBtnIcon.PAUSE,
            isStopButtonVisible = true,
            isTimerVisible = true,
            audioSettingsButtonVisible = false,
        )

        data object Paused: UiState(
            isRecPauseBtnVisible = true,
            recPauseBtnIcon = RecPauseBtnIcon.RECORD,
            isStopButtonVisible = true,
            isTimerVisible = true,
            audioSettingsButtonVisible = false,
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

    val audioSources = settings.audioSourceOptions

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

    fun selectAudioSource(value: Int) {
        settings.setAudioSource(value)
    }

    val outputFormats = settings.outputFormatOptions.values

    fun isChecked(format: Settings.OutputFormatOption) =
        settings.state.value.outputFormat.value == format.value

    fun selectOutputFormat(value: Int) {
        settings.setOutputFormat(value)
    }

    fun isChecked(audioSource: Settings.AudioSourceOption) =
        audioSource.value == settings.state.value.audioSource

    val encoderOptions = settings.encoderOptions

    fun isEncoderChecked(encoder: Int) =
        settings.state.value.encoder == encoder

    fun setEncoder(encoder: Int) {
        settings.setCodec(encoder)
    }

}