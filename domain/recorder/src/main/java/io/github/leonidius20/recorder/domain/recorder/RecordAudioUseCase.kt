package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.di.Dispatcher
import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import io.github.leonidius20.recorder.domain.settings.AudioConfigReadRepository
import io.github.leonidius20.recorder.domain.settings.UserSettingsReadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

// todo: unit tests for business logic?
// todo: remove all the fucking notifications from here
@Singleton
class RecordAudioUseCase @Inject constructor(
    private val settings: AudioConfigReadRepository,
    private val userSettings: UserSettingsReadRepository,
    @param:Scope.App private val scope: CoroutineScope,
    @param:Dispatcher.Main private val dispatcher: CoroutineDispatcher,
    @param:Dispatcher.Default private val defaultDispatcher: CoroutineDispatcher,
    private val notificationsManager: RecordingNotificationsManager, // todo: instead somewhere subscribe to states and update notifications accordingliy
    private val systemEventObserver: SystemEventObserver,
    private val outputFileFactory: OutputFileFactory,
    private val recorderFactory: AudioRecorderFactory,
    private val stopwatch: Stopwatch,
) {

    private val _state = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val state: StateFlow<RecordingState>
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
                if (userSettings.userSettings.value.stopOnLowStorage) {
                    stopAbruptly("The device is running out of storage.")
                }
            }

            SystemEvent.LOW_BATTERY -> {
                if (userSettings.userSettings.value.stopOnLowBattery) {
                    stopAbruptly(explanation = "The device is running out of battery.")
                }
            }

            SystemEvent.INCOMING_CALL -> {
                if (userSettings.userSettings.value.pauseOnCall) {
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
        _state.value = RecordingState.Preparing

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

        } catch (e: Throwable) {
            e.printStackTrace()
            stopOnError(e)
            return false
        }

        recorder.start()

        _state.value = RecordingState.Recording(
            supportsPausing = recorder.supportsPausing()
        )

        stopwatch.start()


        amplitudeVizUpdateJob = scope.launch(defaultDispatcher) {
            // every 100ms, emit maxAmplitude
            while (isActive) {
                if (state.value is RecordingState.Recording) {
                    _amplitudes.emit(recorder.maxAmplitude())
                    delay(100.milliseconds)
                } else {
                    // first() supposed to be cancellable?
                    state.first { it is RecordingState.Recording }
                }
            }
        }

        return recorder.supportsPausing()
    }

    fun stopOnError(t: Throwable) {
        _state.value = RecordingState.Error(t)

        // let the subscribers handle the error state
        // before setting Stopping state
        scope.launch {
            stopSelf()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun stopSelf() {
        _state.value = RecordingState.Stopping

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
    fun toggleRecPause(): RecordingState {
        when (state.value) {
            is RecordingState.Recording -> {
                pause()
            }

            is RecordingState.Paused -> {
                resume()
            }

            else -> throw IllegalStateException()
        }
        return state.value
    }

    fun stop(): Job {
        amplitudeVizUpdateJob.cancel()
        stopwatch.stop()

        return scope.launch {
            recorder.stop()
            stopSelf()
        }
    }

    /**
     * called by service when it's destroyed normally or not
     */
    fun onDestroy() {
        _state.value = RecordingState.Idle
    }

    private fun pause() {
        recorder.pause()
        stopwatch.pause()
        _state.value = RecordingState.Paused
        notificationsManager.updateNotification(state.value, recorder.supportsPausing())
    }

    private fun resume() {
        recorder.resume()
        stopwatch.resume()
        _state.value = RecordingState.Recording(
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
