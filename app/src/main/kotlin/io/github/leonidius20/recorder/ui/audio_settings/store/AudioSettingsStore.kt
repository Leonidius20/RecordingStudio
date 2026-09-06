package io.github.leonidius20.recorder.ui.audio_settings.store

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import io.github.leonidius20.recorder.audio_config.data.repository.AudioConfigRepositoryImpl
import io.github.leonidius20.recorder.audio_config.domain.impl.AudioSourceOption
import io.github.leonidius20.recorder.audio_config.domain.impl.GetAvailableSettingsUseCase
import io.github.leonidius20.recorder.audio_config.domain.impl.options.AudioConfigSettings
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Intent
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.State
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

interface AudioSettingsStore : Store<Intent, State, Nothing> {

    sealed interface Intent {

        data class SetAudioSource(
            val source: AudioSourceOption,
        ) : Intent

        data class SetContainerFormat(
            val format: Container,
        ) : Intent

        data class SetCodec(
            val codec: Codec<*>,
        ) : Intent

        data class SetChannels(
            val channels: AudioChannels,
        ) : Intent

        data class SetSampleRate(
            val rate: Int,
        ) : Intent

        data class SetBitDepth(
            val depth: BitDepthOption,
        ) : Intent

        data class SetBitRate(
            val rate: Float,
        ): Intent

    }

    data class State(
        val audioConfigSettings: AudioConfigSettings = AudioConfigSettings(),
    )

}

class AudioSettingsStoreFactory @Inject constructor(
    private val storeFactory: StoreFactory,
    private val executorProvider: Provider<ExecutorImpl>,
) {

    sealed interface Action {

        data object SubscribeToUpdates : Action

    }

    sealed interface Msg {

        data class SettingsUpdated(
            val newSettings: AudioConfigSettings,
        ) : Msg

    }


    class ExecutorImpl @Inject constructor(
        // todo: also move write capabilities to use case.
        //  and make repo internal if we add DI to data modules
        private val settings: AudioConfigRepositoryImpl,
        private val getAvailableSettings: GetAvailableSettingsUseCase
    ): CoroutineExecutor<Intent, Action, State, Msg, Nothing>() {

        override fun executeIntent(intent: Intent) {
            when(intent) {
                is Intent.SetAudioSource -> {
                    // todo: make it async
                    settings.setAudioSource(intent.source.value)
                }
                is Intent.SetContainerFormat -> {
                    settings.setOutputFormat(intent.format)
                }
                is Intent.SetCodec -> {
                    settings.setCodec(intent.codec)
                }
                is Intent.SetChannels -> {
                    settings.setNumberOfChannels(intent.channels)
                }
                is Intent.SetSampleRate -> {
                    settings.setSampleRate(intent.rate)
                }
                is Intent.SetBitDepth -> {
                    settings.setBitDepth(intent.depth)
                }
                is Intent.SetBitRate -> {
                    settings.setBitRate(intent.rate)
                }
            }
        }

        override fun executeAction(action: Action) {
            when(action) {
                is Action.SubscribeToUpdates -> {
                    scope.launch {
                        getAvailableSettings.settings.collect {
                            dispatch(Msg.SettingsUpdated(it))
                        }
                    }
                }
            }
        }

    }

    fun create(): AudioSettingsStore = object : AudioSettingsStore, Store<Intent, State, Nothing> by storeFactory.create(
        name = "AudioSettingsStore",
        initialState = State(),
        bootstrapper = SimpleBootstrapper(Action.SubscribeToUpdates),
        executorFactory = { executorProvider.get() },
        reducer = { msg ->
            when(msg) {
                is Msg.SettingsUpdated -> {
                    copy(audioConfigSettings = msg.newSettings)
                }
            }
        }
    ) {}

}
