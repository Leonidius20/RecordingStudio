package io.github.leonidius20.recorder.di

import javax.inject.Qualifier

class Scope {

    @Qualifier
    @Retention(AnnotationRetention.BINARY)
    annotation class App

}
