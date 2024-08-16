package io.github.leonidius20.recorder.ui.crash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.CrashDialogBinding

@AndroidEntryPoint
class CrashActivity: AppCompatActivity() {

    private lateinit var config: CaocConfig

    val viewModel by viewModels<CrashViewModel>()

    private lateinit var dialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val configOrNull = CustomActivityOnCrash.getConfigFromIntent(intent)

        if (configOrNull == null) {
            //This should never happen - Just finish the activity to avoid a recursive crash.
            finish()
            return
        }

        config = configOrNull

        setContentView(R.layout.activity_crash)

        val dialogViewBinding = CrashDialogBinding.inflate(layoutInflater).also {
            it.activity = this

            it.lifecycleOwner = this
        }

        dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name_short)
            .setMessage(R.string.crash_explanation)
            .setCancelable(false)
            .setView(dialogViewBinding.root)
            .show()
    }

    /**
     * if user wants to ignore crash or chose to go to github
     */
    fun closeApp() {
        CustomActivityOnCrash.closeApplication(this, config)
    }

    fun restartApp() {
        CustomActivityOnCrash.restartApplication(this, config)
    }

    fun copyStacktrace() {
        viewModel.copyToClipboard(
            CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, intent)
        )
    }

    fun goToGithub() {
        viewModel.launchGithubInBrowser()
        closeApp()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialog.dismiss()
    }

}