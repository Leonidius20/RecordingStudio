package io.github.leonidius20.recorder.domain.recorder

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import com.yashovardhan99.timeit.Stopwatch
import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.data.recorder.MediaRecorderWrapper
import io.github.leonidius20.recorder.data.recorder.PcmAudioRecorder
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.BitRateSettingType
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.PcmBitDepthOption
import io.github.leonidius20.recorder.data.settings.Settings
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// todo: unit tests for business logic?
// todo: remove all the fucking notifications from here
@ServiceScoped
class RecordAudioUseCase @Inject constructor(
    private val settings: Settings,
    private val context: Context, // the context in which we record. i.e. service context. should be provided by hilt
    private val scope: CoroutineScope,
    private val recordingsListRepository: RecordingsListRepository,
    private val notificationsManager: RecordingNotificationsManager,
    private val systemEventObserver: UnitedSystemEventObserver,
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

    lateinit var fileUri: Uri

    private lateinit var descriptor: ParcelFileDescriptor

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

        val fileFormat = settings.state.value.outputFormat

        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())

        val fileName = dateFormat.format(Date(System.currentTimeMillis()))

        // http://androidxref.com/4.4.4_r1/xref/frameworks/base/media/java/android/media/MediaFile.java#174
        fileUri = recordingsListRepository.createRecordingFile(
            fileName,
            fileFormat.mimeType
        )
        descriptor = context.contentResolver.openFileDescriptor(fileUri, "rw")!!

        val settingsState = settings.state.value

        if (fileFormat == Container.WAV) {
            recorder = PcmAudioRecorder(
                descriptor = descriptor,
                audioSource = settingsState.audioSource,
                sampleRate = settingsState.sampleRate,
                monoOrStereo = settingsState.numOfChannels,
                bitDepth = settingsState.bitDepth as? PcmBitDepthOption
                    ?: PcmBitDepthOption.PCM_16BIT_INT,
                coroutineScope = scope,
            )
        } else {

            try {
                recorder = MediaRecorderWrapper(
                    audioSource = settingsState.audioSource,
                    container = fileFormat,
                    descriptor = descriptor,
                    encoder = settingsState.encoder,
                    channels = settingsState.numOfChannels,
                    sampleRate = settingsState.sampleRate,
                    bitRate =
                        if (settingsState.encoder.bitRateSettingType is BitRateSettingType.BitRateValues)
                            settingsState.bitRate
                        else null
                )
            } catch (e: IOException) {
                Log.e("Recorder", "prepare() failed", e)
                stopOnError()
            }

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
    @RequiresApi(Build.VERSION_CODES.N)
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
        context.contentResolver.update(fileUri, ContentValues().apply {
            put(MediaStore.MediaColumns.SIZE, descriptor.statSize)
            put(MediaStore.MediaColumns.DURATION, stopwatch.elapsedTime)
        }, null, null)

        descriptor.close()

        notificationsManager.cancelPausedOnIncomingCallNotification()

        systemEventObserver.unregister()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pause() {
        recorder.pause()
        stopwatch.pause()
        _state.value = State.PAUSED
        notificationsManager.updateNotification(state.value)
    }

    @RequiresApi(Build.VERSION_CODES.N)
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
