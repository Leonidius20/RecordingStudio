package io.github.leonidius20.recorder.ui.settings

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.BuildConfig
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.import_file.NativeBridge
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.Settings
import javax.inject.Inject
import javax.inject.Provider

@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var recordingsListRepository: RecordingsListRepository // todo: remove

    @Inject
    lateinit var nativeBridge: Provider<NativeBridge>

    val launchFileSelector = registerForActivityResult(ActivityResultContracts.OpenDocument()) { selectedUri ->
        if (selectedUri == null) return@registerForActivityResult
        val resolver = requireContext().contentResolver
        val mime = resolver.getType(selectedUri) ?: "audio/ogg"
        val copyUri = recordingsListRepository.createRecordingFile("imported_file", mime)

        val sourceFd = resolver.openFileDescriptor(selectedUri, "r")!!
        val destFd = resolver.openFileDescriptor(copyUri, "w")!!

        try {
            val result = nativeBridge.get().copyFile(sourceFd.fd, destFd.fd)
            Toast.makeText(requireContext(), if (result) "success" else "fail", Toast.LENGTH_SHORT).show()
        } catch (t: Throwable) {
            t.printStackTrace() // todo: replace with timber or crash reporting
            Toast.makeText(requireContext(), "fail (error)", Toast.LENGTH_SHORT).show()
        } finally {
            sourceFd.close()
            destFd.close()
        }

        //val bytesCopied = resolver.openOutputStream(copyUri, "rw")!!.use { out ->
        //    resolver.openInputStream(selectedUri)!!.use { input ->
        //        input.copyTo(out)
        //    }
        //}
        //resolver.update(copyUri, ContentValues().apply {
        //    put(MediaStore.MediaColumns.SIZE, bytesCopied)
        //}, null, null)
    }

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
                    launchFileSelector.launch(arrayOf("audio/*"))
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
        settings.onSharedPreferenceChanged(key, this)
        // refresh the "pause on call" setting in case it was disabled bc the user didn't grant permissions
        findPreference<SwitchPreferenceCompat>(getString(R.string.pause_on_call_pref_key))!!.isChecked = settings.state.value.pauseOnCall
    }


}