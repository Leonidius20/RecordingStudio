package io.github.leonidius20.recorder.data.recorder

import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.annotation.RequiresApi
import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
import java.io.IOException

/**
 * Wraps MediaRecorder so that it implements our AudioRecorder interface
 */
class MediaRecorderWrapper @Throws(IOException::class) constructor(
    audioSource: Int,
    container: Container,
    descriptor: ParcelFileDescriptor,
    encoder: Codec,
    channels: AudioChannels,
    sampleRate: Int,
    /**
     * bit rate in kbps
     */
    bitRate: Float?,
) : AudioRecorder {

    val recorder = MediaRecorder().apply {
        setAudioSource(audioSource)
        setOutputFormat(container.value)
        setOutputFile(descriptor.fileDescriptor)
        setAudioEncoder(encoder.value)
        setAudioChannels(channels.numberOfChannels())
        //todo uncomment once bit rate is implemented
        setAudioSamplingRate(sampleRate)
        if (bitRate != null) {
            setAudioEncodingBitRate((bitRate * 1000).toInt()) // 1 kbps = 1000 bps
        }

        //setAudioEncodingBitRate()
        /*setOnInfoListener(object : MediaRecorder.OnInfoListener {
            override fun onInfo(
                mr: MediaRecorder?,
                what: Int,
                extra: Int
            ) {

            }

        })*/
        // setAudioEncodingBitRate() // in bits per s

        prepare() // throws IOException
    }

    override fun start() {
        recorder.start()
    }


    @RequiresApi(Build.VERSION_CODES.N)
    override fun pause() {
        recorder.pause()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun resume() {
        recorder.resume()
    }

    override suspend fun stop() {
        recorder.apply {
            stop()
            release()
        }
    }

    override fun maxAmplitude() = recorder.maxAmplitude // 20000 is max value

    override fun supportsPausing() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

}