package io.github.leonidius20.recorder

import android.app.ForegroundServiceStartNotAllowedException
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.MediaRecorder
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException

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

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel.
            val name = "Recording in progress"
            val descriptionText = "Recording in progress"
            val importance = NotificationManager.IMPORTANCE_LOW
            val mChannel = NotificationChannel("io.github.leonidius20.recorder.inprogress", name, importance)
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



        val fileName = "${System.currentTimeMillis()}"

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
                recorder.pause()
                _state.value = State.PAUSED
            }
            State.PAUSED -> {
                recorder.resume()
                _state.value = State.RECORDING
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



}