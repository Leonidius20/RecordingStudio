package io.github.leonidius20.recorder.data.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import javax.inject.Inject

class AudioSettingsDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pref: SharedPreferences,
) {

    // todo: read function (or better - state flow with on change subscription) here too

    /**
     * no validation, just writing
     */
    // todo: make it async and datastore
    fun saveSettingsToDisk(settings: SettingsState<*>) {
        pref.edit {
            putInt(
                context.getString(R.string.pref_audio_source_key),
                settings.audioSource
            )

            putInt(
                context.getString(R.string.pref_output_format_key),
                settings.outputFormat.value
            )

            putInt(
                context.getString(R.string.pref_encoder_key),
                settings.encoder.value
            )

            putInt(
                context.getString(R.string.num_channels_pref_key),
                settings.numOfChannels.numberOfChannels()
            )

            putInt(context.getString(R.string.sample_rate_pref_key),
                settings.sampleRate)

            when (val res = settings.resolution) {
                is Resolution.Bitrate -> {
                    putFloat(
                        settings.encoder.bitDepthOrRateForCodecPrefKey,
                        res.value
                    )
                }

                is Resolution.BitDepth -> {
                    putInt(
                        settings.encoder.bitDepthOrRateForCodecPrefKey,
                        res.value.valueForPref
                    )
                }

                is Resolution.None -> {}
            }
        }
    }

}
