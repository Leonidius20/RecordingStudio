package io.github.leonidius20.recorder.domain.recorder

import com.yashovardhan99.timeit.Stopwatch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class StopwatchWrapper @Inject constructor() :
    io.github.leonidius20.recorder.domain.recorder.Stopwatch {

    private val _timer = MutableStateFlow(0L)
    override val timer: StateFlow<Long>
        get() = _timer

    val stopwatch = Stopwatch()

    init {
        stopwatch.setOnTickListener {
            _timer.value = stopwatch.elapsedTime
        }
    }

    override fun start() {
        stopwatch.start()
    }

    override fun stop() {
        stopwatch.stop()
        _timer.value = stopwatch.elapsedTime
    }

    override fun pause() {
        stopwatch.pause()
    }

    override fun resume() {
        stopwatch.resume()
    }

    override fun clear() {
        _timer.value = 0L
    }

}
