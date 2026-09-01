package io.github.leonidius20.recorder.entities.audio_settings

// todo: move the codec class to domain??
data class Codec(
    val id: CodecId,
    /**
     * value as expected by MediaRecorder.setAudioEncoder()
     */
    val value: Int, // todo: remove from domain. map in data from codecId
    val displayName: String, // todo: remove from domain. map in data from id
    //val isSupportedByDevice: Boolean,
    val supportedSampleRates: List<Int>,

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

    companion object // needed for extension functions

}
