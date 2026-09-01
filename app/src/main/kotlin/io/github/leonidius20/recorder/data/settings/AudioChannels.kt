package io.github.leonidius20.recorder.data.settings

import androidx.annotation.StringRes
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels

@StringRes
fun AudioChannels.displayName(): Int = when(this) {
    AudioChannels.MONO -> R.string.audio_settings_channels_value_mono
    AudioChannels.STEREO -> R.string.audio_settings_channels_value_stereo
}
