package io.github.leonidius20.recorder.data.settings

import android.media.AudioFormat
import io.github.leonidius20.recorder.data.recorder.amplitudes.Float32BitMaxAmpExtractor
import io.github.leonidius20.recorder.data.recorder.amplitudes.Int16BitMaxAmpExtractor
import io.github.leonidius20.recorder.data.recorder.amplitudes.Int8BitMaxAmpExtractor
import io.github.leonidius20.recorder.data.recorder.amplitudes.MaxAmplitudeExtractor

enum class PcmBitDepthOption(
    val valueForAudioRecordApi: Int,
    override val displayName: String,
    val bitsPerSample: Short,
    /**
     * is false, then it's Int
     */
    val isFloat: Boolean,
    val maxAmplitudeExtractorFactory: () -> MaxAmplitudeExtractor,
) : BitDepthOption {

    // todo: fix visualization and re-enable
    /*PCM_8BIT_INT(
        displayName = "8 bit int",
        valueForAudioRecordApi = AudioFormat.ENCODING_PCM_8BIT,
        bitsPerSample = 8,
        isFloat = false,
        maxAmplitudeExtractorFactory = { Int8BitMaxAmpExtractor() },
    ),*/

    PCM_16BIT_INT(
        displayName = "16 bit int",
        valueForAudioRecordApi = AudioFormat.ENCODING_PCM_16BIT,
        bitsPerSample = 16,
        isFloat = false,
        maxAmplitudeExtractorFactory = { Int16BitMaxAmpExtractor() },
    ),

    PCM_BIT_FLOAT(
        displayName = "32 bit float",
        valueForAudioRecordApi = AudioFormat.ENCODING_PCM_FLOAT,
        bitsPerSample = 32,
        isFloat = true,
        maxAmplitudeExtractorFactory = { Float32BitMaxAmpExtractor() },
    );

    // todo: 24 and 32 bit

    override val valueForPref: Int
        get() = valueForAudioRecordApi

}