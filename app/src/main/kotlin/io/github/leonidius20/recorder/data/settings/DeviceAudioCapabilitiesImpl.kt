package io.github.leonidius20.recorder.data.settings

import android.content.Context
import android.content.Context.AUDIO_SERVICE
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.domain.audio_settings.DeviceAudioCapabilities
import io.github.leonidius20.recorder.domain.audio_settings.PcmBitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.CodecId
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.ContainerId
import java.util.SortedSet
import javax.inject.Inject

class DeviceAudioCapabilitiesImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : DeviceAudioCapabilities {

    /**
     * sample rates supported by device. There is also a separate thing which is
     * sample rates supported by various codecs.
     */
    override val sampleRatesSupportedByDevice: SortedSet<Int> =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context.getSystemService(AUDIO_SERVICE) as AudioManager)
                .getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull()
                ?.sampleRates
                ?.toSortedSet()
                ?.let {
                    // if empty, means the device supports arbitrary values with resampling.
                    // we will just stick to some standard ones
                    if (it.isEmpty()) null else it
                }
        } else {
            null
        }) ?: sortedSetOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000)

    override val containers = buildList {

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

    override val containersMap = containers.associateBy {
        it.value
    }

    override val codecs = buildList {

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

    override val codecById = codecs.associateBy { it.id }

    // todo remove
    override val codecByValue = codecs.associateBy { it.value }

    companion object {
        val container3gpp = Container(
            id = ContainerId.THREE_GPP,
            MediaRecorder.OutputFormat.THREE_GPP,
            "3GPP", "audio/3gpp",
            listOf(CodecId.AAC, CodecId.HE_AAC, CodecId.AAC_ELD, CodecId.AMR_NB, CodecId.AMR_WB)
        )

        val codecAmrNb = Codec(
            id = CodecId.AMR_NB,
            MediaRecorder.AudioEncoder.AMR_NB,
            "AMR Narrowband",
            // todo: intersect with device supported sample rates
            supportedSampleRates = listOf(8_000),
            resolutionOptions = BitRateSettingType.BitRateDiscreteValues(
                default = 12.20f,
                bitRateOptions = listOf(4.75f, 5.15f, 5.90f, 6.70f, 7.40f, 7.95f, 10.20f, 12.20f),
            ),

            //supportsStereo = false,
        )
    }

}
