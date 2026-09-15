package io.github.leonidius20.recorder.data.recorder

import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.annotation.RequiresApi
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.recorder.domain.recorder.AudioRecorder
import java.io.IOException

/**
 * Wraps MediaRecorder so that it implements our AudioRecorder interface
 */
class MediaRecorderWrapper @Throws(IOException::class) constructor(
    audioSource: Int,
    container: Container,
    descriptor: ParcelFileDescriptor,
    encoder: Codec<*>,
    channels: AudioChannels,
    sampleRate: Int,
    /**
     * bit rate in kbps
     */
    bitRate: Float?,
) : AudioRecorder {

    private val encoderValue = encoder.value

    private val recorder = MediaRecorder().apply {
        setAudioSource(audioSource)
        setOutputFormat(container.value)
        setOutputFile(descriptor.fileDescriptor)
        setAudioEncoder(encoderValue)
        setAudioChannels(channels.numberOfChannels())
        setAudioSamplingRate(sampleRate)
        if (bitRate != null) {
            setAudioEncodingBitRate((bitRate * 1000).toInt()) // 1 kbps = 1000 bps
        }

        /*setOnInfoListener(object : MediaRecorder.OnInfoListener {
            override fun onInfo(
                mr: MediaRecorder?,
                what: Int,
                extra: Int
            ) {

            }

        })*/

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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && encoderValue == MediaRecorder.AudioEncoder.OPUS) {
                // to avoid a bug where it hangs after stopping when paused
                resume()
            }
            stop()
            release()
        }
    }

    override fun maxAmplitude() = recorder.maxAmplitude // 20000 is max value

    override fun supportsPausing() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

}
