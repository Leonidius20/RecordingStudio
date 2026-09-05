package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build

data class AudioSourceOption(
    /**
     * value expected by MediaRecorder.setAudioSource()
     */
    val value: Int,
    val name: String,
    val description: String,
) {

    companion object {
        val DEFAULT = AudioSourceOption(
            MediaRecorder.AudioSource.DEFAULT,
            "Default",
            "Default audio input. Some processing may be applied by device"
        )
    }

}

val audioSourceOptions = buildList {
    // todo: localize
    addAll(
        listOf(
            AudioSourceOption.DEFAULT,
            AudioSourceOption(
                MediaRecorder.AudioSource.MIC,
                "Mic",
                "Regular microphone input (some processing may be applied by device)"
            ),
            AudioSourceOption(
                MediaRecorder.AudioSource.CAMCORDER,
                "Camcorder",
                "Input tuned for video recording. If there are many microphones, this would be the one with the same orientation as the camera"
            ),
            AudioSourceOption(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                "Voice recognition",
                "Tuned for voice recognition"
            ),
            AudioSourceOption(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                "Voice communication",
                "Tuned for VoIP and the like. Applies processing like echo cancellation or gain control (determined by device manufacturer)"
            ),
        )
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        add(
            AudioSourceOption(
                MediaRecorder.AudioSource.UNPROCESSED,
                "Unprocessed",
                "No processing if the phone supports it, default otherwise"
            )
        )
    }
}
