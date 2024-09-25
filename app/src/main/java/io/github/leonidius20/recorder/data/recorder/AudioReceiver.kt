package io.github.leonidius20.recorder.data.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Named

@ServiceScoped
class AudioReceiver @Inject constructor(
    @Named("cpu") private val cpuDispatcher: CoroutineDispatcher,
)  {

    /**
     * we are going to have 1 coroutine reading audio bytes and putting them into a channel
     * with an unlimited buffer,
     * another coroutine in another class - reading from channel, encoding with mediaencoder
     * and either saving to file or passing to a file-writing coroutine in third class
     * (probably good idea considering that IO takes a long time)
     *
     * and we will have UI thread doing its work.
     * all other coroutines will be executed in other threads (cpu-intensive or IO)
     */

    private val _channel = Channel<ByteArray>(capacity = Channel.UNLIMITED)

    val channel: ReceiveChannel<ByteArray>
        get() = _channel

    private lateinit var audioRecord: AudioRecord

    private lateinit var readJob: Job

    @SuppressLint("MissingPermission")
    fun CoroutineScope.startRecording() {

        // todo: withCountext(Dispatchers.Defaut) {}

        // SharedFlow? it has to be hot i think
        // it could also be a channel theoretically

        val sampleRate = 8_000

        val inputChannel = AudioFormat.CHANNEL_IN_MONO
        val encoder = AudioFormat.ENCODING_PCM_16BIT

        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate, inputChannel, encoder
        )
        val bufSize = minBufSize * 4 // why 4?

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC, // todo: get from settings
            sampleRate,
            inputChannel,
            encoder,
            bufSize
        )



        // todo: move the following into resumeRec() and call it here
        readJob = this.launch(cpuDispatcher) {
            val buffer = ByteArray(bufSize)


            while (this.isActive) {
                val bytesRead = audioRecord.read(buffer, 0, bufSize)

                _channel.send(
                    buffer.sliceArray(0 until bytesRead)
                )
            }
        }
    }

    fun stopRec() {
        readJob.cancel() // todo: pause?
        audioRecord.stop()
        audioRecord.release() // todo: only if final stop and not just pause
        channel.cancel()
    }

    fun pauseRec() {
        //
    }

    fun CoroutineScope.resumeRec() {

    }



}