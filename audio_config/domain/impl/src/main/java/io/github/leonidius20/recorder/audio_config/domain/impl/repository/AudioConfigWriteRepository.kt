package io.github.leonidius20.recorder.audio_config.domain.impl.repository

import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container

interface AudioConfigWriteRepository {

    fun setAudioSource(value: Int)

    fun setOutputFormat(format: Container)

    fun setCodec(codec: Codec<*>)

    fun setNumberOfChannels(channels: AudioChannels)

    fun setSampleRate(rate: Int)

    fun setBitDepth(bitDepth: BitDepthOption)

    fun setBitRate(rate: Float)

}
