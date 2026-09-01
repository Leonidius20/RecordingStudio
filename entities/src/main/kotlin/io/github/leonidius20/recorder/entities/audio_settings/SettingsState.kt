package io.github.leonidius20.recorder.entities.audio_settings

// todo: separate audio config and user settings into different classes
data class SettingsState<T: BitRateSettingType>(
    val stopOnLowBattery: Boolean,
    val stopOnLowStorage: Boolean,
    val pauseOnCall: Boolean,
    val audioSource: Int,
    val outputFormat: Container,
    val encoder: Codec<T>,
    val numOfChannels: AudioChannels,
    val sampleRate: Int,

    // todo: have an AudioConfig class with some sealed heirarchy that
    //  does away with the nullable things like that
    val resolution: Resolution<T>,

    // todo: maybe we instead should make Codec a proper class with subsclasses,
    // where each instance is a codec with certain parameters set up (sample rate, bit rate)
    // and the class itself will be checking if these parameters work together?
)

sealed interface Resolution<T: BitRateSettingType> {

    data object None : Resolution<BitRateSettingType.None>

    data class Bitrate(
        val value: Float,
    ) : Resolution<BitRateSettingType.BitRateValues>

    data class BitDepth(
        val value: BitDepthOption,
    ) : Resolution<BitRateSettingType.BitDepthDiscreteValues>

}

