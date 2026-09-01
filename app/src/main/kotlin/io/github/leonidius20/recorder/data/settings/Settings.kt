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
import io.github.leonidius20.recorder.data.common.di.Dispatcher
import io.github.leonidius20.recorder.data.common.di.Scope
import io.github.leonidius20.recorder.domain.settings.SettingsInterface
import io.github.leonidius20.recorder.entities.audio_settings.AudioChannels
import io.github.leonidius20.recorder.entities.audio_settings.BitDepthOption
import io.github.leonidius20.recorder.entities.audio_settings.BitRateSettingType
import io.github.leonidius20.recorder.entities.audio_settings.Codec
import io.github.leonidius20.recorder.entities.audio_settings.Container
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
import timber.log.Timber
import java.util.SortedSet
import javax.inject.Inject
import javax.inject.Singleton

// todo: lift to domain
data class UserSettings(
    val stopOnLowBattery: Boolean = true,
    val stopOnLowStorage: Boolean = true,
    val pauseOnCall: Boolean = false,
)

data class AudioConfig(
    val audioSource: Int,
    val outputFormat: Container,
    val encoder: Codec,
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

    private fun getCurrentSettingsState(): SettingsState {
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

        val bitDepth = (codec.bitRateSettingType as? BitRateSettingType.BitDepthDiscreteValues)?.let {
            try {
                codec.getBitDepthOptionFromPrefValue(
                    pref.getInt(
                        codec.bitDepthOrRateForCodecPrefKey,
                        it.default.valueForPref
                    )
                )
            } catch (t: Throwable) {
                Timber.e(t)
                it.default
            }
        }

        val bitRate = (codec.bitRateSettingType as? BitRateSettingType.BitRateValues)?.let {
            pref.getFloat(
                codec.bitDepthOrRateForCodecPrefKey,
                it.default,
            )
        }

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
            bitDepth = bitDepth,
            bitRate = bitRate,
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

    data class AudioSourceOption(
        /**
         * value expected by MediaRecorder.setAudioSource()
         */
        val value: Int,
        val name: String,
        val description: String,
    ) {

        companion object {
            val DEFAULT = AudioSourceOption(
                MediaRecorder.AudioSource.DEFAULT,
                "Default",
                "Default audio input. Some processing may be applied by device"
            )
        }

    }

    val audioSourceOptions = buildList {
        // todo: localize
        addAll(
            listOf(
                AudioSourceOption.DEFAULT,
                AudioSourceOption(
                    MediaRecorder.AudioSource.MIC,
                    "Mic",
                    "Regular microphone input (some processing may be applied by device)"
                ),
                AudioSourceOption(
                    MediaRecorder.AudioSource.CAMCORDER,
                    "Camcorder",
                    "Input tuned for video recording. If there are many microphones, this would be the one with the same orientation as the camera"
                ),
                AudioSourceOption(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    "Voice recognition",
                    "Tuned for voice recognition"
                ),
                AudioSourceOption(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    "Voice communication",
                    "Tuned for VoIP and the like. Applies processing like echo cancellation or gain control (determined by device manufacturer)"
                ),
            )
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            add(
                AudioSourceOption(
                    MediaRecorder.AudioSource.UNPROCESSED,
                    "Unprocessed",
                    "No processing if the phone supports it, default otherwise"
                )
            )
        }
    }

    fun setAudioSource(value: Int) {
        val key = context.getString(R.string.pref_audio_source_key)

        // todo: migrate to datastore that provides async api.
        //  we can do away with the change listeners too
        pref.edit().putInt(
            key,
            value
        ).apply()

        // the listener only exists while the SettingsFragment is started,
        // so we call manually.
        onSharedPreferenceChanged()
    }

    fun setOutputFormat(format: Container) {
        val key = context.getString(R.string.pref_output_format_key)

        val editingPref = pref.edit().putInt(
            key, format.value
        )

        val currentCodec = state.value.encoder
        if (!format.supports(currentCodec)) {
            setCodec(format.defaultCodec, fireChangeListener = false)
        }

        editingPref.apply()

        // the listener only exists while the SettingsFragment is started,
        // so we call manually.
        onSharedPreferenceChanged()
        // we don't need to call this for the changed codec, as long as
        // this function reloads all of the settings every time
    }

    fun setCodec(codec: Codec, fireChangeListener: Boolean = true) {
        val key = context.getString(R.string.pref_encoder_key)

        pref.edit().putInt(
            key, codec.value
        ).apply()

        val currentSampleRate = state.value.sampleRate

        if (!codec.supportsSampleRate(currentSampleRate)) {
            setSampleRate(
                codec.supportedSampleRateClosestTo(currentSampleRate),
                fireChangeListener = false
            )
        }

        // the listener only exists while the SettingsFragment is started,
        // so we call manually.
        if (fireChangeListener)
            onSharedPreferenceChanged()
    }

    fun setNumberOfChannels(channels: AudioChannels) {
        val key = context.getString(R.string.num_channels_pref_key)

        pref.edit().putInt(key, channels.numberOfChannels())
            .apply()

        onSharedPreferenceChanged()
    }

    fun setSampleRate(rate: Int, fireChangeListener: Boolean = true) {

        val key = context.getString(R.string.sample_rate_pref_key)

        pref.edit().putInt(key, rate)
            .apply()

        if (fireChangeListener)
            onSharedPreferenceChanged()
    }


    fun setBitDepth(bitDepth: BitDepthOption) {
        require(
            state.value.encoder.bitRateSettingType
                    is BitRateSettingType.BitDepthDiscreteValues
        )

        val key = state.value.encoder.bitDepthOrRateForCodecPrefKey

        pref.edit().putInt(key, bitDepth.valueForPref)
            .apply()

        onSharedPreferenceChanged()
    }

    fun setBitRate(rate: Float) {
        require(state.value.encoder.bitRateSettingType
                is BitRateSettingType.BitRateValues)

        val key = state.value.encoder.bitDepthOrRateForCodecPrefKey

        pref.edit().putFloat(key, rate)
            .apply()

        onSharedPreferenceChanged()
    }

    /**
     * returns not the highest and not the lowest sample rate supported by codec
     * and device. Some middle value. The reason for this is that the highest sample
     * rate sounds bad with the default bitrate, and we have not implemented changing
     * the latter yet.
     */
    private fun medianSampleRateSupportedByCodecAndDevice(codec: Codec): Int {
        val rates = codec.supportedSampleRates.intersect(
            sampleRatesSupportedByDevice
        ).toIntArray()

        val middleIndex = rates.size / 2

        return rates[middleIndex]
    }

}
