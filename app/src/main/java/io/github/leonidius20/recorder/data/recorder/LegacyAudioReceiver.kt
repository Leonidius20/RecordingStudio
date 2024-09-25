package io.github.leonidius20.recorder.data.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.util.Log
import io.github.leonidius20.recorder.data.settings.AudioChannels
import java.io.FileOutputStream
import kotlin.concurrent.thread

private const val WAV_HEADER_LENGTH_BYTES = 44

class LegacyAudioReceiver(
    private val descriptor: ParcelFileDescriptor,
    private val audioSource: Int = MediaRecorder.AudioSource.MIC,
    private val sampleRate: Int = 44_100, // Sampling rates for raw PCM recordings at 8000, 16000 and 44100 Hz.
    private val monoOrStereo: AudioChannels = AudioChannels.MONO
) : AudioRecorder {


    private lateinit var audioRecord: AudioRecord

    @Volatile
    private var isRecording = false

    @Volatile
    private lateinit var micReadingThread: Thread

    @Volatile
    private var bytesRecorded = 0


    private val inputChannel = when (monoOrStereo) {
        AudioChannels.MONO -> AudioFormat.CHANNEL_IN_MONO
        AudioChannels.STEREO -> AudioFormat.CHANNEL_IN_STEREO
    }

    val encoder = AudioFormat.ENCODING_PCM_16BIT

    val minBufSize = AudioRecord.getMinBufferSize(
        sampleRate, inputChannel, encoder
    )
    val bufSize = minBufSize * 4 // why 4?


    @SuppressLint("MissingPermission")
    override fun start() {

        audioRecord = AudioRecord(
            audioSource,
            sampleRate,
            inputChannel,
            encoder,
            bufSize
        )

        audioRecord.startRecording(/*null*/) // todo: mediaSyncEvent
        isRecording = true



        micReadingThread = thread(start = true) {
            val outStream = FileOutputStream(descriptor.fileDescriptor).also {
                // leaving space for the header
                it.channel.position(WAV_HEADER_LENGTH_BYTES.toLong())
            }


            //android.system.Os.lseek(descriptor.fileDescriptor, WAV_HEADER_LENGTH_BYTES.toLong(), OsConstants.SEEK_SET)
            // leaving space for header

            //var bytesRecorded = 0

            // outStream.write(ByteArray(WAV_HEADER_LENGTH_BYTES)) // placeholder for wav header
            val buffer = ByteArray(bufSize)
            while (isRecording) {
                val bytesRead = audioRecord.read(buffer, 0, bufSize)
                if (bytesRead == 0
                    || bytesRead == AudioRecord.ERROR_INVALID_OPERATION
                    || bytesRead == AudioRecord.ERROR_BAD_VALUE
                    || bytesRead == AudioRecord.ERROR_DEAD_OBJECT
                    || bytesRead == AudioRecord.ERROR
                ) {
                    continue
                }
                outStream.write(buffer.sliceArray(0 until bytesRead))
                bytesRecorded += bytesRead
            }

            // going back to add header
            //android.system.Os.lseek(descriptor.fileDescriptor, 0, OsConstants.SEEK_SET)
            /*outStream.write(generateWavHeader(
        numOfChannels = 1, // todo
        sampleRateHz = sampleRate,
    ))*/

            /*outStream.channel.apply {
        position(0) // back to start where we left 44 bytes for header
        write(
            ByteBuffer.wrap(
                generateWavHeader(
                    numOfChannels = 1, // todo
                    sampleRateHz = sampleRate,
                )
            )
        )
    }*/

            //outStream.close()

            outStream.channel.position(0) // back to the start to fill in the header
            outStream.write(
                generateWavHeader(
                    bytesRecorded = bytesRecorded,
                    numOfChannels = monoOrStereo.numberOfChannels().toShort(),
                    sampleRateHz = sampleRate,
                )
            )
            //Log.d("audio rec", "wrote header")

            outStream.close()


        }

    }

    override fun stop() {
        isRecording = false
        audioRecord.stop()
        audioRecord.release()


        // micReadingThread.join()


        /*tempFile.inputStream().use { input ->
            Log.d("audio rec", "before writing out file")
            // val data = ByteArray(bufSize)
            nonTempOutStream.write(
                input.readBytes()
            )
            Log.d("audio rec", "after writing out file")
        }

        Log.d("audio rec", "after closing temp file")
        nonTempOutStream.close()
        Log.d("audio rec", "after closing perm out file")

        tempFile.delete()
        Log.d("audio rec", "after deleting temp file")*/

        // micReadingJob.cancelAndJoin() // we should re-do it with coroutines and make sure ServiceScope doesn't die until all coroutines inside are finished
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
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

        //val audioFormat: Short = 1 // 1 - pcm int, 3 - IEEE 754 float todo
        // audioFormat
        //    .toLittleEndianByteArray()
        //    .copyInto(header, 20) // 2 bytes
        header[20] = 1
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

        val bitsPerSample: Short = 16 // todo: other formats support
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

}