package io.github.leonidius20.recorder.ui

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UiModule {

    @Binds
    @Singleton
    abstract fun bindStoreFactory(default: DefaultStoreFactory): StoreFactory

}

@Module
@InstallIn(SingletonComponent::class)
object MviModule {

    @Provides
    fun provideDefaultStoreFactory() = DefaultStoreFactory()

}
