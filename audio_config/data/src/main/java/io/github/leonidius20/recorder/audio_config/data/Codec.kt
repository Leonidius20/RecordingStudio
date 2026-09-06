package io.github.leonidius20.recorder.audio_config.data

import io.github.leonidius20.recorder.audio_config.domain.impl.PcmBitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec

internal fun Codec<BitRateSettingType.BitDepthDiscreteValues>.getBitDepthOptionFromPrefValue(prefValue: Int): BitDepthOption {
    return PcmBitDepthOption.entries.find { it.valueForPref == prefValue }!!
}
