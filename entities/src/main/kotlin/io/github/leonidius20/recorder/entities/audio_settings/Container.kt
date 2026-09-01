package io.github.leonidius20.recorder.entities.audio_settings

// todo: replace w/ sealed interface?
class Container(
    val id: ContainerId,
    /**
     * value as expected by MediaRecorder.setOutputFormat()
     */
    val value: Int, // todo: cannot be lifted to domain. todo: have a ContainerId here and in data have a mapper to mediarec values
    val displayName: String, // todo: also remove, map in data instead
    val mimeType: String,
    /**
     * determined based on https://developer.android.com/media/platform/supported-formats
     */
    val supportedCodecIds: List<CodecId>, // does not take into account what the device supports
) {

    companion object

}
