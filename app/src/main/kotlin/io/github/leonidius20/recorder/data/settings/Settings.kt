package io.github.leonidius20.recorder.data.settings

import android.app.Application.AUDIO_SERVICE
import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.BoolRes
import androidx.annotation.StringRes
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.leonidius20.recorder.R
import io.github.leonidius20.recorder.di.Dispatcher
import io.github.leonidius20.recorder.di.Scope
import io.github.leonidius20.recorder.domain.settings.SettingsInterface
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
import io.github.leonidius20.recorder.entities.audio_settings.Resolution
import io.github.leonidius20.recorder.entities.audio_settings.SettingsState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.SortedSet
import javax.inject.Inject
import javax.inject.Singleton

// todo: lift to domain
data class UserSettings(
    val stopOnLowBattery: Boolean = true,
    val stopOnLowStorage: Boolean = true,
    val pauseOnCall: Boolean = false,
)

// todo: parametrize properly
data class AudioConfig(
    val audioSource: Int,
    val outputFormat: Container,
    val encoder: Codec<*>,
    val numOfChannels: AudioChannels,
    val sampleRate: Int,
    val audioResolution: BitRateSettingType,
)


interface UserSettingsReadRepository {

    val userSettings: StateFlow<UserSettings>

}

@Singleton
class UserSettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:Dispatcher.Io private val ioDispatcher: CoroutineDispatcher,
    @param:Scope.App private val appScope: CoroutineScope,
) : UserSettingsReadRepository {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    // stored here so that it's not garbage collected.
    // prefs only store weak ref
    lateinit var prefListener: SharedPreferences.OnSharedPreferenceChangeListener

    override val userSettings = callbackFlow {
        val scope = this
        trySend(getData())

        prefListener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
            scope.launch {
                trySend(getData())
            }
        }

        pref.registerOnSharedPreferenceChangeListener(prefListener)

        awaitClose {
            pref.unregisterOnSharedPreferenceChangeListener(prefListener)
            // todo: delete ref
        }
    }.stateIn(appScope, SharingStarted.Eagerly, UserSettings())

    private suspend fun getData() = withContext(ioDispatcher) {
        UserSettings(
            stopOnLowBattery = pref.getBoolean(
                R.string.stop_on_low_battery_pref_key,
                R.bool.stop_on_low_battery_default
            ),
            stopOnLowStorage = pref.getBoolean(
                R.string.stop_on_low_storage_pref_key,
                R.bool.stop_on_storage_default
            ),
            pauseOnCall = pref.getBoolean(
                R.string.pause_on_call_pref_key,
                R.bool.pause_on_call_default
            ),
        )
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

}

