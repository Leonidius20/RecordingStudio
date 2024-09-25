package io.github.leonidius20.recorder.data.recorder

interface AudioRecorder {

    fun start()

    fun pause()

    fun resume()

    fun stop()

    /**
     * max amplitude sampled since the last call to this method
     */
    fun maxAmplitude(): Int

    enum class State {
        IDLE,
        RECORDING,
        PAUSED,
        ERROR,
    }

}