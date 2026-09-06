package io.github.leonidius20.recorder.audio_config.domain.impl

data class AudioSourceOption(
    /**
     * value expected by MediaRecorder.setAudioSource().
     * todo remove from here, move to data or ui or something
     */
    val value: Int,
    val name: String,
    val description: String,
) {

    companion object

}
