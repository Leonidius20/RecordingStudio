package io.github.leonidius20.recorder.data.recorder

interface AudioRecorder {

    fun start()

    fun pause()

    fun resume()

    fun stop()

    enum class State {
        IDLE,
        RECORDING,
        PAUSED,
        ERROR,
    }

}