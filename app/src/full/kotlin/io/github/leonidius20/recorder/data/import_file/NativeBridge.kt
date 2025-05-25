package io.github.leonidius20.recorder.data.import_file

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NativeBridge @Inject constructor() {

    init {
        System.loadLibrary("dummy")
    }

    external fun copyFile(inputFd: Int, outputFd: Int): Boolean

}
