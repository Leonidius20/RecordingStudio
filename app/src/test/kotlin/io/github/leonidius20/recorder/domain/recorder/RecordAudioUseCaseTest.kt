package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.audio_config.data.data_source.DeviceAudioCapabilitiesImpl.Companion.codecAmrNb
import io.github.leonidius20.recorder.audio_config.data.data_source.DeviceAudioCapabilitiesImpl.Companion.container3gpp
import io.github.leonidius20.recorder.audio_config.domain.api.AudioConfigReadRepository
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import io.github.leonidius20.recorder.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.recorder.domain.events.SystemEventObserver
import io.github.leonidius20.recorder.recorder.domain.recorder.AudioRecorder
import io.github.leonidius20.recorder.recorder.domain.recorder.AudioRecorderFactory
import io.github.leonidius20.recorder.recorder.domain.recorder.OutputFile
import io.github.leonidius20.recorder.recorder.domain.recorder.OutputFileFactory
import io.github.leonidius20.recorder.recorder.domain.recorder.RecordAudioUseCase
import io.github.leonidius20.recorder.recorder.domain.recorder.RecordingState
import io.github.leonidius20.recorder.recorder.domain.recorder.Stopwatch
import io.github.leonidius20.recorder.recorder.domain.settings.UserSettings
import io.github.leonidius20.recorder.recorder.domain.settings.UserSettingsReadRepository
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Test

// todo: make all mocks relaxed
// todo: move to domain/recorder module
// todo: refactor Codec and Container to remove references to MediaRecorder ints
class RecordAudioUseCaseTest {

    val fakeAudioSettings = SettingsState(
        0, container3gpp,
        codecAmrNb, AudioChannels.MONO, 0,
        // todo: redesign this api
        Resolution.Bitrate(0f) as Resolution<BitRateSettingType.BitRateDiscreteValues>,
    )

    val fakeUserSettings = UserSettings(
        stopOnLowBattery = false,
        stopOnLowStorage = false,
        pauseOnCall = false,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `When low battery, stop if setting enabled`() = runTest {
        val scope = this

        val settings = fakeUserSettings.copy(
            stopOnLowBattery = true
        )

        val userSettingsProvider = object : UserSettingsReadRepository {
            val _state = MutableStateFlow(settings)
            override val userSettings: StateFlow<UserSettings>
                get() = _state
        }

        val settingsProvider = object : AudioConfigReadRepository {
            override val state: StateFlow<SettingsState<*>>
                get() = MutableStateFlow(fakeAudioSettings)
        }

        val observer = object : SystemEventObserver {

            override val eventsFlow = MutableSharedFlow<SystemEvent>()

            fun sendEvent() = scope.launch {
                eventsFlow.emit(SystemEvent.LOW_BATTERY)
            }

        }

        fun createUseCase() = RecordAudioUseCase(
            settingsProvider,
            scope = backgroundScope,
            dispatcher = UnconfinedTestDispatcher(testScheduler),
            defaultDispatcher = UnconfinedTestDispatcher(testScheduler),
            notificationsManager = mockk(relaxed = true), // todo: remove mocking and this dependency too
            systemEventObserver = observer,
            outputFileFactory = object : OutputFileFactory {
                override fun create(namePattern: String, format: Container) = object : OutputFile {

                    override fun close() {

                    }

                    override fun open() {

                    }

                    override fun updateMetadata(duration: Long) {

                    }

                }
            },
            recorderFactory = object : AudioRecorderFactory {
                override fun create(file: OutputFile): AudioRecorder {
                    return mockk(relaxed = true) // todo: return fake impl?
                }
            },
            stopwatch = object : Stopwatch {
                override val timer: StateFlow<Long>
                    get() = MutableStateFlow(0L)

                override fun start() {}

                override fun stop() {}

                override fun pause() {}

                override fun resume() {}

                override fun clear() {}
            },
            userSettings = userSettingsProvider,
        )

        var useCase = createUseCase()

        useCase.start()

        observer.sendEvent().join()

        assertEquals(RecordingState.Stopping, useCase.state.value)

        useCase.stop()

        userSettingsProvider._state.value = settings.copy(
            stopOnLowBattery = false
        )

        useCase = createUseCase()
        useCase.start()

        observer.sendEvent().join()

        assertThat(useCase.state.value, instanceOf(RecordingState.Recording::class.java))
    }

}
