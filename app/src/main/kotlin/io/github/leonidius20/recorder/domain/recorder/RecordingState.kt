package io.github.leonidius20.recorder.domain.recorder

sealed interface RecordingState {

    data object Preparing : RecordingState

    data class Recording(
        val supportsPausing: Boolean,
    ) : RecordingState

    data object Paused : RecordingState

    data object Error : RecordingState

    data object Stopping : RecordingState

    data object Idle : RecordingState
}
