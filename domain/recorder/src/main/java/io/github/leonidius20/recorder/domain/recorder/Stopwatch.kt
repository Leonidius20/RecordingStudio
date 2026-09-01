package io.github.leonidius20.recorder.domain.recorder

import kotlinx.coroutines.flow.StateFlow

interface Stopwatch {

    val timer: StateFlow<Long>

    fun start()

    fun stop()

    fun pause()

    fun resume()

    /** prepare for next run **/
    fun clear()

}
