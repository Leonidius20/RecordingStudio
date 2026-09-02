package io.github.leonidius20.recorder.data.settings

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaRecorder
import androidx.annotation.BoolRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.domain.audio_settings.AudioSettingsDataSource
import io.github.leonidius20.domain.audio_settings.DeviceAudioCapabilities
import io.github.leonidius20.domain.audio_settings.bitDepthOrRateForCodecPrefKey
import io.github.leonidius20.domain.audio_settings.defaultCodec
import io.github.leonidius20.domain.audio_settings.getByValue
import io.github.leonidius20.domain.audio_settings.supportedBitRateClosestTo
import io.github.leonidius20.domain.audio_settings.supports
import io.github.leonidius20.domain.audio_settings.supportsBitrate
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class AudioSettingsDataSourceImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val pref: SharedPreferences,
    private val capabilities: DeviceAudioCapabilities,
) : AudioSettingsDataSource {

    // stored here so that it's not garbage collected.
    // prefs only store weak ref
    lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override val settings = callbackFlow {
        val scope = this
        trySend(getCurrentSettingsState())

        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
            scope.launch {
                trySend(getCurrentSettingsState())
            }
        }

        pref.registerOnSharedPreferenceChangeListener(prefListener)

        awaitClose {
            pref.unregisterOnSharedPreferenceChangeListener(prefListener)
            // todo: delete ref
        }
    }

    // todo: read function (or better - state flow with on change subscription) here too

    /**
     * no validation, just reading
     */
    override fun getCurrentSettingsState(): SettingsState<*> {
        val container = Container.getByValue(
            pref.getInt(
                R.string.pref_output_format_key,
                MediaRecorder.OutputFormat.THREE_GPP, // todo: remove reference to android here, Use enum with IDs
            ), capabilities
        )

        var codec = Codec.getByValue(
            pref.getInt(
                R.string.pref_encoder_key,
                container.defaultCodec(capabilities).value,
            ), capabilities
        )
        if (!container.supports(codec, capabilities)) {
            codec = container.defaultCodec(capabilities)
        }


        return buildTypedSettings(container, codec)
    }

    private fun <T: BitRateSettingType> buildTypedSettings(
        container: Container,
        codec: Codec<T>,
    ): SettingsState<T> {
        // todo: find better way?
        @Suppress("UNCHECKED_CAST")
        val resolution = when(val options = codec.resolutionOptions) {
            is BitRateSettingType.BitRateValues -> Resolution.Bitrate(
                value = pref.getFloat(
                    codec.bitDepthOrRateForCodecPrefKey,
                    options.default,
                ).run {
                    val codec = (codec as Codec<BitRateSettingType.BitRateValues>)
                    if (!codec.supportsBitrate(this)) {
                        codec.supportedBitRateClosestTo(this)
                    } else this
                }
            )
            is BitRateSettingType.BitDepthDiscreteValues -> Resolution.BitDepth(
                (codec as (Codec<BitRateSettingType.BitDepthDiscreteValues>)).getBitDepthOptionFromPrefValue(
                    pref.getInt(
                        codec.bitDepthOrRateForCodecPrefKey,
                        options.default.valueForPref
                    )
                )
            )
            is BitRateSettingType.None -> Resolution.None
        } as Resolution<T>

        return SettingsState(
            audioSource = pref.getInt(
                R.string.pref_audio_source_key,
                MediaRecorder.AudioSource.MIC,
            ),
            outputFormat = container,
            encoder = codec,
            numOfChannels = AudioChannels.fromInt(
                pref.getInt(
                    R.string.num_channels_pref_key,
                    AudioChannels.MONO.numberOfChannels()
                )
            ),
            sampleRate = pref.getInt(
                R.string.sample_rate_pref_key,
                codec.supportedSampleRates.first() // todo in sanitization check if device supports this sample rate
                // medianSampleRateSupportedByCodecAndDevice(codec)
            ),
            resolution = resolution,
        )
    }

    /**
     * no validation, just writing
     */
    // todo: make it async and datastore
    override fun saveSettingsToDisk(settings: SettingsState<*>) {
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

    // todo: move default values and keys into code
    //  so as not to depend on context here?
    private fun SharedPreferences.getBoolean(
        @StringRes key: Int,
        @BoolRes defaultValue: Int,
    ) = getBoolean(
        context.getString(key),
        context.resources.getBoolean(defaultValue)
    )

    private fun SharedPreferences.getInt(
        @StringRes key: Int,
        defaultValue: Int,
    ) = getInt(
        context.getString(key),
        defaultValue
    )

}
