package io.github.leonidius20.recorder.ui.recordings_list.view

import android.net.Uri

data class RecordingUiModel(
    val id: Long,
    val name: String,
    val duration: String,
    val size: String,
    val uri: Uri,
    // val dateTaken: String,
    // val mimeType: String,
    val isSelected: Boolean,
    val isPlaying: Boolean, // todo: refactor for viewmodel to hold info about items that are being played
)
