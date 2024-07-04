package io.github.leonidius20.recorder

import android.app.Service
import android.content.ContentValues
import android.content.Intent
import android.media.MediaRecorder
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import java.io.IOException

class RecorderService : Service() {

    private lateinit var descriptor: ParcelFileDescriptor

    private lateinit var recorder: MediaRecorder

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
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
            }

            start()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.stop()
        recorder.release()
        descriptor.close()
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

}