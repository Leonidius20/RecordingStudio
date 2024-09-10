package io.github.leonidius20.recorder.ui.home

import androidx.core.content.FileProvider
import io.github.leonidius20.recorder.R

/**
 * this provider is used to provide recording files to external apps that
 * request a recording to be made for them by using an intent with RECORD_SOUND action
 */
class RecordingFileProvider : FileProvider(R.xml.file_provider_paths)