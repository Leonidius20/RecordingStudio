package io.github.leonidius20.recorder.ui.home.controller

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
import io.github.leonidius20.recorder.ui.home.store.HomeStoreFactory
import io.github.leonidius20.recorder.ui.home.view.HomeView
import io.github.leonidius20.recorder.ui.home.view.stateToModel
import kotlinx.coroutines.flow.map

class HomeController @AssistedInject constructor(
    private val storeFactory: HomeStoreFactory,
    @Assisted instanceKeeper: InstanceKeeper,
) {

    @AssistedFactory
    interface Factory {

        fun create(
            instanceKeeper: InstanceKeeper,
        ): HomeController

    }

    private val store =
        instanceKeeper.getStore(storeFactory::create)

    fun onViewCreated(view: HomeView, viewLifecycle: Lifecycle) {
        bind(viewLifecycle, BinderLifecycleMode.START_STOP) {
            store.states.map(stateToModel) bindTo view
            store.labels bindTo view::handleLabel
            store.amplitudes bindTo view::handleAmplitudeUpdate
            store.timerText bindTo view::handleTimerTick
            view.events bindTo store
        }
    }

}
