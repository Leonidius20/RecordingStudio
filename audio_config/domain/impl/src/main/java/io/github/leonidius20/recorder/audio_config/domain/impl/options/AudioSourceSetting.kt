package io.github.leonidius20.recorder.audio_config.domain.impl.options

import io.github.leonidius20.recorder.audio_config.domain.impl.AudioSourceOption
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container

data class AudioSourceSetting(
    override val option: AudioSourceOption,
    override val isSelected: Boolean,
) : DiscreteAudioSetting<AudioSourceOption> {

    override val id: Int
        get() = option.value

    override val displayName: String
        get() = option.name

}

data class ContainerSetting(
    override val option: Container,
    override val isSelected: Boolean,
) : DiscreteAudioSetting<Container> {

    override val id: Int
        get() = option.value

    override val displayName: String
        get() = option.displayName

}

data class CodecSetting(
    override val option: Codec<*>,
    override val isSelected: Boolean,
) : DiscreteAudioSetting<Codec<*>> {

    override val id: Int
        get() = option.value

    override val displayName: String
        get() = option.displayName

}

data class ChannelsSetting(
    override val option: AudioChannels,
    override val isSelected: Boolean
) : DiscreteAudioSetting<AudioChannels> {

    override val id: Int
        get() = option.value

    override val displayName: String
        get() = option.name // todo: replace with display name, but need context for that

}

data class SampleRateSetting(
    val rate: Int,
    val isSelected: Boolean,
)

data class BitDepthSetting(
    val depth: BitDepthOption,
    val isSelected: Boolean,
)

data class BitRateSettings(
    val type: BitRateSettingType.BitRateValues,
    val current: Float,
)

data class AudioConfigSettings(
    val audioSources: List<AudioSourceSetting> = emptyList(),
    val audioSource: AudioSourceOption? = null,
    val containers: List<ContainerSetting> = emptyList(),
    val codecs: List<CodecSetting> = emptyList(),
    val channelOptions: List<ChannelsSetting> = emptyList(),
    val sampleRates: List<SampleRateSetting> = emptyList(),
    val bitRateSettings: BitRateSettings? = null,
    val bitDepths: List<BitDepthSetting>? = null,
)
