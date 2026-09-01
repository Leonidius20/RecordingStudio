package io.github.leonidius20.recorder.entities.audio_settings

// todo: separate audio config and user settings into different classes
data class SettingsState(
    val stopOnLowBattery: Boolean,
    val stopOnLowStorage: Boolean,
    val pauseOnCall: Boolean,
    val audioSource: Int,
    val outputFormat: Container,
    val encoder: Codec,
    val numOfChannels: AudioChannels,
    val sampleRate: Int,

    // todo: have an AudioConfig class with some sealed heirarchy that
    //  does away with the nullable things like that
    val bitDepth: BitDepthOption?,
    val bitRate: Float?,

    // todo: maybe we instead should make Codec a proper class with subsclasses,
    // where each instance is a codec with certain parameters set up (sample rate, bit rate)
    // and the class itself will be checking if these parameters work together?
)
