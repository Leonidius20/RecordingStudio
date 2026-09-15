package io.github.leonidius20.recorder.ui.settings

import androidx.fragment.app.Fragment

abstract class ImportFileSettingHandler {

    lateinit var fragment: Fragment

    fun attach(fragment: Fragment): ImportFileSettingHandler {
        this.fragment = fragment
        register()
        return this
    }

    protected abstract fun register()

    abstract fun launchImport()

}
