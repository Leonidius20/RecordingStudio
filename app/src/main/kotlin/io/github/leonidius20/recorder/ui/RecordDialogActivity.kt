package io.github.leonidius20.recorder.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.R

/**
 * an activity that is launched when some other app requests to record an audio.
 * shown as a dialog
 */
@AndroidEntryPoint
class RecordDialogActivity: AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.record_dialog)

    }

}