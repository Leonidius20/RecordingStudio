package io.github.leonidius20.recorder.audio_config.presentation.view

import com.arkivanov.mvikotlin.core.view.MviView
import io.github.leonidius20.recorder.audio_config.presentation.store.AudioSettingsStore

interface AudioSettingsView : MviView<AudioSettingsStore.State, AudioSettingsStore.Intent>
