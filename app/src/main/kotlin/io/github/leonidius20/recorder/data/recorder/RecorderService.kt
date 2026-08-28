package io.github.leonidius20.recorder.data.recorder

import android.app.ForegroundServiceStartNotAllowedException
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.github.leonidius20.recorder.domain.recorder.PERSISTENT_NOTIFICATION_ID
import io.github.leonidius20.recorder.domain.recorder.RecordAudioUseCase
import io.github.leonidius20.recorder.domain.recorder.RecordingNotificationsManager
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RecorderService : LifecycleService() {

    @Inject
    lateinit var recordAudioUseCase: RecordAudioUseCase

    @Inject
    lateinit var notificationsManager: RecordingNotificationsManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        lifecycleScope.launch {
            recordAudioUseCase.state.collect {
                if (it is RecordAudioUseCase.State.Stopping) {
                    stopSelf()
                }
            }
        }

        val supportsPausing = recordAudioUseCase.start()

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

        // we only move service to foreground after the recording was successfully started
        try {
            ServiceCompat.startForeground(
                this, PERSISTENT_NOTIFICATION_ID,
                notificationsManager.buildPersistentNotification(
                    RecordAudioUseCase.State.Recording(supportsPausing),
                    supportsPausing,
                ), foregroundServiceType
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {
                e.printStackTrace()
                // App not in a valid state to start foreground service
                // (e.g. started from bg)
            }
            e.printStackTrace()
            stopSelf()
        }

        return START_NOT_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()

        recordAudioUseCase.onDestroy()
    }

}
