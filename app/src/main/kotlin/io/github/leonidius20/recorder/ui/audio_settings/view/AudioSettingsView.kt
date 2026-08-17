package io.github.leonidius20.recorder.ui.audio_settings.view

import com.arkivanov.mvikotlin.core.view.MviView
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore

interface AudioSettingsView : MviView<AudioSettingsStore.State, AudioSettingsStore.Intent>
