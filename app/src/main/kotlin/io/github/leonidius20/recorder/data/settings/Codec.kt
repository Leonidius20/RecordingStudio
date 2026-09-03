package io.github.leonidius20.recorder.data.settings

import io.github.leonidius20.recorder.domain.audio_settings.PcmBitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec

fun Codec<BitRateSettingType.BitDepthDiscreteValues>.getBitDepthOptionFromPrefValue(prefValue: Int): BitDepthOption {
    return PcmBitDepthOption.entries.find { it.valueForPref == prefValue }!!
}
