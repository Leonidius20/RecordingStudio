package io.github.leonidius20.recorder.domain.recorder

import io.github.leonidius20.recorder.entities.audio_settings.Container

interface OutputFileFactory {
    fun create(
        namePattern: String,
        format: Container,
    ) : OutputFile
}
