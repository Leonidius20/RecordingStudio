package io.github.leonidius20.recorder.data.import_file

class NativeBridge {

    init {
        System.loadLibrary("dummy")
    }

    external fun copyFile(inputFd: Int, outputFd: Int): Boolean

}