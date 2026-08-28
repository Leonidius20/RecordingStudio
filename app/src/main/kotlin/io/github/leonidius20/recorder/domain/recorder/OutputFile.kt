package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.data.settings.Container

interface OutputFile {

    fun open()

    fun close()

    fun updateMetadata(duration: Long)

}

interface OutputFileFactory {
    fun create(
        namePattern: String,
        format: Container,
    ) : OutputFile
}
