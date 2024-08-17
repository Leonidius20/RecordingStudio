package io.github.leonidius20.recorder.ui.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.settings.Settings
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    @Inject
    lateinit var settings: Settings

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<SwitchPreferenceCompat>(getString(R.string.pause_on_call_pref_key)).run {

            // MediaRecorder doesn't support pausing before N.
            // once we re-implement recording with AudioRecord + MediaCodec, we can
            // remove this
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                this?.isChecked = false
                this?.isEnabled = false
            }


        }
    }

    override fun onResume() {
        super.onResume()
        preferenceManager.sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        preferenceManager.sharedPreferences!!.unregisterOnSharedPreferenceChangeListener(this)
        super.onPause()
    }

    override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        settings.onSharedPreferenceChanged(key, this)
        // refresh the "pause on call" setting in case it was disabled bc the user didn't grant permissions
        findPreference<SwitchPreferenceCompat>(getString(R.string.pause_on_call_pref_key))!!.isChecked = settings.state.value.pauseOnCall
    }


}