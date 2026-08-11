package io.github.leonidius20.recorder.ui.audio_settings.view_impl

import com.arkivanov.mvikotlin.core.view.BaseMviView
import io.github.leonidius20.recorder.databinding.BottomSheetAudioSettingsBinding
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsView
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsView.Model
import io.github.leonidius20.recorder.ui.audio_settings.view.AudioSettingsView.Event

class AudioSettingsViewImpl(
    val binding: BottomSheetAudioSettingsBinding,
    val fragment: AudioSettingsBottomSheet,
) : BaseMviView<Model, Event>(), AudioSettingsView {

    override fun handleLabel(label: AudioSettingsStore.Label) {

    }

}
