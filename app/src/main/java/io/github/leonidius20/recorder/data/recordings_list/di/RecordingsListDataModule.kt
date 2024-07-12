package io.github.leonidius20.recorder.data.recordings_list.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RecordingsListDataModule {

    @Provides
    @Singleton
    @Named("io")
    fun provideIoDispatcher() = Dispatchers.IO

}