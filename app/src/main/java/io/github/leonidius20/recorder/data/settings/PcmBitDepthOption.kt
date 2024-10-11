package io.github.leonidius20.recorder.data.settings

import android.media.AudioFormat

enum class PcmBitDepthOption(
    val valueForAudioRecordApi: Int,
    override val displayName: String,
    val bitsPerSample: Short,
    /**
     * is false, then it's Int
     */
    val isFloat: Boolean,
) : BitDepthOption {

    PCM_8BIT_INT(
        displayName = "8 bit int",
        valueForAudioRecordApi = AudioFormat.ENCODING_PCM_8BIT,
        bitsPerSample = 8,
        isFloat = false,
    ),

    PCM_16BIT_INT(
        displayName = "16 bit int",
        valueForAudioRecordApi = AudioFormat.ENCODING_PCM_16BIT,
        bitsPerSample = 16,
        isFloat = false,
    ),

    PCM_BIT_FLOAT(
        displayName = "32 bit float",
        valueForAudioRecordApi = AudioFormat.ENCODING_PCM_FLOAT,
        bitsPerSample = 32,
        isFloat = true,
    );

    // todo: 24 and 32 bit

    override val valueForPref: Int
        get() = valueForAudioRecordApi

}