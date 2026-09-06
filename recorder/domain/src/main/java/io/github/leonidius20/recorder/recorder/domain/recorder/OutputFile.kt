package io.github.leonidius20.recorder.recorder.domain.recorder

interface OutputFile {

    fun open()

    fun close()

    fun updateMetadata(duration: Long)

}
