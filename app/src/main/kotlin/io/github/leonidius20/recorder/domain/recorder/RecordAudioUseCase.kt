package io.github.leonidius20.recorder.domain.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.permissionx.guolindev.PermissionX
import com.yashovardhan99.timeit.Stopwatch
import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.MainActivity
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.recorder.MediaRecorderWrapper
import io.github.leonidius20.recorder.data.recorder.PERSISTENT_NOTIFICATION_ID
import io.github.leonidius20.recorder.data.recorder.PcmAudioRecorder
import io.github.leonidius20.recorder.data.recorder.REC_ABRUPT_STOP_CHANNEL_ID
import io.github.leonidius20.recorder.data.recorder.REC_IN_PROGRESS_CHANNEL_ID
import io.github.leonidius20.recorder.data.recorder.REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID
import io.github.leonidius20.recorder.data.recorder.REC_STOPPED_LOW_BATTERY_OR_STORAGE_NOTIFICATION_ID
import io.github.leonidius20.recorder.data.recorder.RecordingControlBroadcastReceiver
import io.github.leonidius20.recorder.data.recorder.observers.ControlObserver
import io.github.leonidius20.recorder.data.recorder.observers.IncomingCallObserver
import io.github.leonidius20.recorder.data.recorder.observers.LowBatteryObserver
import io.github.leonidius20.recorder.data.recorder.observers.LowStorageObserver
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.BitRateSettingType
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.PcmBitDepthOption
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// todo: remove all the fucking notifications from here
@ServiceScoped
class RecordAudioUseCase @Inject constructor(
    private val settings: Settings,
    private val context: Context, // the context in which we record. i.e. service context. should be provided by hilt
    private val scope: CoroutineScope,
    private val recordingsListRepository: RecordingsListRepository,
) {

    init {
        Timber.d("Created audio record use case")

    }

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

    private lateinit var observers: List<SystemEventObserver>

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
                    sendNotificationAboutPausingOnCall()
                }
            }
        }
    }

    fun start() {
        Timber.d("use case start()")

        createRecInProgressNotificationChannel()

        createPrematureStopNotificationChannel()

        // used to control the recording from
        // todo: have another class handle all of them and expose one method
        observers = listOf(
            ControlObserver(context, scope),
            LowBatteryObserver(context, scope),
            LowStorageObserver(context, scope),
            IncomingCallObserver(context, scope),
        )

        scope.launch {
            observers.map { it.events.consumeAsFlow() }.merge().collect(
                ::onSystemEvent
            )
        }

        observers.forEach(SystemEventObserver::register)

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
     * create a notification channel for the persistent notification that is
     * shown while the recording is in progress or paused
     */
    private fun createRecInProgressNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording status"
            val descriptionText = "Shown while a recording is in progress or paused"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(REC_IN_PROGRESS_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun createPrematureStopNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording stopped abruptly"
            val descriptionText =
                "Sent if a recording was stopped because the device was running out of battery or storage"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(REC_ABRUPT_STOP_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
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

        NotificationManagerCompat.from(context).cancel(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID)

        observers.forEach(SystemEventObserver::unregister)
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pause() {
        //recorder.pause()
        recorder.pause()
        stopwatch.pause()
        _state.value = State.PAUSED
        updateNotification()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resume() {
        //recorder.resume()
        recorder.resume()
        stopwatch.resume()
        _state.value = State.RECORDING

        NotificationManagerCompat.from(context).cancel(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID)

        updateNotification()
    }

    private fun stopAbruptly(explanation: String) {
        if (PermissionX.isGranted(context, PermissionX.permission.POST_NOTIFICATIONS)) {
            NotificationCompat.Builder(context, REC_ABRUPT_STOP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording stopped")
                .setContentText(explanation)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build().also { notification ->
                    NotificationManagerCompat.from(context)
                        .notify(REC_STOPPED_LOW_BATTERY_OR_STORAGE_NOTIFICATION_ID, notification)
                }
        }

        //launcher!!.onServiceStopped() // update ui state
        stop()
    }

    private fun sendNotificationAboutPausingOnCall() {
        if (PermissionX.isGranted(context, PermissionX.permission.POST_NOTIFICATIONS)) {
            NotificationCompat.Builder(context, REC_ABRUPT_STOP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording paused")
                .setContentText("Incoming phone call")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setAutoCancel(true)
                .build().also { notification ->
                    NotificationManagerCompat.from(context)
                        .notify(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID, notification)
                }
        }
    }

    /**
     * should happen after toggling rec/pause or every second to update timer
     */
    private fun updateNotification() {
        NotificationManagerCompat.from(context).notify(
            PERSISTENT_NOTIFICATION_ID, buildPersistentNotification()
        )
    }

    fun buildPersistentNotification(): Notification {
        val titleText = when (state.value) {
            State.RECORDING -> context.getString(R.string.notif_recording_in_progress)
            State.PAUSED -> context.getString(R.string.notif_recording_paused)
            else -> ""
        }

        val recPauseToggleActionText = when (state.value) {
            State.RECORDING -> context.getString(R.string.notif_action_pause)
            State.PAUSED -> context.getString(R.string.notif_action_resume)
            else -> ""
        }


        val notificationB = NotificationCompat.Builder(context, REC_IN_PROGRESS_CHANNEL_ID)
            // Create the notification to display while the service is running
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_microphone)
            .setContentTitle(titleText)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )

        // todo: make it always once we re-implement recording with a lower-level api
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val toggleRecPauseIntent =
                Intent(RecordingControlBroadcastReceiver.ACTION_PAUSE_OR_RESUME)
            notificationB.addAction(
                R.drawable.ic_pause,
                recPauseToggleActionText,
                PendingIntent.getBroadcast(
                    context,
                    0,
                    toggleRecPauseIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
        }

        val stopIntent = Intent(RecordingControlBroadcastReceiver.ACTION_STOP)
        notificationB.addAction(
            R.drawable.ic_stop,
            context.getString(R.string.notif_action_stop),
            PendingIntent.getBroadcast(context, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)
        )

        return notificationB.build()
    }

}
