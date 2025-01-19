package io.github.leonidius20.recorder.data.settings

/**
 * denotes the way bitrate can be set for a codec
 */
sealed interface BitRateSettingType {

    data class BitDepthDiscreteValues(
        val availableOptions: Array<BitDepthOption>,
        val default: BitDepthOption,
    ) : BitRateSettingType

    interface BitRateValues : BitRateSettingType {
        val default: Float
    }

    data class BitRateDiscreteValues(
        val bitRateOptions: Array<Float>,
        override val default: Float,
    ) : BitRateValues

    data class BitRateContinuousRange(
        /**
         * in kbps
         */
        val min: Float,
        /**
         * in kbps
         */
        val max: Float,
        override val default: Float,
    ) : BitRateValues

    data object None : BitRateSettingType

}