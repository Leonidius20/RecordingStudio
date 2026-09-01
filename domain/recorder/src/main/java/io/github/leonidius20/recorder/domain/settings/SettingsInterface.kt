package io.github.leonidius20.recorder.domain.settings

import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import kotlinx.coroutines.flow.StateFlow

interface SettingsInterface {

    val state: StateFlow<SettingsState<*>>

}
