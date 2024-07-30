package io.github.leonidius20.recorder.data.recorder

import android.Manifest
import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.permissionx.guolindev.PermissionX
import com.yashovardhan99.timeit.Stopwatch
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.MainActivity
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.data.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

// todo: refactor maybe, place audio-related stuff in separate class to separate from
// service-related stuff
@AndroidEntryPoint
class RecorderService : Service() {

    enum class State {
        PREPARING,
        RECORDING,
        PAUSED,
        ERROR,
    }

    private lateinit var descriptor: ParcelFileDescriptor

    private lateinit var recorder: MediaRecorder

    private val binder = Binder()

    private val _state = MutableStateFlow(State.PREPARING)
    val state: StateFlow<State>
        get() = _state

    private val job = SupervisorJob()
    val serviceScope = CoroutineScope(job + Dispatchers.Main)

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

    /**
     * the launcher class responsible for starting this service.
     * injected by RecorderServiceLauncher itself when starting.
     * Needed so that we can notify the UI when the service is stopped
     * by a broadcast receiver bc of low battery or storage
     */
    var launcher: RecorderServiceLauncher? = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording status"
            val descriptionText = "Shown while a recording is in progress or paused"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel("io.github.leonidius20.recorder.inprogress", name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        val recStopNotificationId = "io.github.leonidius20.recorder.stopped"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording stopped abruptly"
            val descriptionText = "Sent if a recording was stopped because the device was running out of battery or storage"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val mChannel = NotificationChannel(recStopNotificationId, name, importance)
            mChannel.description = descriptionText
            // Register the channel with the system. You can't change the importance
            // or other notification behaviors after this.
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(mChannel)
        }

        val notification = NotificationCompat.Builder(this, "io.github.leonidius20.recorder.inprogress")
            // Create the notification to display while the service is running
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_microphone)
            .setContentTitle("Recording in progress")
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()


        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0


        try {
            /*startForeground(
                100,
                notification,
                foregroundServiceType
            )*/
            ServiceCompat.startForeground(
                this, 100,
                notification, foregroundServiceType
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

        if (settings.state.value.stopOnLowBattery) {
            val lowBatteryBroadcastReceiver = BroadcastReceiverWithCallback(
                callback = {

                    if (PermissionX.isGranted(this, PermissionX.permission.POST_NOTIFICATIONS)) {
                        NotificationCompat.Builder(this, recStopNotificationId)
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .setContentTitle("Recording stopped")
                            .setContentText("The device is running out of battery. Charge it or disable auto-stopping recording in settings.")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
                            .setAutoCancel(true)
                            .build().also { notification ->
                                NotificationManagerCompat.from(this).notify(1, notification)
                            }
                    }

                    launcher!!.onServiceStopped() // update ui state
                    stop()
                }
            ).apply {
                val intentFilter = IntentFilter(Intent.ACTION_BATTERY_LOW)
                ContextCompat.registerReceiver(
                    this@RecorderService, this,
                    intentFilter, ContextCompat.RECEIVER_EXPORTED)
            }
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.getDefault())

        val fileName = dateFormat.format(Date(System.currentTimeMillis()))

        descriptor = getRecFileUri(fileName)

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(descriptor.fileDescriptor)
            // todo: check what codecs there are and provide user with options
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("Recorder", "prepare() failed")
                _state.value = State.ERROR
                stopSelf()
            }

            start()
        }

        _state.value = State.RECORDING

        stopwatch = Stopwatch()
        stopwatch.setOnTickListener {
            _timer.value = stopwatch.elapsedTime
        }
        stopwatch.start()

        serviceScope.launch {
            // every 100ms, emit maxAmplitude
            while(true) {
                if (state.value == State.RECORDING) {
                    _amplitudes.emit(recorder.maxAmplitude)
                }
                delay(100)
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?) = binder

    override fun onDestroy() {
        super.onDestroy()
        // ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        recorder.stop()
        recorder.release()
        descriptor.close()
        job.cancel()
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

    fun stop() {
        stopSelf()
    }


    private fun getRecFileUri(name: String): ParcelFileDescriptor {
        val resolver = applicationContext.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/3gpp") // todo: other types
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Recordings/RecordingStudio")
        }

        val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)

        return resolver.openFileDescriptor(uri!!, "w")!!
    }

    inner class Binder: android.os.Binder() {

        val service = this@RecorderService

    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pause() {
        recorder.pause()
        stopwatch.pause()
        _state.value = State.PAUSED
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun resume() {
        recorder.resume()
        stopwatch.resume()
        _state.value = State.RECORDING
    }



}