package io.github.leonidius20.recorder.data.recordings_list.di

import android.os.Build
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.leonidius20.recorder.data.recordings_list.RecordingsDataSource
import io.github.leonidius20.recorder.data.recordings_list.RecordingsDataSourceAndroid10And11
import io.github.leonidius20.recorder.data.recordings_list.RecordingsDataSourceAndroid12AndUp
import io.github.leonidius20.recorder.data.recordings_list.RecordingsDataSourceAndroid5to9
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

    @Provides
    @Singleton
    fun provideRecordingsDataSource(
        fiveToNine: Lazy<RecordingsDataSourceAndroid5to9>,
        tenToEleven: Lazy<RecordingsDataSourceAndroid10And11>,
        twelveAndUp: Lazy<RecordingsDataSourceAndroid12AndUp>,
    ): RecordingsDataSource {
        val androidVersion = Build.VERSION.SDK_INT
        val android10 = Build.VERSION_CODES.Q // api 29
        val android12 = Build.VERSION_CODES.S // api 31

        return if (androidVersion < android10) {
            fiveToNine.get()
        } else if (androidVersion >= android10 && androidVersion < android12) {
            tenToEleven.get()
        } else {
            twelveAndUp.get()
        }
    }

}