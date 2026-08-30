package io.github.leonidius20.recorder.entities.audio_settings

enum class AudioChannels(
    val value: Int,
) {
    MONO(1),
    STEREO(2);

    fun numberOfChannels() = value

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }

}
