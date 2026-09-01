package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.data.recorder.MediaRecorderWrapper
import io.github.leonidius20.recorder.data.recorder.PcmAudioRecorder
import io.github.leonidius20.recorder.data.settings.PcmBitDepthOption
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import kotlinx.coroutines.CoroutineScope
import java.io.IOException
import javax.inject.Inject

class AudioRecorderFactoryImpl @Inject constructor(
    private val settings: Settings, // todo maybe pass in method instead of injecting
    @param:Scope.App private val scope: CoroutineScope,
) : AudioRecorderFactory {

    @Throws(IOException::class)
    override fun create(
        file: OutputFile,
    ): AudioRecorder {
        // todo pass here so that it doesn't get changed between file creation and now
        val settingsState = settings.state.value
        val fileFormat = settingsState.outputFormat

        // todo: do something about this
        val file = file as OutputFileImpl

        // todo: support for other codecs with bit depth, etc
        //  or remove mediarecorderwrapper at all
        return when(val resolution = settingsState.resolution) {
            is Resolution.BitDepth -> {
                PcmAudioRecorder(
                    descriptor = file.descriptor,
                    audioSource = settingsState.audioSource,
                    sampleRate = settingsState.sampleRate,
                    monoOrStereo = settingsState.numOfChannels,
                    // todo: redo when settings allow
                    bitDepth = resolution.value as? PcmBitDepthOption
                        ?: PcmBitDepthOption.PCM_16BIT_INT,
                    coroutineScope = scope,
                )
            }
            else -> {
                val bitrate = when(resolution) {
                    is Resolution.None -> null
                    is Resolution.Bitrate -> resolution.value
                    else -> throw IllegalStateException() // todo: can we remove?
                }

                MediaRecorderWrapper(
                    audioSource = settingsState.audioSource,
                    container = fileFormat,
                    descriptor = file.descriptor,
                    encoder = settingsState.encoder,
                    channels = settingsState.numOfChannels,
                    sampleRate = settingsState.sampleRate,
                    bitRate = bitrate
                )
            }
        }
    }

}
