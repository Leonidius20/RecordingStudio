package io.github.leonidius20.recorder.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.R
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Settings @Inject constructor(
    @ApplicationContext private val context: Context,
): OnSharedPreferenceChangeListener {

    data class SettingsState(
        val stopOnLowBattery: Boolean,
        val stopOnLowStorage: Boolean,
        val pauseOnCall: Boolean,
    )

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    val state = MutableStateFlow(getCurrentSettingsState())

    init {
        pref.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        state.value = getCurrentSettingsState()
    }

    private fun getCurrentSettingsState(): SettingsState {
        return SettingsState(
            stopOnLowBattery = pref.getBoolean(
                context.getString(R.string.stop_on_low_battery_pref_key),
                context.resources.getBoolean(R.bool.stop_on_low_battery_default)),
            stopOnLowStorage = pref.getBoolean(
                context.getString(R.string.stop_on_low_storage_pref_key),
                context.resources.getBoolean(R.bool.stop_on_storage_default)),
            pauseOnCall = pref.getBoolean(
                context.getString(R.string.pause_on_call_pref_key),
                context.resources.getBoolean(R.bool.pause_on_call_default)),
        )
    }


}