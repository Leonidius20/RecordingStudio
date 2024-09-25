package io.github.leonidius20.recorder.data.recorder

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MimeTypes
import com.permissionx.guolindev.PermissionX
import com.yashovardhan99.timeit.Stopwatch
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.MainActivity
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.recordings_list.RecordingsListRepository
import io.github.leonidius20.recorder.data.settings.Settings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

private const val REC_IN_PROGRESS_CHANNEL_ID = "io.github.leonidius20.recorder.inprogress"
private const val REC_ABRUPT_STOP_CHANNEL_ID = "io.github.leonidius20.recorder.stopped"

// todo: refactor maybe, place audio-related stuff in separate class to separate from
// todo: for this, use lifecycle-aware components

@AndroidEntryPoint
class RecorderService : LifecycleService() {

    enum class State {
        PREPARING,
        RECORDING,
        PAUSED,
        ERROR,
    }

    private lateinit var descriptor: ParcelFileDescriptor

    private lateinit var recorder: MediaRecorder

    lateinit var rec: AudioRecorder

    private val binder = Binder()

    private val _state = MutableStateFlow(State.PREPARING)
    val state: StateFlow<State>
        get() = _state

    //private val job = SupervisorJob()
    //val serviceScope = CoroutineScope(job + Dispatchers.Main)

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

    private lateinit var stopwatch: Stopwatch

    //private lateinit var lowBatteryBroadcastReceiver: BroadcastReceiverWithCallback

    @Inject
    lateinit var settings: Settings

    // needed here so that we can return it from activity started for result (action record audio)
    lateinit var fileUri: Uri

    @Inject
    lateinit var recordingsListRepository: RecordingsListRepository

    private lateinit var recControlBroadcastReceiver: RecordingControlBroadcastReceiver
    private lateinit var lowBatteryBroadcastReceiver: BroadcastReceiverWithCallback
    private lateinit var lowStorageBroadcastReceiver: BroadcastReceiverWithCallback
    private lateinit var callBroadcastReceiver: IncomingCallBroadcastReceiver


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        createRecInProgressNotificationChannel()

        createPrematureStopNotificationChannel()

        // used to control the recording from
        recControlBroadcastReceiver = RecordingControlBroadcastReceiver().apply {
            val intentFilter = IntentFilter().apply {
                addAction(RecordingControlBroadcastReceiver.ACTION_PAUSE_OR_RESUME)
                addAction(RecordingControlBroadcastReceiver.ACTION_STOP)
            }

            ContextCompat.registerReceiver(
                this@RecorderService, this,
                intentFilter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }

        lowBatteryBroadcastReceiver = BroadcastReceiverWithCallback(
            callback = {
                if (settings.state.value.stopOnLowBattery) {
                    stopAbruptly(explanation = "The device is running out of battery.")
                }
            }
        ).apply {
            val intentFilter = IntentFilter(Intent.ACTION_BATTERY_LOW)
            ContextCompat.registerReceiver(
                this@RecorderService, this,
                intentFilter, ContextCompat.RECEIVER_EXPORTED)
        }

        lowStorageBroadcastReceiver = BroadcastReceiverWithCallback {
            if (settings.state.value.stopOnLowStorage) {
                stopAbruptly("The device is running out of storage.")
            }
        }.apply {
            val intentFilter = IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW)
            ContextCompat.registerReceiver(
                this@RecorderService, this,
                intentFilter, ContextCompat.RECEIVER_EXPORTED)
        }

        callBroadcastReceiver = IncomingCallBroadcastReceiver {
            if (settings.state.value.pauseOnCall) {
                pause()
                sendNotificationAboutPausingOnCall()
            }
        }.apply {
            registerInContext(this@RecorderService)
        }

