package io.github.leonidius20.recorder.data.common.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.domain.audio_settings.AudioConfigRepositoryImpl
import io.github.leonidius20.domain.audio_settings.AudioSettingsDataSource
import io.github.leonidius20.domain.audio_settings.DeviceAudioCapabilities
import io.github.leonidius20.recorder.data.settings.AudioSettingsDataSourceImpl
import io.github.leonidius20.recorder.data.settings.DeviceAudioCapabilitiesImpl
import io.github.leonidius20.recorder.data.settings.UserSettingsRepositoryImpl
import io.github.leonidius20.recorder.di.Dispatcher
import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.domain.events.SystemEventObserver
import io.github.leonidius20.recorder.domain.recorder.AudioRecorderFactory
import io.github.leonidius20.recorder.domain.recorder.AudioRecorderFactoryImpl
import io.github.leonidius20.recorder.domain.recorder.OutputFileFactory
import io.github.leonidius20.recorder.domain.recorder.OutputFileFactoryImpl
import io.github.leonidius20.recorder.domain.recorder.RecordingNotificationsManager
import io.github.leonidius20.recorder.domain.recorder.RecordingNotificationsManagerImpl
import io.github.leonidius20.recorder.domain.recorder.Stopwatch
import io.github.leonidius20.recorder.domain.recorder.StopwatchWrapper
import io.github.leonidius20.recorder.domain.recorder.UnitedSystemEventObserver
import io.github.leonidius20.recorder.domain.settings.AudioConfigReadRepository
import io.github.leonidius20.recorder.domain.settings.UserSettingsReadRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Singleton
    @Provides
    @Dispatcher.Default
    fun providesCpuDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Singleton
    @Provides
    @Dispatcher.Main
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Singleton
    @Provides
    @Scope.App
    fun provideAppScope(): CoroutineScope = MainScope()

    @Provides
    @Singleton
    fun providePref(
        @ApplicationContext context: Context,
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)


}

@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {

    @Singleton
    @Binds
    fun bindSettings(settings: AudioConfigRepositoryImpl): AudioConfigReadRepository

    @Singleton
    @Binds
    fun bindUserSettings(impl: UserSettingsRepositoryImpl): UserSettingsReadRepository

    @Singleton
    @Binds
    fun bindAudioConfigDataSource(impl: AudioSettingsDataSourceImpl): AudioSettingsDataSource

    @Singleton
    @Binds
    fun bindDeviceAudioCapabilities(impl: DeviceAudioCapabilitiesImpl): DeviceAudioCapabilities

}

@Module
@InstallIn(SingletonComponent::class)
interface RecorderModuleBinds {

    @Binds
    @Singleton
    fun bindRecorderFactory(factory: AudioRecorderFactoryImpl): AudioRecorderFactory

    @Binds
    @Singleton
    fun bindEventObserver(observer: UnitedSystemEventObserver): SystemEventObserver

    @Binds
    @Singleton
    fun bindOutputFileFactory(factory: OutputFileFactoryImpl): OutputFileFactory

    @Binds
    @Singleton
    fun bindStopwatch(stopwatch: StopwatchWrapper): Stopwatch
    // todo maybe remove and implement my own kotlin stopwatch or use other library

    @Binds
    @Singleton
    fun bindNotificationManager(manager: RecordingNotificationsManagerImpl): RecordingNotificationsManager

}
