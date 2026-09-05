package io.github.leonidius20.recorder.data.settings

import android.media.AudioFormat
import io.github.leonidius20.recorder.domain.audio_settings.PcmBitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption

val PcmBitDepthOption.valueForAudioRecordApi get() = when(this) {
    PcmBitDepthOption.PCM_16BIT_INT -> AudioFormat.ENCODING_PCM_16BIT
    PcmBitDepthOption.PCM_BIT_FLOAT -> AudioFormat.ENCODING_PCM_FLOAT
    //PCM_8BIT_INT -> AudioFormat.ENCODING_PCM_8BIT
}

val PcmBitDepthOption.valueForPref get() = valueForAudioRecordApi

val BitDepthOption.valueForPref get() = when(this) {
    is PcmBitDepthOption -> this.valueForPref
    else -> throw IllegalArgumentException()
}
