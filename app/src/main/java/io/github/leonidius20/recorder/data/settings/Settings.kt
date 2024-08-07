package io.github.leonidius20.recorder.data.settings

import android.content.Context
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Settings @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    data class SettingsState(
        val stopOnLowBattery: Boolean,
        val stopOnLowStorage: Boolean,
        val pauseOnCall: Boolean,
    )

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    private val _state = MutableStateFlow(getCurrentSettingsState())

    val state = _state.asStateFlow()

    private val pauseOnCallKey = context.getString(R.string.pause_on_call_pref_key)

    fun onSharedPreferenceChanged(
        key: String?, fragment: PreferenceFragmentCompat,
    ) {
        _state.value = getCurrentSettingsState()

        // if pausing on incoming call was just enabled
        if (key == pauseOnCallKey && state.value.pauseOnCall) {
            // check or get call monitoring permission
            PermissionX.init(fragment)
                .permissions(android.Manifest.permission.READ_PHONE_STATE)
                .onExplainRequestReason { scope, deniedList ->
                    scope.showRequestReasonDialog(deniedList,
                        message = fragment.getString(R.string.phone_state_permission_rationale),
                        positiveText = fragment.getString(android.R.string.ok)
                    )
                }.onForwardToSettings { scope, deniedList ->
                    scope.showForwardToSettingsDialog(deniedList,
                        message = fragment.getString(R.string.permissions_rationale_grant_in_settings, fragment.getString(R.string.phone_state_permission_rationale)),
                        positiveText = fragment.getString(android.R.string.ok),
                        negativeText = fragment.getString(android.R.string.cancel)
                    )
                }.request { allGranted: Boolean, grantedList, deniedList ->
                    if (!allGranted) {
                        // disable the setting
                        pref.edit().putBoolean(pauseOnCallKey, false).apply()
                    }
                }
        }
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