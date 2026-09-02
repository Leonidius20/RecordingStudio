package io.github.leonidius20.domain.audio_settings

import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.CodecId
import io.github.leonidius20.recorder.entities.audio_settings.Container
import java.util.SortedSet

interface DeviceAudioCapabilities {
    /**
     * sample rates supported by device. There is also a separate thing which is
     * sample rates supported by various codecs.
     */
    val sampleRatesSupportedByDevice: SortedSet<Int>

    val  containers: List<Container>

    val containersMap: Map<Int, Container>

    val codecs: List<Codec<out BitRateSettingType>>

    val codecById: Map<CodecId, Codec<out BitRateSettingType>>

    val codecByValue: Map<Int, Codec<out BitRateSettingType>>

}
