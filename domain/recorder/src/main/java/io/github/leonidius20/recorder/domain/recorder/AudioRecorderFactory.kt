package io.github.leonidius20.recorder.domain.recorder

import java.io.IOException

// todo: replace with a lambda?
interface AudioRecorderFactory {

    @Throws(IOException::class)
    fun create(
        file: OutputFile,
    ): AudioRecorder

}
