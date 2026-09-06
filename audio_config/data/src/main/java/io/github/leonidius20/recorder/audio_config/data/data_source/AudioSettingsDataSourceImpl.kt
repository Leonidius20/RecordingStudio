package io.github.leonidius20.recorder.audio_config.data.data_source

import android.content.SharedPreferences
import android.media.MediaRecorder
import androidx.core.content.edit
import io.github.leonidius20.recorder.audio_config.data.getBitDepthOptionFromPrefValue
import io.github.leonidius20.recorder.audio_config.data.valueForPref
import io.github.leonidius20.recorder.audio_config.data.repository.AudioSettingsDataSource
import io.github.leonidius20.recorder.audio_config.domain.impl.DeviceAudioCapabilities
import io.github.leonidius20.recorder.audio_config.domain.impl.bitDepthOrRateForCodecPrefKey
import io.github.leonidius20.recorder.audio_config.domain.impl.defaultCodec
import io.github.leonidius20.recorder.audio_config.domain.impl.getByValue
import io.github.leonidius20.recorder.audio_config.domain.impl.supportedBitRateClosestTo
import io.github.leonidius20.recorder.audio_config.domain.impl.supports
import io.github.leonidius20.recorder.audio_config.domain.impl.supportsBitrate
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

    /**
     * no validation, just reading
     */
    override fun getCurrentSettingsState(): SettingsState<*> {
        val container = Container.getByValue(
            pref.getInt(
                PREF_OUTPUT_FORMAT_KEY,
                MediaRecorder.OutputFormat.THREE_GPP, // todo: remove reference to android here, Use enum with IDs
            ), capabilities
        )

        var codec = Codec.getByValue(
            pref.getInt(
                PREF_ENCODER_KEY,
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
                PREF_AUDIO_SOURCE_KEY,
                MediaRecorder.AudioSource.MIC,
            ),
            outputFormat = container,
            encoder = codec,
            numOfChannels = AudioChannels.fromInt(
                pref.getInt(
                    PREF_NUM_CHANNELS_KEY,
                    AudioChannels.MONO.numberOfChannels()
                )
            ),
            sampleRate = pref.getInt(
                PREF_SAMPLE_RATE_KEY,
                // in sanitation, we will check if device supports this sample rate
                codec.supportedSampleRates.first()
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
                PREF_AUDIO_SOURCE_KEY,
                settings.audioSource
            )

            putInt(
                PREF_OUTPUT_FORMAT_KEY,
                settings.outputFormat.value
            )

            putInt(
                PREF_ENCODER_KEY,
                settings.encoder.value
            )

            putInt(
                PREF_NUM_CHANNELS_KEY,
                settings.numOfChannels.numberOfChannels()
            )

            putInt(PREF_SAMPLE_RATE_KEY,
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

    companion object {
        private const val PREF_OUTPUT_FORMAT_KEY = "output_format"
        private const val PREF_ENCODER_KEY = "codec"
        private const val PREF_AUDIO_SOURCE_KEY = "audio_source"
        private const val PREF_NUM_CHANNELS_KEY = "num_channels"
        private const val PREF_SAMPLE_RATE_KEY = "sample_rate_pref_key"
    }

}
