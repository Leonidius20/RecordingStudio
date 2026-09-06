package io.github.leonidius20.recorder.audio_config.domain.impl.options

interface DiscreteAudioSetting<T> {
    val id: Int

    val isSelected: Boolean

    // todo: create a mapper to get R.string value for this
    val displayName: String

    val option: T
}
