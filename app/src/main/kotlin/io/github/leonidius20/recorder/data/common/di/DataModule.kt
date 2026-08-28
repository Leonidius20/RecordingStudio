package io.github.leonidius20.recorder.data.common.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.data.settings.SettingsInterface
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import javax.inject.Qualifier
import javax.inject.Singleton

class Dispatcher {
    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Default

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Io

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class Main
}

class Scope {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class App

}

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


}

@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {

    @Singleton
    @Binds
    fun bindSettings(settings: Settings): SettingsInterface

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
