package io.github.leonidius20.recorder.audio_config.data.repository

import io.github.leonidius20.recorder.audio_config.domain.api.AudioConfigReadRepository
import io.github.leonidius20.recorder.audio_config.domain.impl.SanitizeSettingsUseCase
import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioConfigRepositoryImpl @Inject constructor(
    @param:Scope.App private val appScope: CoroutineScope,
    private val dataSource: AudioSettingsDataSource,
    private val sanitizeSettings: SanitizeSettingsUseCase,
) : AudioConfigReadRepository {

    override val state = dataSource.settings.map {
        sanitizeSettings(it)
    }.stateIn(appScope, SharingStarted.Eagerly,
        // todo: async?
        sanitizeSettings(dataSource.getCurrentSettingsState())
    )

    fun setAudioSource(value: Int) {
        sanitizeAndWriteSettings(state.value, audioSource = value)
    }

    fun setOutputFormat(format: Container) {
        sanitizeAndWriteSettings(state.value,
            outputFormat = format)
    }

    fun setCodec(codec: Codec<*>) {
        sanitizeAndWriteSettings(
            state.value,
            encoder = codec
        )
    }

    fun setNumberOfChannels(channels: AudioChannels) {
        sanitizeAndWriteSettings(
            state.value,
            numOfChannels = channels
        )
    }

    fun setSampleRate(rate: Int) {
        sanitizeAndWriteSettings(
            state.value,
            sampleRate = rate
        )
    }

    fun setBitDepth(bitDepth: BitDepthOption) {
        require(
            state.value.encoder.resolutionOptions
                    is BitRateSettingType.BitDepthDiscreteValues
        )

        sanitizeAndWriteSettings(
            state.value,
            resolution = Resolution.BitDepth(bitDepth)
        )
    }

    fun setBitRate(rate: Float) {
        require(state.value.encoder.resolutionOptions
                is BitRateSettingType.BitRateValues)

        sanitizeAndWriteSettings(
            state.value,
            resolution = Resolution.Bitrate(rate)
        )
    }

    fun sanitizeAndWriteSettings(
        settings: SettingsState<*>,
        audioSource: Int = settings.audioSource,
        outputFormat: Container = settings.outputFormat,
        encoder: Codec<*> = settings.encoder,
        numOfChannels: AudioChannels = settings.numOfChannels,
        sampleRate: Int = settings.sampleRate,
        resolution: Resolution<*> = settings.resolution,
    ) {
        dataSource.saveSettingsToDisk(
            sanitizeSettings(
                settings, audioSource, outputFormat, encoder, numOfChannels, sampleRate, resolution
            )
        )
    }

}
