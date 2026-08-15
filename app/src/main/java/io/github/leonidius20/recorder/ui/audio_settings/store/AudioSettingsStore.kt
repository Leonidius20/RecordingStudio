package io.github.leonidius20.recorder.ui.audio_settings.store

import com.arkivanov.mvikotlin.core.store.SimpleBootstrapper
import com.arkivanov.mvikotlin.core.store.Store
import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.extensions.coroutines.CoroutineExecutor
import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.BitDepthOption
import io.github.leonidius20.recorder.data.settings.BitRateSettingType
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Intent
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.Label
import io.github.leonidius20.recorder.ui.audio_settings.store.AudioSettingsStore.State
import io.github.leonidius20.recorder.ui.audio_settings.view_impl.ChipSetting
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

interface AudioSettingsStore : Store<Intent, State, Label> {

    sealed interface Intent {

        data class SetAudioSource(
            val source: Settings.AudioSourceOption,
        ) : Intent

        data class SetContainerFormat(
            val format: Container,
        ) : Intent

        data class SetCodec(
            val codec: Codec,
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

    // todo: maybe move make AudioSourceOption itself implement the interface,
    //  since all data is there, and
    //  also add description to the interface?
    data class AudioSourceSetting(
        override val option: Settings.AudioSourceOption,
        override val isSelected: Boolean,
    ) : ChipSetting<Settings.AudioSourceOption> {

        override val id: Int
            get() = option.value

        override val displayName: String
            get() = option.name

    }

    data class ContainerSetting(
        override val option: Container,
        override val isSelected: Boolean,
    ) : ChipSetting<Container> {

        override val id: Int
            get() = option.value

        override val displayName: String
            get() = option.displayName

    }

    data class CodecSetting(
        override val option: Codec,
        override val isSelected: Boolean,
    ) : ChipSetting<Codec> {

        override val id: Int
            get() = option.value

        override val displayName: String
            get() = option.displayName

    }

    data class ChannelsSetting(
        override val option: AudioChannels,
        override val isSelected: Boolean
    ) : ChipSetting<AudioChannels> {

        override val id: Int
            get() = option.value

        override val displayName: String
            get() = option.name // todo: replace with display name

    }

    data class SampleRateSetting(
        val rate: Int,
        val isSelected: Boolean,
    )

    data class BitDepthSetting(
        val depth: BitDepthOption,
        val isSelected: Boolean,
    )

    data class BitRateSettings(
        val type: BitRateSettingType.BitRateValues,
        val current: Float,
    )

    sealed interface State {

        data object Loading : State

        data class Ready(
            // todo: can have all these as empty lists and remove sealed class
            //  hierarchy
            val audioSources: List<AudioSourceSetting>,
            val audioSource: Settings.AudioSourceOption,

            val containers: List<ContainerSetting>,

            val codecs: List<CodecSetting>,

            val channelOptions: List<ChannelsSetting>,

            val sampleRates: List<SampleRateSetting>,

            val bitRateSettings: BitRateSettings?,

            val bitDepths: List<BitDepthSetting>?,
        ) : State

    }

    sealed interface Label {

    }

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
            val newSettings: State.Ready,
        ) : Msg

    }


    class ExecutorImpl @Inject constructor(
        private val settings: Settings,
    ): CoroutineExecutor<Intent, Action, State, Msg, Label>() {

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
                        settings.state.collect { newSettings ->
                            val container = newSettings.outputFormat
                            val codec = newSettings.encoder

                            val availableBitDepths = run {
                                val bitRateSetting = codec.bitRateSettingType
                                if (bitRateSetting is BitRateSettingType.BitDepthDiscreteValues) {
                                    bitRateSetting.availableOptions
                                } else null
                            }


                            val supportedSampleRates = run {
                                codec.supportedSampleRates
                                    .intersect(settings.sampleRatesSupportedByDevice)
                                    .sorted()
                            }

                            val bitRateSettingType = codec.bitRateSettingType


                            val audioSources = settings.audioSourceOptions.map {
                                AudioSettingsStore.AudioSourceSetting(
                                    option = it,
                                    // todo: have settings expose enum value and not int?
                                    isSelected = it.value == newSettings.audioSource
                                )
                            }

                            val currentBitDepth = newSettings.bitDepthsForCodecs[codec]

                            val newState = State.Ready(
                                audioSources = audioSources,
                                audioSource = settings.audioSourceOptions.find {
                                    it.value == newSettings.audioSource
                                }!!, // todo: move this logic to Settings

                                containers = Container.supportedContainers().map {
                                    AudioSettingsStore.ContainerSetting(
                                        option = it,
                                        isSelected = newSettings.outputFormat == it,
                                    )
                                },

                                codecs = container.availableCodecs.map {
                                    AudioSettingsStore.CodecSetting(
                                        option = it,
                                        isSelected = it == codec,
                                    )
                                },

                                channelOptions = AudioChannels.entries.map {
                                    AudioSettingsStore.ChannelsSetting(
                                        option = it,
                                        isSelected = newSettings.numOfChannels == it
                                    )
                                }, // todo: do all phones support stereo?

                                sampleRates = supportedSampleRates.map {
                                    AudioSettingsStore.SampleRateSetting(
                                        rate = it,
                                        isSelected = it == newSettings.sampleRate,
                                    )
                                },

                                // todo: have Settings expose type and current value
                                //  instead of this list of bitrates per codec
                                bitRateSettings = when (bitRateSettingType) {
                                    is BitRateSettingType.BitDepthDiscreteValues,
                                    BitRateSettingType.None -> null

                                    is BitRateSettingType.BitRateValues -> {
                                        newSettings.bitRatesForCodecs[codec]?.let {
                                            AudioSettingsStore.BitRateSettings(
                                                type = bitRateSettingType,
                                                current = it
                                            )
                                        }
                                    }
                                },

                                bitDepths = availableBitDepths?.map {
                                    AudioSettingsStore.BitDepthSetting(
                                        depth = it,
                                        isSelected = it == currentBitDepth
                                    )
                                },
                            )

                            dispatch(Msg.SettingsUpdated(newState))
                        }
                    }
                }
            }
        }

    }

    fun create(): AudioSettingsStore = object : AudioSettingsStore, Store<Intent, State, Label> by storeFactory.create(
        name = "AudioSettingsStore",
        initialState = State.Loading as State,
        bootstrapper = SimpleBootstrapper(Action.SubscribeToUpdates),
        executorFactory = { executorProvider.get() },
        reducer = { msg ->
            when(msg) {
                is Msg.SettingsUpdated -> {
                    msg.newSettings
                }
            }
        }
    ) {}

}
