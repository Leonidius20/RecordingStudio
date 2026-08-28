package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.data.common.di.Dispatcher
import io.github.leonidius20.recorder.data.common.di.Scope
import io.github.leonidius20.recorder.data.settings.SettingsInterface
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// todo: unit tests for business logic?
// todo: remove all the fucking notifications from here
@Singleton
class RecordAudioUseCase @Inject constructor(
    private val settings: SettingsInterface,
    @param:Scope.App private val scope: CoroutineScope,
    @param:Dispatcher.Main private val dispatcher: CoroutineDispatcher,
    private val notificationsManager: RecordingNotificationsManager, // todo: instead somewhere subscribe to states and update notifications accordingliy
    private val systemEventObserver: SystemEventObserver,
    private val outputFileFactory: OutputFileFactory,
    private val recorderFactory: AudioRecorderFactory,
    private val stopwatch: StopwatchInterface,
) {

    sealed interface State {

        data object Preparing : State

        data class Recording(
            val supportsPausing: Boolean,
        ) : State

        data object Paused : State

        data object Error : State

        data object Stopping : State  // new: for service to stop. todo: replace with IDLE?

        data object Idle : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State>
        get() = _state

    lateinit var file: OutputFile

    lateinit var recorder: AudioRecorder

    private lateinit var amplitudeVizUpdateJob: Job

    /**
     * length of the recording so far in milliseconds
     */
    val timer: StateFlow<Long>
        get() = stopwatch.timer

    private val _amplitudes = MutableSharedFlow<Int>(replay = 60)
    /**
     * emits max amplitude every 100ms. Used for audio visualization
     */
    val amplitudes = _amplitudes.asSharedFlow()

    private var watchSystemEventsJob: Job? = null

    private fun onSystemEvent(event: SystemEvent) {
        when(event) {
            SystemEvent.TOGGLE_REC_PAUSE -> {
                toggleRecPause()
            }
            SystemEvent.STOP -> {
                stop()
            }

            SystemEvent.LOW_STORAGE -> {
                if (settings.state.value.stopOnLowStorage) {
                    stopAbruptly("The device is running out of storage.")
                }
            }

            SystemEvent.LOW_BATTERY -> {
                if (settings.state.value.stopOnLowBattery) {
                    stopAbruptly(explanation = "The device is running out of battery.")
                }
            }

            SystemEvent.INCOMING_CALL -> {
                if (settings.state.value.pauseOnCall) {
                    pause()
                    notificationsManager.sendNotificationAboutPausingOnCall()
                }
            }
        }
    }

    /**
     * @return whether pausing is supported
     */
    fun start(): Boolean {
        _state.value = State.Preparing

        notificationsManager.createRecInProgressNotificationChannel()

        notificationsManager.createPrematureStopNotificationChannel()

        // used to control the recording from
        watchSystemEventsJob = scope.launch(dispatcher) {
            systemEventObserver.eventsFlow.collect(
                ::onSystemEvent
            )
        }

        // todo: this is what has to be lifted out. how do we
        //  stop depending on the
        val fileFormat = settings.state.value.outputFormat

        file = outputFileFactory.create(
            namePattern = "yyyy-MM-dd-HH-mm-ss",
            format = fileFormat
        ).apply {
            open()
        }

        try {
            recorder = recorderFactory.create(
                file
            )
        } catch (e: IOException) {
            e.printStackTrace()
            stopOnError()
            return false
        }

        recorder.start()

        _state.value = State.Recording(
            supportsPausing = recorder.supportsPausing()
        )

        stopwatch.start()


        amplitudeVizUpdateJob = scope.launch(Dispatchers.Default) {
            // every 100ms, emit maxAmplitude
            while (isActive) {
                if (state.value is State.Recording) {
                    _amplitudes.emit(recorder.maxAmplitude())
                    delay(100)
                } else {
                    // first() supposed to be cancellable?
                    state.first { it is State.Recording }
                }
            }
        }

        return recorder.supportsPausing()
    }

    fun stopOnError() {
        _state.value = State.Error
        // todo: have the service subscribe to state and
        //  kill itself on ERROR
        stopSelf()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun stopSelf() {
        _state.value = State.Stopping

        watchSystemEventsJob?.cancel()
        watchSystemEventsJob = null

        stopwatch.clear()
        // todo: maybe move .buffer() to viewmodel or wherever
        _amplitudes.resetReplayCache()

        file.updateMetadata(duration = timer.value)

        file.close()

        notificationsManager.cancelPausedOnIncomingCallNotification()
    }

    /**
     * @return the new state
     */
    fun toggleRecPause(): State {
        when (state.value) {
            is State.Recording -> {
                pause()
            }

            is State.Paused -> {
                resume()
            }

            else -> throw IllegalStateException()
        }
        return state.value
    }

    fun stop() {
        amplitudeVizUpdateJob.cancel()
        stopwatch.stop()

        scope.launch {
            recorder.stop()
            stopSelf()
        }
    }

    /**
     * called by service when it's destroyed normally or not
     */
    fun onDestroy() {
        _state.value = State.Idle
    }

    private fun pause() {
        recorder.pause()
        stopwatch.pause()
        _state.value = State.Paused
        notificationsManager.updateNotification(state.value, recorder.supportsPausing())
    }

    private fun resume() {
        recorder.resume()
        stopwatch.resume()
        _state.value = State.Recording(
            supportsPausing = recorder.supportsPausing()
        )

        notificationsManager.cancelPausedOnIncomingCallNotification()

        notificationsManager.updateNotification(state.value, recorder.supportsPausing())
    }

    private fun stopAbruptly(explanation: String) {
        notificationsManager.sendAbruptStopNotification(explanation)

        //launcher!!.onServiceStopped() // update ui state
        stop()
    }

}
