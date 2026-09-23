package org.androidaudioplugin.manager

import dev.atsushieno.ktmidi.Ump
import dev.atsushieno.ktmidi.UmpFactory
import dev.atsushieno.ktmidi.toPlatformNativeBytes
import org.androidaudioplugin.hosting.NativeRemotePluginInstance
import org.androidaudioplugin.hosting.UmpHelper

class PluginPlayer private constructor(private val native: Long) : AutoCloseable {

    companion object {

        @Volatile
        private var sampleCallback: (Float) -> Unit = {}

        fun create(sampleRate: Int, framesPerCallback: Int, channelCount: Int, outFileFd: Int = -1) =
            PluginPlayer(createNewPluginPlayer(sampleRate, framesPerCallback, channelCount, outFileFd))

        init {
            System.loadLibrary("androidaudioplugin-manager")
        }

        @JvmStatic
        private external fun createNewPluginPlayer(sampleRate: Int, framesPerCallback: Int, channelCount: Int, outFileFd: Int): Long

        @JvmStatic
        fun callback(sample: Float) {
            // Log.d("PluginPlayer", "Got {sample} sample in callback")
            sampleCallback(sample)
        }
    }

    override fun close() {
        deletePluginPlayer(native)
    }

    private external fun deletePluginPlayer(native: Long)

    fun addPlugin(plugin: NativeRemotePluginInstance) = addPluginNative(native, plugin.client, plugin.instanceId)

    private external fun addPluginNative(player: Long, nativeClient: Long, instanceId: Int)

    fun loadAudioResource(bytes: ByteArray, filename: String) =
        loadAudioResourceNative(native, bytes, filename)

    private external fun loadAudioResourceNative(player: Long, bytes: ByteArray, filename: String)

    fun startProcessing() = startProcessingNative(native)

    private external fun startProcessingNative(native: Long)

    fun pauseProcessing() = pauseProcessingNative(native)

    private external fun pauseProcessingNative(native: Long)

    fun playPreloadedAudio() = playPreloadedAudioNative(native)

    private external fun playPreloadedAudioNative(native: Long)

    fun enableAudioRecorder() = enableAudioRecorderNative(native)

    private external fun enableAudioRecorderNative(native: Long)

    // MIDI events

    fun addMidiEvent(ump: Long) = addMidiEvent(Ump(ump))

    fun addMidiEvent(ump: Ump) = addMidiEvents(ump.toPlatformNativeBytes())

    fun addMidiEvents(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset, timestampInNanoseconds: Long = 0) =
        addMidiEventsNative(native, bytes, offset, length, timestampInNanoseconds)

    private external fun addMidiEventsNative(native: Long, bytes: ByteArray, offset: Int = 0, length: Int, timestampInNanoseconds: Long)

    fun setParameterValue(parameterId: UInt, value: Float) {
        val ints = UmpHelper.aapUmpSysex8Parameter(parameterId, value)
        val umps = ints.filterIndexed { i, _ -> i % 4 == 0 }.flatMapIndexed { i, v ->
            Ump(v, ints[i * 4 + 1], ints[i * 4 + 2], ints[i * 4 + 3]).toPlatformNativeBytes().asList() }
        addMidiEvents(umps.toByteArray())
    }

    fun processPitchBend(note: Int, value: Float) {
        assert(value in -1.0..1.0)
        val ump =
            if (note < 0) UmpFactory.midi2PitchBend(0, 0, (0x1_0000_0000 * value).toLong())
            else UmpFactory.midi2PerNotePitchBend(0, 0, note, (0x1_0000_0000 * value).toLong())
        addMidiEvent(ump)
    }

    fun processPressure(note: Int, value: Float) {
        assert(value in -1.0..1.0)
        val ump = Ump(UmpFactory.midi2PAf(0, 0, note, (0x1_0000_0000 * value).toLong()))
        addMidiEvent(ump)
    }

    fun setNoteState(note: Int, velocity16: Int, isNoteOn: Boolean) {
        val ump =
            if (isNoteOn) Ump(UmpFactory.midi2NoteOn(0, 0, note, 0, velocity16, 0))
            else Ump(UmpFactory.midi2NoteOff(0, 0, note, 0, velocity16, 0))
        addMidiEvent(ump)
    }

    // non-MIDI events
    // plugin number = index of plugin in chain, starting from 0
    fun setPresetIndex(
        index: Int,
        pluginNumber: Int = 0,
    ) = setPresetIndexNative(native, pluginNumber, index)

    private external fun setPresetIndexNative(native: Long, pluginNumber: Int, index: Int)
}
