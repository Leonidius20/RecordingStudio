package io.github.leonidius20.recorder.ui.crash

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.R
import javax.inject.Inject

@HiltViewModel
class CrashViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
): ViewModel() {

    private val _isStacktraceCopied = MutableLiveData(false)
    val isStacktraceCopied: LiveData<Boolean> = _isStacktraceCopied

    private fun notifyStacktraceCopied() {
        _isStacktraceCopied.value = true
    }

    fun copyToClipboard(crashData: String) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Rec Studio crash data", crashData)
        clipboard.setPrimaryClip(clip)

        notifyStacktraceCopied()
    }

    fun launchGithubInBrowser() {
        val url = context.getString(R.string.github_bug_reporting_url)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

}