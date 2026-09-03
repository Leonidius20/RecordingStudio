package io.github.leonidius20.recorder.ui.home.store

import android.net.Uri
import com.arkivanov.mvikotlin.core.store.Store
import io.github.leonidius20.recorder.domain.recorder.RecordingState
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Intent
import io.github.leonidius20.recorder.ui.home.store.HomeStore.Label
import io.github.leonidius20.recorder.ui.home.store.HomeStore.State
import kotlinx.coroutines.flow.Flow

interface HomeStore : Store<Intent, State, Label> {

    // todo: shared flow with buffer
    val amplitudes: Flow<Int>

    // todo: make sure it only updates every 1 sec
    //  and add to main state??
    val timerText: Flow<String>

    sealed interface Intent {

        data object ToggleRecPause : Intent

        data object NotifyRecordingPermissionsConfirmed : Intent

        data object StopRecording : Intent

        data object OpenAudioSettings : Intent

    }

    sealed interface Label {

        data object CheckRecordingPermissions : Label

        data object OpenAudioSettings : Label

        data class NotifyRecordingDone(
            val result: Uri,
        ) : Label

        data object RecreateVisualizer : Label

    }

    data class State(
        val recordingState: RecordingState = RecordingState.Idle,
    )

}
