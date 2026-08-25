package io.github.leonidius20.recorder.domain.recorder

import com.yashovardhan99.timeit.Stopwatch
import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.SettingsInterface
import io.github.leonidius20.recorder.domain.events.SystemEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

// todo: unit tests for business logic?
// todo: remove all the fucking notifications from here
@ServiceScoped
class RecordAudioUseCase @Inject constructor(
    private val settings: SettingsInterface,
    private val scope: CoroutineScope,
    private val recordingsListRepository: RecordingsListRepository,
    private val notificationsManager: RecordingNotificationsManager,
    private val systemEventObserver: UnitedSystemEventObserver,
    private val outputFileFactory: OutputFileAbstraction.Factory,
    private val recorderFactory: AudioRecorderFactory,
) {

    // todo: is this needed even???
    enum class State {
        PREPARING,
        RECORDING,
        PAUSED,
        ERROR,

        STOP, // new: for service to stop
    }
    private val _state = MutableStateFlow(State.PREPARING)
    val state: StateFlow<State>
        get() = _state

    lateinit var file: OutputFileAbstraction

    lateinit var recorder: AudioRecorder

    private lateinit var stopwatch: Stopwatch

    private lateinit var amplitudeVizUpdateJob: Job

    private val _timer = MutableStateFlow(0L)

    /**
     * length of the recording so far in milliseconds
     */
    val timer: StateFlow<Long>
        get() = _timer

    private val _amplitudes = MutableSharedFlow<Int>()
    /**
     * emits max amplitude every 100ms. Used for audio visualization
     */
    val amplitudes = _amplitudes.asSharedFlow()

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

    fun start() {
        notificationsManager.createRecInProgressNotificationChannel()

        notificationsManager.createPrematureStopNotificationChannel()

        // used to control the recording from
        scope.launch {
            systemEventObserver.events.collect(
                ::onSystemEvent
            )
        }

        systemEventObserver.register()

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
            return
        }

        recorder.start()

        _state.value = State.RECORDING


        stopwatch = Stopwatch()
        stopwatch.setOnTickListener {
            _timer.value = stopwatch.elapsedTime
        }
        stopwatch.start()


        amplitudeVizUpdateJob = scope.launch(Dispatchers.Default) {
            // every 100ms, emit maxAmplitude
            while (isActive) {
                if (state.value == State.RECORDING) {
                    _amplitudes.emit(recorder.maxAmplitude())
                    delay(100)
                } else {
                    // first() supposed to be cancellable?
                    state.first { it == State.RECORDING }
                }
            }
        }
    }

    fun stopOnError() {
        _state.value = State.ERROR
        // todo: have the service subscribe to state and
        //  kill itself on ERROR
        stopSelf()
    }

    private fun stopSelf() {
        _state.value = State.STOP
    }

    /**
     * @return the new state
     */
    fun toggleRecPause(): State {
        when (state.value) {
            State.RECORDING -> {
                pause()
            }

            State.PAUSED -> {
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
        recordingsListRepository.updateRecordingMetadata(
            file.fileUri, size = file.descriptor.statSize,
            duration = stopwatch.elapsedTime
        )
        file.close()

        notificationsManager.cancelPausedOnIncomingCallNotification()

        systemEventObserver.unregister()
    }

    private fun pause() {
        recorder.pause()
        stopwatch.pause()
        _state.value = State.PAUSED
        notificationsManager.updateNotification(state.value)
    }

    private fun resume() {
        recorder.resume()
        stopwatch.resume()
        _state.value = State.RECORDING

        notificationsManager.cancelPausedOnIncomingCallNotification()

        notificationsManager.updateNotification(state.value)
    }

    private fun stopAbruptly(explanation: String) {
        notificationsManager.sendAbruptStopNotification(explanation)

        //launcher!!.onServiceStopped() // update ui state
        stop()
    }

}
