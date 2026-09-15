package io.github.leonidius20.recorder.audio_config.data.repository

import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import kotlinx.coroutines.flow.Flow

interface AudioSettingsDataSource {
    val settings: Flow<SettingsState<out BitRateSettingType>>

    /**
     * no validation, just reading
     */
    fun getCurrentSettingsState(): SettingsState<*>

    /**
     * no validation, just writing
     */
    // todo: make it async and datastore
    fun saveSettingsToDisk(settings: SettingsState<*>)
}
