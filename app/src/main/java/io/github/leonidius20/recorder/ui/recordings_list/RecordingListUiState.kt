package io.github.leonidius20.recorder.ui.recordings_list

data class RecordingListUiState(
    val recordings: ArrayList<RecordingListItemUiState>,
) {

    val isContextActionsBarShown
        get() = recordings.any { it.isSelected }

    companion object {
        fun default() = RecordingListUiState(
            recordings = ArrayList()
        )
    }


}

data class RecordingListItemUiState(
    val id: Long,
    val name: String,
    val duration: String,
    val size: String,
    val isSelected: Boolean,
    val isPlaying: Boolean,
)