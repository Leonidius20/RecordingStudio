package io.github.leonidius20.recorder.ui.home

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.leonidius20.recorder.data.recorder.RecorderServiceLauncher
import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.BitDepthOption
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
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
            RecorderServiceLauncher.State.IDLE -> UiState.Idle
            RecorderServiceLauncher.State.RECORDING -> UiState.Recording(isPausingSupported = recorderServiceLauncher.isPausingSupported)
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

    val outputFormats = Container.supportedContainers()

    val selectedContainer = settings.state.map {
        it.outputFormat
    }.asLiveData(viewModelScope.coroutineContext)

    fun isChecked(container: Container) =
        settings.state.value.outputFormat.value == container.value

    fun selectOutputFormat(container: Container) {
        settings.setOutputFormat(container)
    }

    fun isChecked(audioSource: Settings.AudioSourceOption) =
        audioSource.value == settings.state.value.audioSource

    val encoderOptions = selectedContainer
        .map { it.availableCodecs }
        .distinctUntilChanged()


    fun isEncoderChecked(encoder: Codec) =
        settings.state.value.encoder.value == encoder.value

    fun setEncoder(encoder: Codec) {
        settings.setCodec(encoder)
    }

    val audioChannelsOptions = AudioChannels.entries

    // todo: one state for all settings, mvi
    fun isChannelsOptionsChecked(channels: AudioChannels) =
        channels == settings.state.value.numOfChannels

    fun setChannels(channels: AudioChannels) {
        settings.setNumberOfChannels(channels)
    }

    val supportedSampleRates = settings.state.map {
        it.encoder.supportedSampleRates
            .intersect(settings.sampleRatesSupportedByDevice)
            .sorted()
    }.asLiveData(viewModelScope.coroutineContext)

    fun setSampleRate(rate: Int) {
        settings.setSampleRate(rate)
    }

    val currentSampleRate
        get() = settings.state.value.sampleRate

    val availableBitDepths = settings.state.map {
        if (it.encoder.supportsSettingBitDepth) {
            it.encoder.bitDepthOptions
        } else emptyArray()
    }.asLiveData(viewModelScope.coroutineContext)

    val currentBitDepth
        get() = with(settings.state.value) {
            if (encoder.supportsSettingBitDepth) {
                bitDepthsForCodecs[encoder]
            } else null
        }

    fun setBitDepth(bitDepthOption: BitDepthOption) {
        settings.setBitDepth(bitDepthOption)
    }

    @Deprecated("remove for 0.2.0") // todo
    val isCurrentEncoderPcm =
        settings.state
            .map { it.encoder == Codec.PCM }
            .asLiveData(viewModelScope.coroutineContext)

}