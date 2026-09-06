package io.github.leonidius20.recorder.audio_config.domain.impl.use_cases

import io.github.leonidius20.recorder.audio_config.domain.impl.DeviceAudioCapabilities
import io.github.leonidius20.recorder.audio_config.domain.impl.defaultCodec
import io.github.leonidius20.recorder.audio_config.domain.impl.supportedBitRateClosestTo
import io.github.leonidius20.recorder.audio_config.domain.impl.supportedSampleRateClosestTo
import io.github.leonidius20.recorder.audio_config.domain.impl.supports
import io.github.leonidius20.recorder.audio_config.domain.impl.supportsBitrate
import io.github.leonidius20.recorder.audio_config.domain.impl.supportsSampleRate
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import javax.inject.Inject

// todo: Unit test
class SanitizeSettingsUseCase @Inject constructor(
    private val deviceAudioCapabilities: DeviceAudioCapabilities,
) {

    operator fun invoke(
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

        return when (encoder.resolutionOptions) {
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

}
