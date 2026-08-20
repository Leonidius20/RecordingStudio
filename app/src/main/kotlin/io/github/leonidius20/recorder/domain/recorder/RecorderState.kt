package io.github.leonidius20.recorder.domain.recorder

enum class RecorderState {
    IDLE,
    RECORDING,
    PAUSED,
    ERROR, // todo: wrap throwable in it?
}
