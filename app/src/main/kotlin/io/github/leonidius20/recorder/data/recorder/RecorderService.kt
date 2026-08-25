package io.github.leonidius20.recorder.data.recorder

import android.app.ForegroundServiceStartNotAllowedException
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import io.github.leonidius20.recorder.domain.recorder.AudioRecorderFactory
import io.github.leonidius20.recorder.domain.recorder.AudioRecorderFactoryImpl
import io.github.leonidius20.recorder.domain.recorder.PERSISTENT_NOTIFICATION_ID
import io.github.leonidius20.recorder.domain.recorder.RecordAudioUseCase
import io.github.leonidius20.recorder.domain.recorder.RecordingNotificationsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

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

    val supportsPausing
        get() = recordAudioUseCase.recorder.supportsPausing()

    private val binder = Binder()

    private val _state = MutableStateFlow(State.PREPARING)
    val state: StateFlow<State>
        get() = _state

    /**
     * length of the recording so far in milliseconds
     */
    val timer: StateFlow<Long>
        get() = recordAudioUseCase.timer


    /**
     * emits max amplitude every 100ms. Used for audio visualization
     */
    val amplitudes get() = recordAudioUseCase.amplitudes

    // needed here so that we can return it from activity started for result (action record audio)
    val fileUri: Uri
        get() = recordAudioUseCase.file.fileUri

    @Inject
    lateinit var recordAudioUseCase: RecordAudioUseCase

    @Inject
    lateinit var notificationsManager: RecordingNotificationsManager

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Timber.d("RecorderService.onStartCommand()")

        lifecycleScope.launch {
            recordAudioUseCase.state.collect {
                when(it) {
                    RecordAudioUseCase.State.STOP -> {
                        stopSelf()
                    }
                    else -> {
                        // todo: remove
                        _state.value = when(it) {
                            RecordAudioUseCase.State.PAUSED -> State.PAUSED
                            RecordAudioUseCase.State.RECORDING -> State.RECORDING
                            RecordAudioUseCase.State.ERROR -> State.ERROR
                            RecordAudioUseCase.State.PREPARING -> State.PREPARING
                            else -> State.ERROR
                        }
                    }
                }


                if (it == RecordAudioUseCase.State.STOP) {
                    stopSelf()
                }
            }
        }

        recordAudioUseCase.start()

        val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0

        // we only move service to foreground after the recording was successfully started
        try {
            ServiceCompat.startForeground(
                this, PERSISTENT_NOTIFICATION_ID,
                notificationsManager.buildPersistentNotification(
                    RecordAudioUseCase.State.RECORDING
                ), foregroundServiceType
            )
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && e is ForegroundServiceStartNotAllowedException
            ) {

                // App not in a valid state to start foreground service
                // (e.g. started from bg)
            }
            e.printStackTrace()

            //recordAudioUseCase.stopOnError()

            _state.value = State.ERROR
            stopSelf()
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()

        recordAudioUseCase.onDestroy()
    }

    /**
     * @return the new state
     */
    @RequiresApi(Build.VERSION_CODES.N)
    fun toggleRecPause(): RecordAudioUseCase.State {
        return recordAudioUseCase.toggleRecPause()
    }

    fun stop() {
        recordAudioUseCase.stop()
    }

    inner class Binder : android.os.Binder() {

        // maybe binder should provide use case directly???

        val service = this@RecorderService

    }

}

@Module
@InstallIn(ServiceComponent::class)
object RecorderModule {

    @Provides
    @ServiceScoped
    fun provideScope(
        service: Service
    ): CoroutineScope = (service as LifecycleOwner).lifecycleScope

    @Provides
    @ServiceScoped
    fun provideContext(
        service: Service
    ): Context = service

}

@Module
@InstallIn(ServiceComponent::class)
interface RecorderModuleBinds {

    @Binds
    @ServiceScoped
    fun bindRecorderFactory(factory: AudioRecorderFactoryImpl): AudioRecorderFactory

}
