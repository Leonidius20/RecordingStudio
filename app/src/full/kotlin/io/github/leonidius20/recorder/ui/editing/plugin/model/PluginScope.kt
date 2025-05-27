package io.github.leonidius20.recorder.ui.editing.plugin.model

import android.content.Context
import android.media.AudioManager
import android.media.MediaFormat
import android.net.Uri
import android.widget.Toast
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

        val sampleRate = getSampleRate(file)
        // todo: get sample rate from file

        // It is for the audio processor's callback
        // FIXME: make them configurable?
        val frames = 1024 //audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
        val channelCount = 2 // todo: should depend in the file, could be mono or stereo, we should query that. Or is this for output, not input? Check the details of the example file (sample rate, num channels)
        PluginPlayer.create(sampleRate, frames, channelCount, outFileDescriptor).apply {
            setPlugin(instance!!)
            // todo: replace file name here

            // todo: handle errpr
            context.contentResolver.openInputStream(file)!!.use {
                val bytes = ByteArray(it.available())
                it.read(bytes)
                // replace with "filename". (Maybe MediaStore returns it w/o extension??)
                loadAudioResource(bytes, PluginPlayer.sample_audio_filename)
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
        Toast.makeText(context, "started processing", Toast.LENGTH_SHORT).show()
    }

    fun pauseProcessing() {
        pluginPlayer.pauseProcessing()
        isProcessing = false
        Toast.makeText(context, "paused processing", Toast.LENGTH_SHORT).show()
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

    private fun getSampleRate(uri: Uri): Int {
        // todo: i suppose you could do it with MediaStore??

        val extractor = MediaExtractorCompat(context)
        extractor.setDataSource(uri, 0)
        return extractor.getTrackFormat(0).getInteger(MediaFormat.KEY_SAMPLE_RATE).also {
            Timber.d("Detected sample rate as $it")
        }
    }

}
