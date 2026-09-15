package io.github.leonidius20.recorder.ui.recordings_list.view

data class RecordingUiModel(
    val id: Long,
    val name: String,
    val duration: String,
    val size: String,
    val isSelected: Boolean,
    val isPlaying: Boolean,
)
