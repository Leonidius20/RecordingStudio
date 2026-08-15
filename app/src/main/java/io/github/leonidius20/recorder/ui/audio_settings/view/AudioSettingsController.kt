package io.github.leonidius20.recorder.ui.audio_settings.view

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.mvikotlin.core.binder.BinderLifecycleMode
import com.arkivanov.mvikotlin.core.instancekeeper.getStore
import com.arkivanov.mvikotlin.extensions.coroutines.bind
import com.arkivanov.mvikotlin.extensions.coroutines.events
import com.arkivanov.mvikotlin.extensions.coroutines.labels
import com.arkivanov.mvikotlin.extensions.coroutines.states
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStoreFactory

class AudioSettingsController @AssistedInject constructor(
    private val storeFactory: AudioSettingsStoreFactory,
    @Assisted instanceKeeper: InstanceKeeper,
) {

    private val store = instanceKeeper.getStore {
        storeFactory.create()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            instanceKeeper: InstanceKeeper,
        ): AudioSettingsController
    }

    fun onViewCreated(view: AudioSettingsView, viewLifecycle: Lifecycle) {
        bind(viewLifecycle, BinderLifecycleMode.START_STOP) {
            store.states bindTo view
            view.events bindTo store
            store.labels bindTo view::handleLabel
        }
    }

}
