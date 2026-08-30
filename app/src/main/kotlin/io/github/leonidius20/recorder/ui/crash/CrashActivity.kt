package io.github.leonidius20.recorder.ui.crash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cat.ereza.customactivityoncrash.CustomActivityOnCrash
import cat.ereza.customactivityoncrash.config.CaocConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.databinding.CrashDialogBinding
import kotlinx.coroutines.launch

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

        val dialogViewBinding = CrashDialogBinding.inflate(layoutInflater).apply {
            crashDismissBtn.setOnClickListener { closeApp() }
            crashRestartBtn.setOnClickListener { restartApp() }
            crashGithubBtn.setOnClickListener { goToGithub() }
            crashCopyStacktraceBtn.setOnClickListener { copyStacktrace() }
        }

        dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.app_name_short)
            .setMessage(R.string.crash_explanation)
            .setCancelable(false)
            .setView(dialogViewBinding.root)
            .show()

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isStacktraceCopied.collect { copied ->
                    dialogViewBinding.crashCopyStacktraceBtn.apply {
                        isEnabled = !copied
                        setText(if (copied) R.string.crash_stacktrace_copied else R.string.crash_copy_stacktrace)
                        setIconResource(if (copied) R.drawable.ic_done else R.drawable.ic_copy)
                    }
                }
            }
        }
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
