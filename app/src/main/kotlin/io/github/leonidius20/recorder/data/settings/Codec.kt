package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.CodecId
import kotlin.collections.associateBy
import kotlin.collections.buildList
import kotlin.collections.find
import kotlin.collections.listOf
import kotlin.collections.map
import kotlin.collections.mapIndexed
import kotlin.collections.minBy
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


fun Codec<BitRateSettingType.BitDepthDiscreteValues>.getBitDepthOptionFromPrefValue(prefValue: Int): BitDepthOption {
    return PcmBitDepthOption.entries.find { it.valueForPref == prefValue }!!
}

private val Codec.Companion.map by lazy {
    codecs.associateBy { it.value }
}

fun Codec.Companion.getByValue(value: Int) = map[value]!!


val codecAmrNb = Codec(
    id = CodecId.AMR_NB,
    MediaRecorder.AudioEncoder.AMR_NB,
    "AMR Narrowband",
    //true,
    // todo: intersect with device supported sample rates
    supportedSampleRates = listOf(8_000),
    resolutionOptions = BitRateSettingType.BitRateDiscreteValues(
        default = 12.20f,
        bitRateOptions = listOf(4.75f, 5.15f, 5.90f, 6.70f, 7.40f, 7.95f, 10.20f, 12.20f),
    ),

    //supportsStereo = false,
)

val codecs = buildList {

    // todo: check support some other way too

    add(codecAmrNb)

    add(
        Codec(
            id = CodecId.AMR_WB,
            MediaRecorder.AudioEncoder.AMR_WB,
            "AMR Wideband",
            //true,
            supportedSampleRates = listOf(16_000),

            resolutionOptions = BitRateSettingType.BitRateDiscreteValues(
                bitRateOptions = listOf(
                    6.6f,
                    8.85f,
                    12.65f,
                    14.25f,
                    15.85f,
                    18.25f,
                    19.85f,
                    23.05f,
                    23.85f
                ),
                default = 23.85f,
            ),
            //supportsStereo = false,
        )
    )

    // todo: vbr, cbr, etc?
    add(
        Codec(
            id = CodecId.AAC,
            MediaRecorder.AudioEncoder.AAC,
            "AAC-LC",
            //true,
            supportedSampleRates = listOf(
                8000,
                11025,
                12000,
                16000,
                22050,
                24000,
                32000,
                44100,
                48000
            ),
            resolutionOptions = BitRateSettingType.None,
        )
    )

    add(
        Codec(
            id = CodecId.HE_AAC,
            MediaRecorder.AudioEncoder.HE_AAC,
            "HE-AAC",
            //true,
            supportedSampleRates = listOf(
                8000,
                11025,
                12000,
                16000,
                22050,
                24000,
                32000,
                44100,
                48000
            ),
            resolutionOptions = BitRateSettingType.None,
        )
    )

    add(
        Codec(
            id = CodecId.AAC_ELD,
            MediaRecorder.AudioEncoder.AAC_ELD,
            "AAC-ELD",
            //true,
            supportedSampleRates = listOf(16000, 22050, 24000, 32000, 44100, 48000),
            resolutionOptions = BitRateSettingType.None,
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

        add(
            Codec(
                id = CodecId.OPUS,
                MediaRecorder.AudioEncoder.OPUS,
                "Opus",
                //Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                // https://hydrogenaud.io/index.php/topic,115663.0.html
                supportedSampleRates = listOf(8000, 12000, 16000, 24000, 48000),
                resolutionOptions = BitRateSettingType.BitRateContinuousRange(
                    min = 6f,
                    max = 510f,
                    default = 128f,
                ),
            )
        )
    }


    add(
        Codec(
            id = CodecId.PCM,
            value = -1,
            displayName = "PCM",
            //isSupportedByDevice = true,
            supportedSampleRates = listOf(
                8000,
                11025,
                12000,
                16000,
                22050,
                24000,
                32000,
                44100,
                48000
            ),
            resolutionOptions = BitRateSettingType.BitDepthDiscreteValues(
                availableOptions = PcmBitDepthOption.entries.map { it as BitDepthOption },
                default = PcmBitDepthOption.PCM_16BIT_INT,
            ),
        )
    )

}

val codecById = codecs.associateBy { it.id }
