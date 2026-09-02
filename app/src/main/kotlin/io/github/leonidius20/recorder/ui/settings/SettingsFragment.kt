package io.github.leonidius20.recorder.ui.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.permissionx.guolindev.PermissionX
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.BuildConfig
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.domain.settings.UserSettingsReadRepository
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    @Inject
    lateinit var settings: UserSettingsReadRepository

    @Inject
    lateinit var importHandler: ImportFileSettingHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        importHandler.attach(this@SettingsFragment)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        findPreference<SwitchPreferenceCompat>(getString(R.string.pause_on_call_pref_key))?.run {

            // MediaRecorder doesn't support pausing before N.
            // once we re-implement recording with AudioRecord + MediaCodec, we can
            // remove this
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                this.isChecked = false
                this.isEnabled = false
            }

            // todo: check if the permission was not removed
            //  bc the app was accessed too long ago? or better yet
            //   reimpl with audio context

            onPreferenceChangeListener = Preference.OnPreferenceChangeListener { pref, newValue ->
                if (newValue == false) return@OnPreferenceChangeListener true

                // make sure we have the permission to monitor calls.
                // todo: re-implement with audio focus and we will not need the permission

                val permissionCheck = ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.READ_PHONE_STATE
                )

                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    true
                } else {
                    PermissionX.init(this@SettingsFragment)
                        .permissions(android.Manifest.permission.READ_PHONE_STATE)
                        .onExplainRequestReason { scope, deniedList ->
                            scope.showRequestReasonDialog(
                                deniedList,
                                message = getString(R.string.phone_state_permission_rationale),
                                positiveText = getString(android.R.string.ok)
                            )
                        }.onForwardToSettings { scope, deniedList ->
                            scope.showForwardToSettingsDialog(
                                deniedList,
                                message = getString(
                                    R.string.permissions_rationale_grant_in_settings,
                                    getString(R.string.phone_state_permission_rationale)
                                ),
                                positiveText = getString(android.R.string.ok),
                                negativeText = getString(android.R.string.cancel)
                            )
                        }.request { allGranted: Boolean, grantedList, deniedList ->


                            if (allGranted) {
                                // enable the setting
                                preferenceManager.sharedPreferences?.edit {
                                    putBoolean(getString(R.string.pause_on_call_pref_key), true)
                                }
                                findPreference<SwitchPreferenceCompat>(getString(R.string.pause_on_call_pref_key))!!.isChecked = true
                            }
                        }

                    false // do not enable until permission is granted
                }
            }
        }

        if (BuildConfig.FLAVOR == "full") {
            /*Preference(requireContext()).apply {
                title = "View installed plugins"
                setOnPreferenceClickListener { _ ->
                    findNavController().navigate(
                        SettingsFragmentDirections.actionNavSettingsToPluginsList()
                    )
                    true
                }
                preferenceScreen.addPreference(this)
            }*/
            Preference(requireContext()).apply {
                title = "Import file"
                setOnPreferenceClickListener { _ ->
                    importHandler.launchImport()
                    true
                }
                preferenceScreen.addPreference(this)
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

    }

}
