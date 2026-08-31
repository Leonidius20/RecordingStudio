package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import io.github.leonidius20.recorder.entities.audio_settings.CodecId

// todo: replace w/ sealed interface?
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
    private val supportedCodecIds: List<CodecId>,
) {

    THREE_GPP(
        MediaRecorder.OutputFormat.THREE_GPP,
        "3GPP", "audio/3gpp",
        true,
        listOf(CodecId.AAC, CodecId.HE_AAC, CodecId.AAC_ELD, CodecId.AMR_NB, CodecId.AMR_WB)
    ),

    MPEG4(
        MediaRecorder.OutputFormat.MPEG_4,
        "MPEG4", "audio/mp4",
        true,
        listOf(CodecId.AAC, CodecId.HE_AAC, CodecId.AAC_ELD)
    ),

    AAC_ADTS(
        MediaRecorder.OutputFormat.AAC_ADTS,
        "AAC ADTS", "audio/aac-adts",
        true,
        listOf(
            CodecId.AAC,
            // todo fix and bring back
            // CodecId.HE_AAC,
            // CodecId.AAC_ELD,
        )
    ),

    AMR_NB(
        MediaRecorder.OutputFormat.AMR_NB,
        "AMR Narrowband", "audio/amr",
        true,
        listOf(CodecId.AMR_NB)
    ),

    AMR_WB(
        MediaRecorder.OutputFormat.AMR_WB,
        "AMR Wideband", "audio/amr-wb",
        true,
        listOf(CodecId.AMR_WB)
    ),

    @RequiresApi(Build.VERSION_CODES.Q)
    OGG(
        MediaRecorder.OutputFormat.OGG,
        "OGG", "audio/ogg",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        listOf(CodecId.OPUS)
    ),


    WAV(
        value = -1,
        displayName = "WAV", mimeType = "audio/x-wav",
        isSupportedByDevice = true,
        supportedCodecIds = listOf(CodecId.PCM)
    );

    /**
     * we consider the container to be supported if the API level of the device is high
     * enough (isSupportedByDevice) and if there is at least one CodecId that can be put
     * in this container that is supported by the device.
     */
    val isSupported: Boolean
        get() = isSupportedByDevice && availableCodecs.isNotEmpty()

    /**
     * default CodecId for this container
     */
    val defaultCodec: Codec
        get() = codecById[supportedCodecIds.first { id ->
            // exists in the list of codecs available on device
            codecs.any { it.id == id }
        }]!!

    /**
     * CodecIds that can be put into this container and that are supported by device
     */
    val availableCodecs: List<Codec>
        get() = supportedCodecIds.mapNotNull { codecById[it] }

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
