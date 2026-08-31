package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.data.settings.SettingsInterface
import io.github.leonidius20.recorder.data.settings.codecAmrNb
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
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

class RecordAudioUseCaseTest {

    val fakeSettings = Settings.SettingsState (
        stopOnLowBattery = false,
        false,
        false,
        0, Container.THREE_GPP,
        codecAmrNb, AudioChannels.MONO, 0, null,0f
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `When low battery, stop if setting enabled`() = runTest {
        val scope = this

        val settings = fakeSettings.copy(
            stopOnLowBattery = true
        )

        val settingsProvider = object : SettingsInterface {
            val _state = MutableStateFlow(settings)
            override val state: StateFlow<Settings.SettingsState>
                get() = _state
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
            stopwatch = mockk(relaxed = true) // todo: fake

        )

        var useCase = createUseCase()

        useCase.start()

        observer.sendEvent().join()

        assertEquals(RecordingState.Stopping, useCase.state.value)

        useCase.stop()

        settingsProvider._state.value = settings.copy(
            stopOnLowBattery = false
        )

        useCase = createUseCase()
        useCase.start()

        observer.sendEvent().join()

        assertThat(useCase.state.value, instanceOf(RecordingState.Recording::class.java))
    }

}
