package io.github.leonidius20.recorder.domain.recorder

import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.data.recorder.MediaRecorderWrapper
import io.github.leonidius20.recorder.data.recorder.PcmAudioRecorder
import io.github.leonidius20.recorder.data.settings.BitRateSettingType
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.PcmBitDepthOption
import io.github.leonidius20.recorder.data.settings.Settings
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import javax.inject.Inject
import kotlin.jvm.Throws

interface AudioRecorderFactory {

    @Throws(IOException::class)
    fun create(
        file: OutputFileAbstraction,
    ): AudioRecorder

}

@ServiceScoped
class AudioRecorderFactoryImpl @Inject constructor(
    private val settings: Settings, // todo maybe pass in method instead of injecting
    private val scope: CoroutineScope, // service  scope
) : AudioRecorderFactory {

    @Throws(IOException::class)
    override fun create(
        file: OutputFileAbstraction,
    ): AudioRecorder {
        // todo pass here so that it doesn't get changed between file creation and now
        val settingsState = settings.state.value
        val fileFormat = settingsState.outputFormat

        return if (fileFormat == Container.WAV) {
            PcmAudioRecorder(
                descriptor = file.descriptor,
                audioSource = settingsState.audioSource,
                sampleRate = settingsState.sampleRate,
                monoOrStereo = settingsState.numOfChannels,
                bitDepth = settingsState.bitDepth as? PcmBitDepthOption
                    ?: PcmBitDepthOption.PCM_16BIT_INT,
                coroutineScope = scope,
            )
        } else {

                MediaRecorderWrapper(
                    audioSource = settingsState.audioSource,
                    container = fileFormat,
                    descriptor = file.descriptor,
                    encoder = settingsState.encoder,
                    channels = settingsState.numOfChannels,
                    sampleRate = settingsState.sampleRate,
                    bitRate =
                        if (settingsState.encoder.bitRateSettingType is BitRateSettingType.BitRateValues)
                            settingsState.bitRate
                        else null
                )


        }
    }

}
