package io.github.leonidius20.recorder.data.settings

import android.media.AudioFormat

enum class PcmBitDepthOption(
    val value: Int,
    override val displayName: String,
) : BitDepthOption {

    PCM_8BIT_INT(
        displayName = "8 bit int",
        value = AudioFormat.ENCODING_PCM_8BIT,
    ),

    PCM_16BIT_INT(
        displayName = "16 bit int",
        value = AudioFormat.ENCODING_PCM_16BIT,
    ),

    PCM_BIT_FLOAT(
        displayName = "32 bit float",
        value = AudioFormat.ENCODING_PCM_FLOAT,
    );

    override val valueForPref: Int
        get() = value

}