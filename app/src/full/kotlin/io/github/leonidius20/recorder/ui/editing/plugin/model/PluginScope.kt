package io.github.leonidius20.recorder.ui.editing.plugin.model

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.widget.Toast
import org.androidaudioplugin.ParameterInformation
import org.androidaudioplugin.PluginInformation
import org.androidaudioplugin.hosting.AudioPluginClientBase
import org.androidaudioplugin.hosting.AudioPluginMidiSettings
import org.androidaudioplugin.hosting.NativeRemotePluginInstance
import org.androidaudioplugin.manager.PluginPlayer

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
) : AutoCloseable {
    companion object {
        suspend fun create(pluginInfo: PluginInformation, context: Context, client: AudioPluginClientBase, file: Uri, fileName: String): PluginDetailsScope {
            val scope = PluginDetailsScope(pluginInfo, context, client, file, fileName)
            scope.instantiatePlugin()
            return scope
        }
    }

    var instance: NativeRemotePluginInstance? = null

    private val pluginPlayer by lazy {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val sampleRate = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE).toInt()
        // It is for the audio processor's callback
        // FIXME: make them configurable?
        val frames = 1024 //audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER).toInt()
        val channelCount = 2 // todo: should depend in the file, could be mono or stereo, we should query that. Or is this for output, not input? Check the details of the example file (sample rate, num channels)
        PluginPlayer.create(sampleRate, frames, channelCount).apply {
            setPlugin(instance!!)
            // todo: replace file name here

            // todo: handle errpr
            context.contentResolver.openInputStream(file)!!.use {
                val bytes = ByteArray(it.available())
                it.read(bytes)
                // replace with "filename"
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
}
