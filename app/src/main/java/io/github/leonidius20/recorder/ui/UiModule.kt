package io.github.leonidius20.recorder.ui

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class UiModule {

    @Provides
    fun provideDefaultStoreFactory() = DefaultStoreFactory()

    @Binds
    abstract fun bindStoreFactory(default: DefaultStoreFactory): StoreFactory

}
