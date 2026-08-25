package io.github.leonidius20.recorder.data.common.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.data.settings.SettingsInterface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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

}

@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {

    @Singleton
    @Binds
    fun bindSettings(settings: Settings): SettingsInterface

}
