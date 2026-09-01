package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.CodecId
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.ContainerId

/**
 * we consider the container to be supported if the API level of the device is high
 * enough (isSupportedByDevice) and if there is at least one CodecId that can be put
 * in this container that is supported by the device.
 */
val Container.isSupported: Boolean
    get() = availableCodecs.isNotEmpty()

/**
 * default CodecId for this container
 */
val Container.defaultCodec: Codec<*>
    get() = codecById[supportedCodecIds.first { id ->
        // exists in the list of codecs available on device
        codecs.any { it.id == id }
    }]!!

/**
 * CodecIds that can be put into this container and that are supported by device
 */
val Container.availableCodecs: List<Codec<*>>
    get() = supportedCodecIds.mapNotNull { codecById[it] }

fun Container.supports(codec: Codec<*>) = availableCodecs.contains(codec)

private val Container.Companion.map by lazy {
    containers.associateBy {
        it.value
    }
}

fun Container.Companion.getByValue(value: Int) = map[value]!!

fun Container.Companion.supportedContainers() = containers.filter { it.isSupported }

val container3gpp = Container(
    id = ContainerId.THREE_GPP,
    MediaRecorder.OutputFormat.THREE_GPP,
    "3GPP", "audio/3gpp",
    listOf(CodecId.AAC, CodecId.HE_AAC, CodecId.AAC_ELD, CodecId.AMR_NB, CodecId.AMR_WB)
)

val containers = buildList {

    add(container3gpp)

    add(
        Container(
            id = ContainerId.MPEG4,
            MediaRecorder.OutputFormat.MPEG_4,
            "MPEG4", "audio/mp4",
            listOf(CodecId.AAC, CodecId.HE_AAC, CodecId.AAC_ELD)
        )
    )

    add(
        Container(
            id = ContainerId.AAC_ADTS,
            MediaRecorder.OutputFormat.AAC_ADTS,
            "AAC ADTS", "audio/aac-adts",
            listOf(
                CodecId.AAC,
                // todo fix and bring back
                // CodecId.HE_AAC,
                // CodecId.AAC_ELD,
            )
        )
    )

    add(
        Container(
            id = ContainerId.AMR_NB,
            MediaRecorder.OutputFormat.AMR_NB,
            "AMR Narrowband", "audio/amr",
            listOf(CodecId.AMR_NB)
        )
    )

    add(
        Container(
            id = ContainerId.AMR_WB,
            MediaRecorder.OutputFormat.AMR_WB,
            "AMR Wideband", "audio/amr-wb",
            listOf(CodecId.AMR_WB)
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        add(
            Container(
                id = ContainerId.OGG,
                MediaRecorder.OutputFormat.OGG,
                "OGG", "audio/ogg",
                listOf(CodecId.OPUS)
            )
        )
    }


    add(
        Container(
            id = ContainerId.WAV,
            value = -1,
            displayName = "WAV", mimeType = "audio/x-wav",
            supportedCodecIds = listOf(CodecId.PCM)
        )
    )
}
