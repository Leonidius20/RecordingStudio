package io.github.leonidius20.recorder.data.common.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.recorder.data.settings.Settings
import io.github.leonidius20.recorder.data.settings.SettingsInterface
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DataModule {

    @Singleton
    @Provides
    @Named("cpu")
    fun providesCpuDispatcher() = Dispatchers.Default

}

@Module
@InstallIn(SingletonComponent::class)
interface SettingsModule {

    @Singleton
    @Binds
    fun bindSettings(settings: Settings): SettingsInterface

}
