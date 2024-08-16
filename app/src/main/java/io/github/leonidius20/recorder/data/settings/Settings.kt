package io.github.leonidius20.recorder.data.settings

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
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
        val audioSource: Int,
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
            audioSource = pref.getInt(
                context.getString(R.string.pref_audio_source_key),
                MediaRecorder.AudioSource.MIC,
            )
        )
    }

    data class AudioSourceOption(
        val value: Int,
        val name: String,
        val description: String,
    )

    private val _audioSourceOptions = mutableListOf(
        AudioSourceOption(MediaRecorder.AudioSource.DEFAULT, "Default", "Default audio input. Some processing may be applied by device"),
        AudioSourceOption(MediaRecorder.AudioSource.MIC, "Mic", "Regular microphone input (some processing may be applied by device)"),
        AudioSourceOption(MediaRecorder.AudioSource.CAMCORDER, "Camcorder", "Input tuned for video recording. If there are many microphones, this would be the one with the same orientation as the camera"),
        AudioSourceOption(MediaRecorder.AudioSource.VOICE_RECOGNITION, "Voice recognition", "Tuned for voice recognition"),
        AudioSourceOption(MediaRecorder.AudioSource.VOICE_COMMUNICATION, "Voice communication", "Tuned for VoIP and the like. Applies processing like echo cancellation or gain control (determined by device manufacturer)"),
    )

    val audioSourceOptions: List<AudioSourceOption>
        get() = _audioSourceOptions

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            _audioSourceOptions.add(AudioSourceOption(MediaRecorder.AudioSource.UNPROCESSED, "Unprocessed", "No processing if the phone supports it, default otherwise"))
        }
    }

    fun setAudioSource(value: Int) {
        pref.edit().putInt(
            context.getString(R.string.pref_audio_source_key),
            value
        ).apply()
    }

}