        val fileFormat = settings.state.value.outputFormat

        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())

        val fileName = dateFormat.format(Date(System.currentTimeMillis()))

        // http://androidxref.com/4.4.4_r1/xref/frameworks/base/media/java/android/media/MediaFile.java#174
        fileUri = recordingsListRepository.createRecordingFile(fileName,
            /*fileFormat.mimeType*/ "audio/x-wav"
        )
        descriptor = applicationContext.contentResolver.openFileDescriptor(fileUri, "rw")!!

        val settingsState = settings.state.value

        rec = LegacyAudioReceiver(
            descriptor = descriptor,
            audioSource = settingsState.audioSource,
            sampleRate = 44_100, // todo
            monoOrStereo = settingsState.numOfChannels,
        )

        rec.start()


       /* recorder = MediaRecorder().apply {
            setAudioSource(settingsState.audioSource)
            setOutputFormat(fileFormat.value)
            setOutputFile(descriptor.fileDescriptor)
            setAudioEncoder(settingsState.encoder.value)
            setAudioChannels(settingsState.numOfChannels.value)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recorder", "prepare() failed", e)
                _state.value = State.ERROR
                stopSelf()
            }

            start()
        }*/


        _state.value = State.RECORDING

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

        // we only move service to foreground after the recording was successfully started
        try {
            ServiceCompat.startForeground(
                this, PERSISTENT_NOTIFICATION_ID,
                buildPersistentNotification(), foregroundServiceType
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {

                // App not in a valid state to start foreground service
                // (e.g. started from bg)
            }
            e.printStackTrace()
            _state.value = State.ERROR
            stopSelf()
        }

        stopwatch = Stopwatch()
        stopwatch.setOnTickListener {
            _timer.value = stopwatch.elapsedTime
        }
        stopwatch.start()


        lifecycleScope.launch {
            // every 100ms, emit maxAmplitude
            while(true) {
                if (state.value == State.RECORDING) {
                    // _amplitudes.emit(recorder.maxAmplitude)
                }
                delay(100)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        // ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopwatch.stop()
        //recorder.stop()
        //recorder.release()

        rec.stop()


        contentResolver.update(fileUri, ContentValues().apply {
            put(MediaStore.MediaColumns.SIZE, descriptor.statSize)
            put(MediaStore.MediaColumns.DURATION, stopwatch.elapsedTime)
        }, null, null)

        descriptor.close()

        // job.cancel()

        NotificationManagerCompat.from(this).cancel(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID)

        unregisterReceiver(recControlBroadcastReceiver)
        unregisterReceiver(lowBatteryBroadcastReceiver)
        unregisterReceiver(lowStorageBroadcastReceiver)
        unregisterReceiver(callBroadcastReceiver)
    }

    /**
     * @return the new state
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun toggleRecPause(): State {
        when(state.value) {
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

    /**
     * should happen after toggling rec/pause or every second to update timer
     */
    private fun updateNotification() {
        NotificationManagerCompat.from(this).notify(
            PERSISTENT_NOTIFICATION_ID, buildPersistentNotification()
        )
    }

    private fun buildPersistentNotification(): Notification {
        val titleText = when(state.value) {
            State.RECORDING -> getString(R.string.notif_recording_in_progress)
            State.PAUSED -> getString(R.string.notif_recording_paused)
            else -> ""
        }

        val recPauseToggleActionText = when(state.value) {
            State.RECORDING -> getString(R.string.notif_action_pause)
            State.PAUSED -> getString(R.string.notif_action_resume)
            else -> ""
        }


        val notificationB = NotificationCompat.Builder(this, REC_IN_PROGRESS_CHANNEL_ID)
            // Create the notification to display while the service is running
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_microphone)
            .setContentTitle(titleText)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))

        // todo: make it always once we re-implement recording with a lower-level api
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val toggleRecPauseIntent = Intent(RecordingControlBroadcastReceiver.ACTION_PAUSE_OR_RESUME)
            notificationB.addAction(R.drawable.ic_pause, recPauseToggleActionText, PendingIntent.getBroadcast(this, 0, toggleRecPauseIntent, PendingIntent.FLAG_IMMUTABLE))
        }

        val stopIntent = Intent(RecordingControlBroadcastReceiver.ACTION_STOP)
        notificationB.addAction(R.drawable.ic_stop,
            getString(R.string.notif_action_stop), PendingIntent.getBroadcast(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE))

        return notificationB.build()
    }

    fun stop() {
        stopSelf()
    }

    inner class Binder: android.os.Binder() {

        val service = this@RecorderService

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pause() {
        //recorder.pause()
        rec.pause()
        stopwatch.pause()
        _state.value = State.PAUSED
        updateNotification()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resume() {
        //recorder.resume()
        rec.resume()
        stopwatch.resume()
        _state.value = State.RECORDING

        NotificationManagerCompat.from(this).cancel(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID)

        updateNotification()
    }

    /**
     * called if battery is low or storage is low and we need to stop recording
     * and notify UI
     */
    private fun stopAbruptly(explanation: String) {
        if (PermissionX.isGranted(this, PermissionX.permission.POST_NOTIFICATIONS)) {
            NotificationCompat.Builder(this, REC_ABRUPT_STOP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording stopped")
                .setContentText(explanation)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true)
                .build().also { notification ->
                    NotificationManagerCompat.from(this).notify(REC_STOPPED_LOW_BATTERY_OR_STORAGE_NOTIFICATION_ID, notification)
                }
        }

        //launcher!!.onServiceStopped() // update ui state
        stop()
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
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    /**
     * create a notification channel for the notification that is
     * shown when the recording in prematurely stopped bc of low
     * battery or storage
     */
    private fun createPrematureStopNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording stopped abruptly"
            val descriptionText = "Sent if a recording was stopped because the device was running out of battery or storage"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(REC_ABRUPT_STOP_CHANNEL_ID, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }
    }

    private fun sendNotificationAboutPausingOnCall() {
        if (PermissionX.isGranted(this, PermissionX.permission.POST_NOTIFICATIONS)) {
            NotificationCompat.Builder(this, REC_ABRUPT_STOP_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("Recording paused")
                .setContentText("Incoming phone call")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true)
                .build().also { notification ->
                    NotificationManagerCompat.from(this).notify(REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID, notification)
                }
        }
    }


}

private const val REC_STOPPED_LOW_BATTERY_OR_STORAGE_NOTIFICATION_ID = 1
private const val REC_PAUSED_INCOMING_CALL_NOTIFICATION_ID = 2
private const val PERSISTENT_NOTIFICATION_ID = 100