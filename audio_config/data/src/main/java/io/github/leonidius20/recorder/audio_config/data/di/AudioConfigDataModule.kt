package io.github.leonidius20.recorder.audio_config.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.recorder.audio_config.data.data_source.AudioSettingsDataSourceImpl
import io.github.leonidius20.recorder.audio_config.data.data_source.DeviceAudioCapabilitiesImpl
import io.github.leonidius20.recorder.audio_config.data.repository.AudioConfigRepositoryImpl
import io.github.leonidius20.recorder.audio_config.data.repository.AudioSettingsDataSource
import io.github.leonidius20.recorder.audio_config.domain.api.AudioConfigReadRepository
import io.github.leonidius20.recorder.audio_config.domain.impl.DeviceAudioCapabilities
import io.github.leonidius20.recorder.audio_config.domain.impl.repository.AudioConfigWriteRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal interface AudioConfigDataModule {

    @Singleton
    @Binds
    fun bindSettings(settings: AudioConfigRepositoryImpl): AudioConfigReadRepository

    @Singleton
    @Binds
    fun bindWritableSettings(settings: AudioConfigRepositoryImpl): AudioConfigWriteRepository

    @Singleton
    @Binds
    fun bindAudioConfigDataSource(impl: AudioSettingsDataSourceImpl): AudioSettingsDataSource

    @Singleton
    @Binds
    fun bindDeviceAudioCapabilities(impl: DeviceAudioCapabilitiesImpl): DeviceAudioCapabilities

}
