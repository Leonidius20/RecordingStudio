package io.github.leonidius20.recorder.di

import javax.inject.Qualifier

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
