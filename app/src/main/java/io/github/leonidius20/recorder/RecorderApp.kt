package io.github.leonidius20.recorder

import android.app.Application
import cat.ereza.customactivityoncrash.config.CaocConfig
import dagger.hilt.android.HiltAndroidApp
import io.github.leonidius20.recorder.ui.crash.CrashActivity
import timber.log.Timber

@HiltAndroidApp
class RecorderApp: Application() {

    companion object {
        lateinit var instance: RecorderApp
            private set
    }

    override fun onCreate() {
        super.onCreate()

        instance = this

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        CaocConfig.Builder
            .create()
            .errorActivity(CrashActivity::class.java)
            .restartActivity(MainActivity::class.java)
            .apply()
    }

}