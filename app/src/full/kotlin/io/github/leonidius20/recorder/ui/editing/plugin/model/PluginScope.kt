package io.github.leonidius20.recorder.ui.editing.plugin.model

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.exoplayer.MediaExtractorCompat
import org.androidaudioplugin.ParameterInformation
import org.androidaudioplugin.PluginInformation
import org.androidaudioplugin.hosting.AudioPluginClientBase
import org.androidaudioplugin.hosting.AudioPluginMidiSettings
import org.androidaudioplugin.hosting.NativeRemotePluginInstance
import org.androidaudioplugin.manager.PluginPlayer
import timber.log.Timber

/**
 * taken from example app with almost no changes
 */
class PluginDetailsScope private constructor(
    val pluginInfo: PluginInformation,
    val context: Context,
    val client: AudioPluginClientBase,
    // file descriptor here
    val file: Uri,
    val fileName: String,
    private val outFileDescriptor: Int,
) : AutoCloseable {
    companion object {
        suspend fun create(pluginInfo: PluginInformation, context: Context, client: AudioPluginClientBase, file: Uri, fileName: String, outFileDescriptor: Int): PluginDetailsScope {
            val scope = PluginDetailsScope(pluginInfo, context, client, file, fileName, outFileDescriptor)
            scope.instantiatePlugin()
            return scope
        }
    }

    var instance: NativeRemotePluginInstance? = null

    private val pluginPlayer by lazy {
        // val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        //val sampleRate =
        //    audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()

        val fileMetadata = getSampleRateAndChannelCount(file)
        // todo: get sample rate from file

        // It is for the audio processor's callback
        // FIXME: make them configurable?
        val framesPerCallback = 1024 //audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
        // val channelCount = 2 // todo: should depend in the file, could be mono or stereo, we should query that. Or is this for output, not input? Check the details of the example file (sample rate, num channels)
        PluginPlayer.create(fileMetadata.sampleRate,
            framesPerCallback, fileMetadata.channelCount, outFileDescriptor).apply {
            setPlugin(instance!!)

            // todo: handle errpr
            context.contentResolver.openInputStream(file)!!.use {
                val bytes = ByteArray(it.available())
                it.read(bytes)
                loadAudioResource(
                    bytes,
                    fileName.also {
                        Timber.d("Filename is '$it'")
                    })
            }

            //context.assets.open(PluginPlayer.sample_audio_filename).use {
            //    val bytes = ByteArray(it.available())
            //    it.read(bytes)
            //    loadAudioResource(bytes, fileName)
            //}
        }
    }

    override fun close() {
        pluginPlayer.close()
    }

    suspend fun instantiatePlugin() {
        //if (!manager.connections.any { it.serviceInfo.packageName == pluginInfo.packageName })
            client.connectToPluginService(pluginInfo.packageName)
        instance = client.instantiateNativePlugin(pluginInfo)
    }

    fun setNewMidiMappingFlags(pluginId: String, newFlags: Int) {
        AudioPluginMidiSettings.putMidiSettingsToSharedPreference(
            context,
            pluginId,
            newFlags
        )
    }

    var isProcessing: Boolean = false
        private set

    fun enableAudioRecorder() {
        pluginPlayer.enableAudioRecorder()
    }

    fun startProcessing() {
        pluginPlayer.startProcessing()
        isProcessing = true
    }

    fun pauseProcessing() {
        pluginPlayer.pauseProcessing()
        isProcessing = false
    }

    fun playPreloadedAudio() {
        pluginPlayer.playPreloadedAudio()
    }

    fun setPresetIndex(index: Int) {
        pluginPlayer.setPresetIndex(index)
    }

    fun setParameterValue(id: UInt, value: Float) {
        val ins = instance

        if (ins != null)
            pluginPlayer.setParameterValue(id, value)
    }

    fun getParameters(): Array<ParameterInformation> {
        val count = instance!!.getParameterCount()
        val params = Array(count) { index ->
            instance!!.getParameter(index)
        }
        return params
    }

    /*fun processExpression(origin: DiatonicKeyboardNoteExpressionOrigin, note: Int, value: Float) {
        when(origin) {
            DiatonicKeyboardNoteExpressionOrigin.HorizontalDragging -> pluginPlayer.processPitchBend(-1, value)
            DiatonicKeyboardNoteExpressionOrigin.VerticalDragging -> pluginPlayer.processPitchBend(note, value)
            DiatonicKeyboardNoteExpressionOrigin.Pressure -> pluginPlayer.processPressure(note, value)
        }
    }*/

    fun setNoteState(note: Int, isNoteOn: Boolean) {
        pluginPlayer.setNoteState(note, 0xF800, isNoteOn)
    }

    data class FileDetails(
        val sampleRate: Int,
        val channelCount: Int,
    )

    private fun getSampleRateAndChannelCount(uri: Uri): FileDetails {
        // todo: i suppose you could do it with MediaStore??

        val extractor = MediaExtractorCompat(context)
        extractor.setDataSource(uri, 0)
        val trackFormat = extractor.getTrackFormat(0)

        return FileDetails(
            sampleRate = trackFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
            channelCount = trackFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT),
        ).also {
            Timber.d("Detected sample rate as ${it.sampleRate}, ${it.channelCount} channels")
        }
    }

}
