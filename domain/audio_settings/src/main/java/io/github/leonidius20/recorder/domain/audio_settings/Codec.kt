package io.github.leonidius20.recorder.domain.audio_settings

import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import kotlin.math.abs

/*enum class SampleRate(
    val value: Int,
) {
    SR_8_000(8_000),
    SR_11_025(11_025),
    SR_12_000(12_000),
    SR_16_000(16_000),
    SR_22_050(22_050),
    SR_24_000(24_000),
    SR_32_000(32_000),
    SR_44_100(44_100),
    SR_48_000(48_000),
    ;

}

sealed interface ICodec {

    val value: Int
    val name: String
    val supportedSampleRates: List<SampleRate>

    interface UncompressedCodec : ICodec {

    }

    sealed interface CompressedCodec : ICodec {

        val defaultBitrate: Float

        enum class WithDiscreteBitrate(
            override val value: Int,
            override val name: String,
            override val supportedSampleRates: List<SampleRate>,
            override val defaultBitrate: Float,
            val bitrateOptions: List<Float>,
        ) : CompressedCodec {

        }

        interface WithContinuousBitrate : CompressedCodec {

        }

    }


    /**
     * codec where we cannot set neither bit rate not depth.
     * todo: remove, it's probably temporary. maybe it has more
     * complicated settings like VBR and CBR
     */
    interface NoBitSettingCodec : ICodec

}

val codecs = buildList<ICodec> {
    add(
        ICodec.CompressedCodec.WithDiscreteBitrate(
            MediaRecorder.AudioEncoder.AMR_NB,
            "AMR Narrowband",
            supportedSampleRates = listOf(SampleRate.SR_8_000),
            defaultBitrate = 12.20f,
            bitrateOptions = listOf(4.75f, 5.15f, 5.90f, 6.70f, 7.40f, 7.95f, 10.20f, 12.20f),
        )
    )


add(object : ICodec.CompressedCodec.WithDiscreteBitrate(
    MediaRecorder.AudioEncoder.AMR_NB,
    "AMR Narrowband",

    supportedSampleRates = intArrayOf(8_000),
    bitRateSettingType = BitRateSettingType.BitRateDiscreteValues(
        default = 12.20f,
        bitRateOptions = listOf(4.75f, 5.15f, 5.90f, 6.70f, 7.40f, 7.95f, 10.20f, 12.20f),
    ),

    //supportsStereo = false,
),)
}*/

val Codec<*>.bitDepthOrRateForCodecPrefKey
    get() = "$value-bit"

fun Codec<*>.supportedSampleRateClosestTo(rate: Int): Int {
    return supportedSampleRates.mapIndexed { index, supportedRate ->
        val distance = abs(rate - supportedRate)
        index to distance
    }.minBy { it.second }.let { (index, _) -> supportedSampleRates[index] }
}

fun Codec<*>.supportsSampleRate(rate: Int) = rate in supportedSampleRates

fun Codec<BitRateSettingType.BitRateValues>.supportedBitRateClosestTo(rate: Float): Float {
    return when (val option = resolutionOptions) {
        is BitRateSettingType.BitRateDiscreteValues -> {
            option.bitRateOptions.mapIndexed { index, supportedRate ->
                val distance = abs(rate - supportedRate)
                index to distance
            }.minBy { it.second }.let { (index, _) -> option.bitRateOptions[index] }
        }

        is BitRateSettingType.BitRateContinuousRange -> {
            rate.coerceIn(option.min, option.max)
        }

        else -> throw IllegalStateException()
    }
}

fun Codec<BitRateSettingType.BitRateValues>.supportsBitrate(rate: Float) =
    when (val option = resolutionOptions) {
        is BitRateSettingType.BitRateContinuousRange -> {
            rate >= option.min && rate <= option.max
        }

        is BitRateSettingType.BitRateDiscreteValues -> {
            option.bitRateOptions.contains(rate)
        }

        else -> {
            throw IllegalStateException()
        }
    }



fun Codec.Companion.getByValue(value: Int,
                               capabilities: DeviceAudioCapabilities
) =
    capabilities.codecByValue[value]!!
