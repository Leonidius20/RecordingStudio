package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.abs

enum class Codec(
    /**
     * value as expected by MediaRecorder.setAudioEncoder()
     */
    val value: Int,
    val displayName: String,
    val isSupportedByDevice: Boolean,
    val supportedSampleRates: IntArray,

    // val supportsStereo: Boolean = true,

    val bitRateSettingType: BitRateSettingType,

    //val supportsSettingBitDepth: Boolean = false,
    //val supportsSettingBitRate: Boolean = false,

    //val bitDepthOptions: Array<BitDepthOption>? = null,
    //val defaultBitDepth: BitDepthOption? = null,

    /**
     * in kbps (MediaRecorder asks for bps so there has to be multiplication)
     */
    //val bitRateOptions: Array<Float>? = null,
   // val defaultBitRate: Float? = null,

    // val isBitRateContinuous : Boolean? = null,
) {

    // todo: check support some other way too

    AMR_NB(
        MediaRecorder.AudioEncoder.AMR_NB,
        "AMR Narrowband",
        true,
        supportedSampleRates = intArrayOf(8_000),
        bitRateSettingType = BitRateSettingType.BitRateDiscreteValues(
            default = 12.20f,
            bitRateOptions = arrayOf(4.75f, 5.15f, 5.90f, 6.70f, 7.40f, 7.95f, 10.20f, 12.20f),
        ),

        //supportsStereo = false,
    ),

    AMR_WB(
        MediaRecorder.AudioEncoder.AMR_WB,
        "AMR Wideband",
        true,
        supportedSampleRates = intArrayOf(16_000),

        bitRateSettingType = BitRateSettingType.BitRateDiscreteValues(
            bitRateOptions = arrayOf(6.6f, 8.85f, 12.65f, 14.25f, 15.85f, 18.25f, 19.85f, 23.05f, 23.85f),
            default = 23.85f,
        ),
        //supportsStereo = false,
    ),

    AAC(
        MediaRecorder.AudioEncoder.AAC,
        "AAC-LC",
        true,
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),
        bitRateSettingType = BitRateSettingType.None,
    ),

    HE_AAC(
        MediaRecorder.AudioEncoder.HE_AAC,
        "HE-AAC",
        true,
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),
        bitRateSettingType = BitRateSettingType.None,
    ),

    AAC_ELD(
        MediaRecorder.AudioEncoder.AAC_ELD,
        "AAC-ELD",
        true,
        supportedSampleRates = intArrayOf(16000, 22050, 24000, 32000, 44100, 48000),
        bitRateSettingType = BitRateSettingType.None,
    ),

    @RequiresApi(Build.VERSION_CODES.Q)
    OPUS(
        MediaRecorder.AudioEncoder.OPUS,
        "Opus",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        // https://hydrogenaud.io/index.php/topic,115663.0.html
        supportedSampleRates = intArrayOf(8000, 12000, 16000, 24000, 48000),
        bitRateSettingType = BitRateSettingType.BitRateContinuousRange(
            min = 6f,
            max = 510f,
            default = 128f,
        ),
    ),


    PCM(
        value = -1,
        displayName = "PCM",
        isSupportedByDevice = true,
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),
        bitRateSettingType = BitRateSettingType.BitDepthDiscreteValues(
            availableOptions = PcmBitDepthOption.entries.map { it as BitDepthOption }.toTypedArray(),
            default = PcmBitDepthOption.PCM_16BIT_INT,
        ),
    );

    val bitDepthOrRateForCodecPrefKey
        get() = "$value-bit"

    fun supportedSampleRateClosestTo(rate: Int): Int {
        return supportedSampleRates.mapIndexed { index, supportedRate ->
            val distance = abs(rate - supportedRate)
            index to distance
        }.minBy { it.second }.let { (index, _) -> supportedSampleRates[index] }
    }

    fun supportsSampleRate(rate: Int) = rate in supportedSampleRates

    fun getBitDepthOptionFromPrefValue(prefValue: Int): BitDepthOption {
        if(this == Codec.PCM) {
            return PcmBitDepthOption.entries.find { it.valueForPref == prefValue }!!
        } else {
            throw Error("this codec does not support setting bit depths")
        }
    }

    companion object {

        private val map by lazy {
            Codec.entries.associateBy { it.value }
        }

        fun getByValue(value: Int) = map[value]!!

    }

}