@Singleton
class Settings @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val dataSource: AudioSettingsDataSource,
    ) : SettingsInterface {

    private val pref = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * sample rates supported by device. There is also a separate thing which is
     * sample rates supported by various codecs.
     */
    val sampleRatesSupportedByDevice: SortedSet<Int> =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context.getSystemService(AUDIO_SERVICE) as AudioManager)
                .getDevices(AudioManager.GET_DEVICES_INPUTS)
                .firstOrNull()
                ?.sampleRates
                ?.toSortedSet()
                ?.let {
                    // if empty, means the device supports arbitrary values with resampling.
                    // we will just stick to some standard ones
                    if (it.isEmpty()) null else it
                }
        } else {
            null
        }) ?: sortedSetOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000)


    private val _state = MutableStateFlow(getCurrentSettingsState())

    override val state = _state.asStateFlow()

    fun onSharedPreferenceChanged() {
        _state.value = getCurrentSettingsState()
    }

    private fun getCurrentSettingsState(): SettingsState<*> {
        val container = Container.getByValue(
            pref.getInt(
                R.string.pref_output_format_key,
                MediaRecorder.OutputFormat.THREE_GPP,
            )
        )

        var codec = Codec.getByValue(
            pref.getInt(
                R.string.pref_encoder_key,
                container.defaultCodec.value,
            )
        )
        if (!container.supports(codec)) {
            codec = container.defaultCodec
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
            stopOnLowBattery = pref.getBoolean(
                R.string.stop_on_low_battery_pref_key,
                R.bool.stop_on_low_battery_default
            ),
            stopOnLowStorage = pref.getBoolean(
                R.string.stop_on_low_storage_pref_key,
                R.bool.stop_on_storage_default
            ),
            pauseOnCall = pref.getBoolean(
                R.string.pause_on_call_pref_key,
                R.bool.pause_on_call_default
            ),
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
                medianSampleRateSupportedByCodecAndDevice(codec)
            ),
            resolution = resolution,
        )
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

    fun setAudioSource(value: Int) {
        sanitizeAndWriteSettings(state.value, audioSource = value)
    }

    fun setOutputFormat(format: Container) {
        sanitizeAndWriteSettings(state.value,
            outputFormat = format)
    }

    fun setCodec(codec: Codec<*>) {
        sanitizeAndWriteSettings(
            state.value,
            encoder = codec
        )
    }

    fun setNumberOfChannels(channels: AudioChannels) {
        sanitizeAndWriteSettings(
            state.value,
            numOfChannels = channels
        )
    }

    fun setSampleRate(rate: Int) {
        sanitizeAndWriteSettings(
            state.value,
            sampleRate = rate
        )
    }


    fun setBitDepth(bitDepth: BitDepthOption) {
        require(
            state.value.encoder.resolutionOptions
                    is BitRateSettingType.BitDepthDiscreteValues
        )

        sanitizeAndWriteSettings(
            state.value,
            resolution = Resolution.BitDepth(bitDepth)
        )
    }

    fun setBitRate(rate: Float) {
        require(state.value.encoder.resolutionOptions
                is BitRateSettingType.BitRateValues)

        sanitizeAndWriteSettings(
            state.value,
            resolution = Resolution.Bitrate(rate)
        )
    }

    /**
     * returns not the highest and not the lowest sample rate supported by codec
     * and device. Some middle value. The reason for this is that the highest sample
     * rate sounds bad with the default bitrate, and we have not implemented changing
     * the latter yet.
     */
    private fun medianSampleRateSupportedByCodecAndDevice(codec: Codec<*>): Int {
        val rates = codec.supportedSampleRates.intersect(
            sampleRatesSupportedByDevice
        ).toIntArray()

        val middleIndex = rates.size / 2

        return rates[middleIndex]
    }

    // todo: one function to update (write updated) the settings.
    //  one function to sanitize

    fun sanitizeAndWriteSettings(
        settings: SettingsState<*>,
        audioSource: Int = settings.audioSource,
        outputFormat: Container = settings.outputFormat,
        encoder: Codec<*> = settings.encoder,
        numOfChannels: AudioChannels = settings.numOfChannels,
        sampleRate: Int = settings.sampleRate,
        resolution: Resolution<*> = settings.resolution,
    ) {
        dataSource.saveSettingsToDisk(
            sanitizeSettings(
                settings, audioSource, outputFormat, encoder, numOfChannels, sampleRate, resolution
            )
        )
        onSharedPreferenceChanged()
    }

    private fun sanitizeSettings(
        settings: SettingsState<*>,
        audioSource: Int = settings.audioSource,
        outputFormat: Container = settings.outputFormat,
        encoder: Codec<*> = settings.encoder,
        numOfChannels: AudioChannels = settings.numOfChannels,
        sampleRate: Int = settings.sampleRate,
        resolution: Resolution<*> = settings.resolution,
    ): SettingsState<*> {

        val encoder = if (!outputFormat.supports(encoder)) {
            outputFormat.defaultCodec
        } else encoder

        val sampleRate = if (!encoder.supportsSampleRate(sampleRate)) {
            encoder.supportedSampleRateClosestTo(sampleRate)
        } else sampleRate

        return when(encoder.resolutionOptions) {
            is BitRateSettingType.None -> {
                SettingsState(
                    stopOnLowBattery = settings.stopOnLowBattery,
                    stopOnLowStorage = settings.stopOnLowStorage,
                    pauseOnCall = settings.pauseOnCall,
                    audioSource = audioSource,
                    outputFormat = outputFormat,
                    encoder = encoder as Codec<BitRateSettingType.None>,
                    numOfChannels = numOfChannels,
                    sampleRate = sampleRate,
                    resolution = Resolution.None
                )
            }
            is BitRateSettingType.BitRateValues -> {
                SettingsState(
                    stopOnLowBattery = settings.stopOnLowBattery,
                    stopOnLowStorage = settings.stopOnLowStorage,
                    pauseOnCall = settings.pauseOnCall,
                    audioSource = audioSource,
                    outputFormat = outputFormat,
                    encoder = encoder as Codec<BitRateSettingType.BitRateValues>,
                    numOfChannels = numOfChannels,
                    sampleRate = sampleRate,
                    resolution = Resolution.Bitrate(
                        if (resolution is Resolution.Bitrate) {
                            val prev = resolution.value

                            if (!encoder.supportsBitrate(prev)) {
                                encoder.supportedBitRateClosestTo(prev)
                            } else prev
                        } else encoder.resolutionOptions.default
                    )
                )
            }
            is BitRateSettingType.BitDepthDiscreteValues -> {
                SettingsState(
                    stopOnLowBattery = settings.stopOnLowBattery,
                    stopOnLowStorage = settings.stopOnLowStorage,
                    pauseOnCall = settings.pauseOnCall,
                    audioSource = audioSource,
                    outputFormat = outputFormat,
                    encoder = encoder as Codec<BitRateSettingType.BitDepthDiscreteValues>,
                    numOfChannels = numOfChannels,
                    sampleRate = sampleRate,
                    resolution = Resolution.BitDepth(
                        if (resolution is Resolution.BitDepth) resolution.value
                        else encoder.resolutionOptions.default
                    )
                )
            }
        }
    }

}
