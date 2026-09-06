package io.github.leonidius20.recorder.audio_config.data.repository

import io.github.leonidius20.recorder.audio_config.domain.api.AudioConfigReadRepository
import io.github.leonidius20.recorder.audio_config.domain.impl.repository.AudioConfigWriteRepository
import io.github.leonidius20.recorder.audio_config.domain.impl.use_cases.SanitizeSettingsUseCase
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
internal class AudioConfigRepositoryImpl @Inject constructor(
    @param:Scope.App private val appScope: CoroutineScope,
    private val dataSource: AudioSettingsDataSource,
    private val sanitizeSettings: SanitizeSettingsUseCase,
) : AudioConfigReadRepository, AudioConfigWriteRepository {

    override val state = dataSource.settings.map {
        sanitizeSettings(it)
    }.stateIn(appScope, SharingStarted.Eagerly,
        // todo: async?
        sanitizeSettings(dataSource.getCurrentSettingsState())
    )

    override fun setAudioSource(value: Int) {
        sanitizeAndWriteSettings(state.value, audioSource = value)
    }

    override fun setOutputFormat(format: Container) {
        sanitizeAndWriteSettings(state.value,
            outputFormat = format)
    }

    override fun setCodec(codec: Codec<*>) {
        sanitizeAndWriteSettings(
            state.value,
            encoder = codec
        )
    }

    override fun setNumberOfChannels(channels: AudioChannels) {
        sanitizeAndWriteSettings(
            state.value,
            numOfChannels = channels
        )
    }

    override fun setSampleRate(rate: Int) {
        sanitizeAndWriteSettings(
            state.value,
            sampleRate = rate
        )
    }

    override fun setBitDepth(bitDepth: BitDepthOption) {
        require(
            state.value.encoder.resolutionOptions
                    is BitRateSettingType.BitDepthDiscreteValues
        )

        sanitizeAndWriteSettings(
            state.value,
            resolution = Resolution.BitDepth(bitDepth)
        )
    }

    override fun setBitRate(rate: Float) {
        require(state.value.encoder.resolutionOptions
                is BitRateSettingType.BitRateValues)

        sanitizeAndWriteSettings(
            state.value,
            resolution = Resolution.Bitrate(rate)
        )
    }

    private fun sanitizeAndWriteSettings(
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
