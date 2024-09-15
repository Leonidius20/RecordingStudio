package io.github.leonidius20.recorder.data.settings

import androidx.annotation.StringRes
import io.github.leonidius20.recorder.R

enum class AudioChannels(
    val value: Int,
    @StringRes val title: Int,
) {
    MONO(1, R.string.audio_settings_channels_value_mono),
    STEREO(2, R.string.audio_settings_channels_value_stereo);

    fun toInt() = value

    companion object {
        fun fromInt(value: Int) = entries.first { it.value == value }
    }

}