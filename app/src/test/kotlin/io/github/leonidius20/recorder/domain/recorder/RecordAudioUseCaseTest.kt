package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.data.settings.AudioChannels
import io.github.leonidius20.recorder.data.settings.Codec
import io.github.leonidius20.recorder.data.settings.Container
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.data.settings.SettingsInterface
import io.github.leonidius20.recorder.domain.events.SystemEvent
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

// todo: make all mocks relaxed

class RecordAudioUseCaseTest {

    val fakeSettings = Settings.SettingsState (
        stopOnLowBattery = false,
        false,
        false,
        0, Container.THREE_GPP,
        Codec.AMR_NB, AudioChannels.MONO, 0, null,0f
    )

    @Test
    fun `When low battery, stop if setting enabled`() = runTest {
        val scope = this

        var settings = fakeSettings.copy(
            stopOnLowBattery = true
        )

        var settingsProvider = object : SettingsInterface {
            override val state: StateFlow<Settings.SettingsState>
                get() = MutableStateFlow(settings)
        }

        val observer = object : SystemEventObserver {

            override val eventsFlow = MutableSharedFlow<SystemEvent>()

            fun sendEvent() = scope.launch {
                eventsFlow.emit(SystemEvent.LOW_BATTERY)
            }

            override fun register() {}
            override fun unregister() {}

        }

        val useCase = RecordAudioUseCase(
            settingsProvider,
            scope = this,
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
            }

        )

        useCase.start()

        observer.sendEvent()

        assertEquals(RecordAudioUseCase.State.STOP, useCase.state.value)
    }

}
