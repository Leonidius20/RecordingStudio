package io.github.leonidius20.recorder.data.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import io.github.leonidius20.recorder.audio_config.data.valueForAudioRecordApi
import io.github.leonidius20.recorder.audio_config.domain.impl.PcmBitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.recorder.domain.recorder.AudioRecorder
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.max

private const val WAV_HEADER_LENGTH_BYTES = 44

class PcmAudioRecorder(
    private val descriptor: ParcelFileDescriptor,
    private val audioSource: Int = MediaRecorder.AudioSource.MIC,
    private val sampleRate: Int,
    private val monoOrStereo: AudioChannels = AudioChannels.MONO,
    private val bitDepth: PcmBitDepthOption = PcmBitDepthOption.PCM_16BIT_INT,

    /**
     * used to launch the coroutine reading bytes from mic in loop
     */
    private val coroutineScope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AudioRecorder {

    private lateinit var micReadingThread: Job

    private val inputChannel = when (monoOrStereo) {
        AudioChannels.MONO -> AudioFormat.CHANNEL_IN_MONO
        AudioChannels.STEREO -> AudioFormat.CHANNEL_IN_STEREO
    }

    val encoder = bitDepth.valueForAudioRecordApi

    val minBufSize = AudioRecord.getMinBufferSize(
        sampleRate, inputChannel, encoder
    )
    val bufSize = minBufSize * 4 // why 4?

    private val maxAmplitudeState = MutableStateFlow(0)

    private val maxAmplitudeExtractor = bitDepth.maxAmplitudeExtractorFactory()

    enum class State {
        RECORDING,
        PAUSED,
        STOPPED,
    }

    private val state = MutableStateFlow(State.RECORDING)

    @OptIn(ExperimentalAtomicApi::class)
    @SuppressLint("MissingPermission")
    override fun start() {
        val bufferPoolSize = 4

        // queue of buffers
        val buffersPull = Channel<ByteBuffer>(capacity = bufferPoolSize)
        repeat(bufferPoolSize) {
            buffersPull.trySend(ByteBuffer.allocateDirect(bufSize).order(ByteOrder.LITTLE_ENDIAN))
        }

        val readChunks = Channel<ByteBuffer>(capacity = bufferPoolSize)

        micReadingThread = coroutineScope.launch(ioDispatcher) {
            // producer
            launch {
                val audioRecord = AudioRecord(
                    audioSource,
                    sampleRate,
                    inputChannel,
                    encoder,
                    bufSize
                )

                audioRecord.startRecording()

                try {
                    while (true) {
                        // wait for first non-paused state
                        val state = state.first { it != State.PAUSED }

                        if (state == State.STOPPED) {
                            break
                        }

                        val buffer = buffersPull.receive()

                        // blocking read. non-blocking would
                        // return 0 if not ready, causing the loop
                        // to execute more often than needed
                        val bytesRead = audioRecord.read(
                            buffer, bufSize,
                        )

                        if (bytesRead == 0
                            || bytesRead == AudioRecord.ERROR_INVALID_OPERATION
                            || bytesRead == AudioRecord.ERROR_BAD_VALUE
                            || bytesRead == AudioRecord.ERROR_DEAD_OBJECT
                            || bytesRead == AudioRecord.ERROR
                        ) {
                            // return buffer
                            buffer.clear()
                            buffersPull.send(buffer)
                            continue
                        }

                        buffer.limit(bytesRead)

                        readChunks.send(buffer)

                        // todo: get rid of, move to different thread
                        //  fix speed too
                        extractAndRecordMaxAmplitude(buffer)
                    }
                } finally {
                    readChunks.close()

                    audioRecord.apply {
                        try {
                            stop()
                        } finally {
                            release()
                        }
                    }
                }
            }

            // consumer
            launch {
                val outStream = FileOutputStream(descriptor.fileDescriptor).also {
                    // leaving space for the header
                    it.channel.position(WAV_HEADER_LENGTH_BYTES.toLong())
                }
                var bytesRecorded = 0

                // channel iterator. completes normally
                // when channel is closed + there is no more data in buffer
                try {
                    for (buffer in readChunks) {
                        outStream.channel.write(buffer)
                        bytesRecorded += buffer.limit()
                        buffer.clear()
                        buffersPull.send(buffer)
                    }
                } finally {
                    // maybe because of coroutine cancellation
                    // we just disregard buffers that still may be
                    // in the channel (though could try getting them out)

                    // try writing header and close.
                    // todo: do not swallow exception that can happen here?
                    outStream.use { outStream ->
                        outStream.channel.position(0) // back to the start to fill in the header
                        outStream.write(
                            generateWavHeader(
                                bytesRecorded = bytesRecorded,
                                numOfChannels = monoOrStereo.numberOfChannels().toShort(),
                                sampleRateHz = sampleRate,
                            )
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalAtomicApi::class)
    override suspend fun stop() {
        state.update { State.STOPPED }
        micReadingThread.join()
    }

    override fun pause() {
        state.update { State.PAUSED }
    }

    override fun resume() {
        state.update { State.RECORDING }
    }

    private fun generateWavHeader(
        bytesRecorded: Int,
        numOfChannels: Short,
        sampleRateHz: Int,
    ): ByteArray {

        // todo: redo this header with bit shifts

        val header = ByteArray(WAV_HEADER_LENGTH_BYTES)

        /*
         *   [Master RIFF chunk]
         */
        // "RIFF".toByteArray().copyInto(header, destinationOffset = 0) // 4 bytes
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()


        val fileSizeMinus8Bytes = bytesRecorded + WAV_HEADER_LENGTH_BYTES - 8

        //fileSizeMinus8Bytes.toLittleEndianByteArray()
        //    .copyInto(header, destinationOffset = 4) // 4 bytes

        header[4] = (fileSizeMinus8Bytes and 0xff).toByte()
        header[5] = (fileSizeMinus8Bytes shr 8 and 0xff).toByte()
        header[6] = (fileSizeMinus8Bytes shr 16 and 0xff).toByte()
        header[7] = (fileSizeMinus8Bytes shr 24 and 0xff).toByte()

        //"WAVE".toByteArray().copyInto(header, destinationOffset = 8) // 4 bytes
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()

        /*
         *   [Chunk describing the data format]
         */
        // "fmt ".toByteArray().copyInto(header, destinationOffset = 12) // 4 bytes
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()

        //val sizeOfDataChunkWithoutFirstTwoFields = 16
        //sizeOfDataChunkWithoutFirstTwoFields
        //    .toLittleEndianByteArray()
        //    .copyInto(header, destinationOffset = 16) // 4 bytes
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0

        //val audioFormat: Short = 1 // 1 - pcm int, 3 - IEEE 754 float
        // audioFormat
        //    .toLittleEndianByteArray()
        //    .copyInto(header, 20) // 2 bytes
        header[20] = if (bitDepth.isFloat) 3 else 1 // 1 - pcm int, 3 - IEEE 754 float
        header[21] = 0

        // numOfChannels
        //   .toLittleEndianByteArray()
        //     .copyInto(header, 22) // 2 bytes
        header[22] = numOfChannels.toByte()
        header[23] = 0

        //sampleRateHz
        //     .toLittleEndianByteArray()
        //    .copyInto(header, 24) // 4 bytes
        header[24] = (sampleRateHz and 0xff).toByte()
        header[25] = (sampleRateHz shr 8 and 0xff).toByte()
        header[26] = 0 // could >> 16, but it will never be more than 48000 which fits in 2 bytes
        header[27] = 0

        val bitsPerSample: Short = bitDepth.bitsPerSample
        val bytesPerBlock: Short = ((numOfChannels * bitsPerSample) / 8).toShort() // max 4
        val bytesPerSecond: Int = bytesPerBlock * sampleRateHz // max 4 * 48_000 = ?

        //bytesPerSecond
        //   .toLittleEndianByteArray()
        //    .copyInto(header, destinationOffset = 28) // 4 bytes
        header[28] = (bytesPerSecond and 0xff).toByte()
        header[29] = (bytesPerSecond shr 8 and 0xff).toByte()
        header[30] = (bytesPerSecond shr 16 and 0xff).toByte()
        header[31] = (bytesPerSecond shr 24 and 0xff).toByte()

        //bytesPerBlock
        //    .toLittleEndianByteArray()
        //    .copyInto(header, destinationOffset = 32) // 2 bytes
        header[32] = bytesPerBlock.toByte() // fits into 1 byte even though 2 are given here
        header[33] = 0


        // bitsPerSample
        //    .toLittleEndianByteArray()
        //    .copyInto(header, destinationOffset = 34) // 2 bytes
        header[34] = bitsPerSample.toByte()
        header[35] = 0  // fits into 1 byte even though 2 are given here


        /*
         *   [Chunk containing the sampled data]
         */
        // "data".toByteArray()
        //   .copyInto(header, destinationOffset = 36) // 4 bytes
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()


        //bytesRecorded
        //    .toLittleEndianByteArray()
        //    .copyInto(header, destinationOffset = 40) // 4 bytes
        header[40] = (bytesRecorded and 0xff).toByte()
        header[41] = (bytesRecorded shr 8 and 0xff).toByte()
        header[42] = (bytesRecorded shr 16 and 0xff).toByte()
        header[43] = (bytesRecorded shr 24 and 0xff).toByte()


        return header
    }

    private val bitsPerSample =
        bitDepth.bitsPerSample // for now 16_BIT // means 16 bits per one sample. If stereo, there are going to be 2 samples for left and right for a total of 32 bits (4 bytes)

    // bytes per one sample, if stereo that would be only left or only right channel sample
    private val bytesPerSample = (bitsPerSample / 8)

    // by instant i mean 1 sample if it is mono or 2 samples (left and right) from one instant in time, if it is stereo
    private val bytesPerInstant = bytesPerSample * monoOrStereo.numberOfChannels()


    // this is happening in a non-main thread that reads bytes from mic
    private fun extractAndRecordMaxAmplitude(pcmBytes: ByteBuffer) {
        val valueForThisBuffer = maxAmplitudeExtractor.extractFrom(
            buffer = pcmBytes,
            numberOfChannels = monoOrStereo.numberOfChannels()
        )

        maxAmplitudeState.update { currentValue ->
            max(currentValue, valueForThisBuffer)
        }
    }

    override fun maxAmplitude(): Int {
        // return old value and set new value to 0
        return maxAmplitudeState.getAndUpdate { 0 }
    }

    override fun supportsPausing() = true

}
