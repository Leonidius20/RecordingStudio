package io.github.leonidius20.recorder

import android.app.Application
import cat.ereza.customactivityoncrash.config.CaocConfig
import dagger.hilt.android.HiltAndroidApp
import io.github.leonidius20.recorder.ui.crash.CrashActivity

@HiltAndroidApp
class RecorderApp: Application() {

    override fun onCreate() {
        super.onCreate()
        CaocConfig.Builder
            .create()
            .errorActivity(CrashActivity::class.java)
            .restartActivity(MainActivity::class.java)
            .apply()
    }

}