package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi

enum class Container(
    /**
     * value as expected by MediaRecorder.setOutputFormat()
     */
    val value: Int,
    val displayName: String,
    val mimeType: String,
    private val isSupportedByDevice: Boolean,
    /**
     * determined based on https://developer.android.com/media/platform/supported-formats
     */
    private val supportedCodecs: List<Codec>,
) {

    THREE_GPP(
        MediaRecorder.OutputFormat.THREE_GPP,
        "3GPP", "audio/3gpp",
        true,
        listOf(Codec.AAC, Codec.HE_AAC, Codec.AAC_ELD, Codec.AMR_NB, Codec.AMR_WB)
    ),

    MPEG4(
        MediaRecorder.OutputFormat.MPEG_4,
        "MPEG4", "audio/mp4",
        true,
        listOf(Codec.AAC, Codec.HE_AAC, Codec.AAC_ELD)
    ),

    AAC_ADTS(
        MediaRecorder.OutputFormat.AAC_ADTS,
        "AAC ADTS", "audio/aac-adts",
        true,
        listOf(
            Codec.AAC,
            // todo fix and bring back
            // Codec.HE_AAC,
            // Codec.AAC_ELD,
        )
    ),

    AMR_NB(
        MediaRecorder.OutputFormat.AMR_NB,
        "AMR Narrowband", "audio/amr",
        true,
        listOf(Codec.AMR_NB)
    ),

    AMR_WB(
        MediaRecorder.OutputFormat.AMR_WB,
        "AMR Wideband", "audio/amr-wb",
        true,
        listOf(Codec.AMR_WB)
    ),

    @RequiresApi(Build.VERSION_CODES.Q)
    OGG(
        MediaRecorder.OutputFormat.OGG,
        "OGG", "audio/ogg",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        listOf(Codec.OPUS)
    ),


    WAV(
        value = -1,
        displayName = "WAV", mimeType = "audio/x-wav",
        isSupportedByDevice = true,
        supportedCodecs = listOf(Codec.PCM)
    );

    /**
     * we consider the container to be supported if the API level of the device is high
     * enough (isSupportedByDevice) and if there is at least one codec that can be put
     * in this container that is supported by the device.
     */
    val isSupported: Boolean
        get() = isSupportedByDevice && availableCodecs.isNotEmpty()

    /**
     * default codec for this container
     */
    val defaultCodec: Codec
        get() = supportedCodecs.first { it.isSupportedByDevice }

    /**
     * codecs that can be put into this container and that are supported by device
     */
    val availableCodecs: List<Codec>
        get() = supportedCodecs.filter { it.isSupportedByDevice }

    fun supports(codec: Codec) = availableCodecs.contains(codec)

    companion object {

        private val map by lazy {
            Container.entries.associateBy {
                it.value
            }
        }

        fun getByValue(value: Int) = map[value]!!

        fun supportedContainers() = entries.filter { it.isSupported }

    }

}
