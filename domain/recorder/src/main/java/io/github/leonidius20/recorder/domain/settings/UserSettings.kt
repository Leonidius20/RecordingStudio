package io.github.leonidius20.recorder.domain.settings

import kotlinx.coroutines.flow.StateFlow

data class UserSettings(
    val stopOnLowBattery: Boolean = true,
    val stopOnLowStorage: Boolean = true,
    val pauseOnCall: Boolean = false,
)

interface UserSettingsReadRepository {

    val userSettings: StateFlow<UserSettings>

}
