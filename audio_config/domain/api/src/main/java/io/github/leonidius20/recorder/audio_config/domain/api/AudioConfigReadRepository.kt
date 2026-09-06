package io.github.leonidius20.recorder.audio_config.domain.api

import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import kotlinx.coroutines.flow.StateFlow

interface AudioConfigReadRepository {

    val state: StateFlow<SettingsState<*>>

}
