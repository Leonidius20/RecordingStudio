package io.github.leonidius20.domain.audio_settings

import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.domain.settings.AudioConfigReadRepository
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

// todo: move to own module??
@Singleton
class AudioConfigRepositoryImpl @Inject constructor(
    @param:Scope.App private val appScope: CoroutineScope,
    private val dataSource: AudioSettingsDataSource,
    private val deviceAudioCapabilities: DeviceAudioCapabilities,
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

    /**
     * returns not the highest and not the lowest sample rate supported by codec
     * and device. Some middle value. The reason for this is that the highest sample
     * rate sounds bad with the default bitrate, and we have not implemented changing
     * the latter yet.
     */
    private fun medianSampleRateSupportedByCodecAndDevice(codec: Codec<*>): Int {
        val rates = codec.supportedSampleRates.intersect(
            deviceAudioCapabilities.sampleRatesSupportedByDevice
        ).toIntArray()

        val middleIndex = rates.size / 2

        return rates[middleIndex]
    }

    // todo: one function to update (write updated) the settings.
    //  one function to sanitize

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

    // todo: can we resuse this same function for
    // reading data?? in data source we will simply read
    //  current state from pref, then pass though this function
    //  (using default SettingsState() as base)
    private fun sanitizeSettings(
        settings: SettingsState<*>,
        audioSource: Int = settings.audioSource,
        outputFormat: Container = settings.outputFormat,
        encoder: Codec<*> = settings.encoder,
        numOfChannels: AudioChannels = settings.numOfChannels,
        sampleRate: Int = settings.sampleRate,
        resolution: Resolution<*> = settings.resolution,
    ): SettingsState<*> {
        val encoder = if (!outputFormat.supports(encoder, deviceAudioCapabilities)) {
            outputFormat.defaultCodec(deviceAudioCapabilities)
        } else encoder

        var sampleRate = if (!encoder.supportsSampleRate(sampleRate)) {
            encoder.supportedSampleRateClosestTo(sampleRate)
        } else sampleRate

        if (!deviceAudioCapabilities.sampleRatesSupportedByDevice.contains(sampleRate)) {
            sampleRate = medianSampleRateSupportedByCodecAndDevice(encoder)
        }

        return when(encoder.resolutionOptions) {
            is BitRateSettingType.None -> {
                SettingsState(
                    audioSource = audioSource,
                    outputFormat = outputFormat,
                    encoder = encoder as Codec<BitRateSettingType.None>,
                    numOfChannels = numOfChannels,
                    sampleRate = sampleRate,
                    resolution = Resolution.None
                )
            }
            is BitRateSettingType.BitRateValues -> {
                SettingsState(
                    audioSource = audioSource,
                    outputFormat = outputFormat,
                    encoder = encoder as Codec<BitRateSettingType.BitRateValues>,
                    numOfChannels = numOfChannels,
                    sampleRate = sampleRate,
                    resolution = Resolution.Bitrate(
                        if (resolution is Resolution.Bitrate) {
                            val prev = resolution.value

                            if (!encoder.supportsBitrate(prev)) {
                                encoder.supportedBitRateClosestTo(prev)
                            } else prev
                        } else encoder.resolutionOptions.default
                    )
                )
            }
            is BitRateSettingType.BitDepthDiscreteValues -> {
                SettingsState(
                    audioSource = audioSource,
                    outputFormat = outputFormat,
                    encoder = encoder as Codec<BitRateSettingType.BitDepthDiscreteValues>,
                    numOfChannels = numOfChannels,
                    sampleRate = sampleRate,
                    resolution = Resolution.BitDepth(
                        if (resolution is Resolution.BitDepth) resolution.value
                        else encoder.resolutionOptions.default
                    )
                )
            }
        }
    }

}
