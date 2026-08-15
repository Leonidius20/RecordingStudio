package io.github.leonidius20.recorder.ui.audio_settings.view

import com.arkivanov.mvikotlin.core.view.MviView
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Label

interface AudioSettingsView : MviView<AudioSettingsStore.State, AudioSettingsStore.Intent> {

    fun handleLabel(label: Label)

}
