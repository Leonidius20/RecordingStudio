package io.github.leonidius20.recorder.ui.recordings_list

sealed interface RecordingsListAction {

    data class SelectItem(val position: Int): RecordingsListAction

    data class DeselectItem(val position: Int): RecordingsListAction

    data class DeleteItem(val position: Int): RecordingsListAction

}