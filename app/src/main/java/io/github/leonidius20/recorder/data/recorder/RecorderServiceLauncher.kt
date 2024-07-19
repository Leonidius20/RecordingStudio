package io.github.leonidius20.recorder.data.recorder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * this class is responsible for launching and stopping the RecorderService,
 * as well as communicating with it through the binder. The service is bound to
 * app context, so fragment/activity doesn't have to rebind on recreation.
 */
@Singleton
class RecorderServiceLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : ServiceConnection {

    private var binder: RecorderService.Binder? = null

    enum class State {
        IDLE,
        RECORDING,
        PAUSED,
        ERROR,
    }

    private val _state = MutableStateFlow(State.IDLE)
    val state: StateFlow<State>
        get() = _state


    private val _timer =  MutableStateFlow(0L)

    val timer: StateFlow<Long>
        get() = _timer

    /**
     * @return LiveData with the state of the RecorderService
     * that can be observed while the recording is in progress
     */
    fun launchRecording() {


        // if (!recordingsDirectory.exists()) mkdirs

        // todo: formatted time YYYY-MM-DD-HH-MM-SS-SSS


        ContextCompat.startForegroundService(
            context,
            Intent(context, RecorderService::class.java)
        )

        context.bindService(
            Intent(context, RecorderService::class.java),
            this,
            Context.BIND_IMPORTANT // todo: understand these values
        )
    }

    fun stopRecording() {
        context.stopService(
            Intent(context, RecorderService::class.java)
        )
        _state.value = State.IDLE
    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun toggleRecPause() {
        binder!!.service.toggleRecPause()
    }

    override fun onServiceConnected(
        name: ComponentName?,
        service: IBinder?
    ) {
        binder = service as RecorderService.Binder

        // serviceScope is cancelled when the service is destroyed
        service.service.serviceScope.launch {
            service.service.state.onEach {
                when(it) {
                    RecorderService.State.RECORDING -> _state.value = State.RECORDING
                    RecorderService.State.PAUSED -> _state.value = State.PAUSED
                    RecorderService.State.ERROR -> {
                        // todo error ui state
                        _state.value = State.ERROR
                    }
                    RecorderService.State.PREPARING -> {
                        _state.value = State.IDLE
                    }
                }
            }.launchIn(this)

            service.service.timer.collect {
                _timer.value = it
            }
        }
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        binder = null
    }

}