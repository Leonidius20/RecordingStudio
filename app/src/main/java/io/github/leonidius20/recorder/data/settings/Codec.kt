package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlin.math.abs

enum class Codec(
    /**
     * value as expected by MediaRecorder.setAudioEncoder()
     */
    val value: Int,
    val displayName: String,
    val isSupportedByDevice: Boolean,
    val supportedSampleRates: IntArray,
) {

    // todo: check support some other way too

    AMR_NB(
        MediaRecorder.AudioEncoder.AMR_NB,
        "AMR Narrowband",
        true,
        supportedSampleRates = intArrayOf(8_000),
    ),

    AMR_WB(
        MediaRecorder.AudioEncoder.AMR_WB,
        "AMR Wideband",
        true,
        supportedSampleRates = intArrayOf(16_000)
    ),

    AAC(
        MediaRecorder.AudioEncoder.AAC,
        "AAC-LC",
        true,
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),
    ),

    HE_AAC(
        MediaRecorder.AudioEncoder.HE_AAC,
        "HE-AAC",
        true,
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),
    ),

    AAC_ELD(
        MediaRecorder.AudioEncoder.AAC_ELD,
        "AAC-ELD",
        true,
        supportedSampleRates = intArrayOf(16000, 22050, 24000, 32000, 44100, 48000),
    ),

    @RequiresApi(Build.VERSION_CODES.Q)
    OPUS(
        MediaRecorder.AudioEncoder.OPUS,
        "Opus",
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        // todo: does it really support all these rates?
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),

        ),

    PCM(
        value = -1,
        displayName = "PCM",
        isSupportedByDevice = true,
        supportedSampleRates = intArrayOf(8000, 11025, 12000, 16000, 22050, 24000, 32000, 44100, 48000),
    );

    fun supportedSampleRateClosestTo(rate: Int): Int {
        return supportedSampleRates.mapIndexed { index, supportedRate ->
            val distance = abs(rate - supportedRate)
            index to distance
        }.minBy { it.second }.let { (index, _) -> supportedSampleRates[index] }
    }

    fun supportsSampleRate(rate: Int) = rate in supportedSampleRates

    companion object {

        private val map by lazy {
            Codec.entries.associateBy { it.value }
        }

        fun getByValue(value: Int) = map[value]!!

    }

}
