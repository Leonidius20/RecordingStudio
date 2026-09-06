package io.github.leonidius20.recorder.audio_config.domain.impl

import io.github.leonidius20.recorder.audio_config.domain.api.AudioConfigReadRepository
import io.github.leonidius20.recorder.audio_config.domain.impl.options.AudioConfigSettings
import io.github.leonidius20.recorder.audio_config.domain.impl.options.AudioSourceSetting
import io.github.leonidius20.recorder.audio_config.domain.impl.options.BitDepthSetting
import io.github.leonidius20.recorder.audio_config.domain.impl.options.BitRateSettings
import io.github.leonidius20.recorder.audio_config.domain.impl.options.ChannelsSetting
import io.github.leonidius20.recorder.audio_config.domain.impl.options.CodecSetting
import io.github.leonidius20.recorder.audio_config.domain.impl.options.ContainerSetting
import io.github.leonidius20.recorder.audio_config.domain.impl.options.SampleRateSetting
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Generate the settings that are available to the user to toggle
 * given currently selected container and codec, as well as device
 * capabilities
 */
class GetAvailableSettingsUseCase @Inject constructor(
    audioConfigReadRepository: AudioConfigReadRepository,
    capabilities: DeviceAudioCapabilities,
) {

    val settings = audioConfigReadRepository.state.map { newSettings ->
        val container = newSettings.outputFormat
        val codec = newSettings.encoder

        // todo: redo without type comparisons
        val availableBitDepths = run {
            val bitRateSetting = codec.resolutionOptions
            if (bitRateSetting is BitRateSettingType.BitDepthDiscreteValues) {
                bitRateSetting.availableOptions
            } else null
        }


        val supportedSampleRates = run {
            codec.supportedSampleRates
                // todo: move this logic out of UI; remove dependency on
                //  capabilities; also - redesign whole audio  settings api
                .intersect(capabilities.sampleRatesSupportedByDevice)
                .sorted()
        }

        val bitRateSettingType = codec.resolutionOptions

        val audioSources = capabilities.audioSourceOptions.map {
            AudioSourceSetting(
                option = it,
                // todo: have settings expose enum value and not int?
                isSelected = it.value == newSettings.audioSource
            )
        }

        AudioConfigSettings(
            audioSources = audioSources,
            audioSource = capabilities.audioSourceOptions.find {
                it.value == newSettings.audioSource
            }!!, //?: defaultAudioSource, // todo: move this logic to Settings

            containers = Container.supportedContainers(capabilities).map {
                ContainerSetting(
                    option = it,
                    isSelected = newSettings.outputFormat == it,
                )
            },

            codecs = container.availableCodecs(capabilities).map {
                CodecSetting(
                    option = it,
                    isSelected = it == codec,
                )
            },

            channelOptions = AudioChannels.entries.map {
                ChannelsSetting(
                    option = it,
                    isSelected = newSettings.numOfChannels == it
                )
            }, // todo: do all phones support stereo?

            sampleRates = supportedSampleRates.map {
                SampleRateSetting(
                    rate = it,
                    isSelected = it == newSettings.sampleRate,
                )
            },

            bitRateSettings = when (bitRateSettingType) {
                is BitRateSettingType.BitDepthDiscreteValues,
                BitRateSettingType.None -> null

                is BitRateSettingType.BitRateValues -> {
                    (newSettings.resolution as? Resolution.Bitrate)?.let {
                        BitRateSettings(
                            type = bitRateSettingType,
                            current = it.value
                        )
                    }
                }
            },

            bitDepths = availableBitDepths?.map {
                BitDepthSetting(
                    depth = it,
                    isSelected = it == (newSettings.resolution as? Resolution.BitDepth)?.value
                )
            },
        )
    }

}
