package io.github.leonidius20.recorder.domain.recorder

import kotlinx.coroutines.flow.StateFlow

interface AudioRecorder {

    // val state: StateFlow<RecorderState>

    fun start()

    fun pause()

    fun resume()

    /**
     * Stop the recording, kill the recorder, make sure file is saved, clean
     * resources up. It is a suspend function because it may take a while to
     * finish all these tasks, incl. waiting for any threads to finish,
     * and the Service can only be safely killed after it finishes
     */
    suspend fun stop()

    /**
     * max amplitude sampled since the last call to this method
     */
    fun maxAmplitude(): Int

    fun supportsPausing(): Boolean

}

interface PausableAudioRecorder : AudioRecorder {



}
