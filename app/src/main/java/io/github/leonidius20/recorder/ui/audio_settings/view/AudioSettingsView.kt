package io.github.leonidius20.recorder.ui.audio_settings.view

import com.arkivanov.mvikotlin.core.view.MviView
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Label
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsView.Model
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsView.Event

interface AudioSettingsView : MviView<Model, Event> {

    data class Model(
        val name: String = ""
    )

    sealed interface Event {

    }

    fun handleLabel(label: Label)

}
