package io.github.leonidius20.recorder.data.settings

import android.media.MediaRecorder
import android.os.Build
import androidx.annotation.RequiresApi

enum class Codec(
    /**
     * value as expected by MediaRecorder.setAudioEncoder()
     */
    val value: Int,
    val displayName: String,
    val isSupportedByDevice: Boolean,
) {

    // todo: check support some other way too

    AMR_NB(MediaRecorder.AudioEncoder.AMR_NB, "AMR Narrowband", true),
    AMR_WB(MediaRecorder.AudioEncoder.AMR_WB, "AMR Wideband", true),
    AAC(MediaRecorder.AudioEncoder.AAC, "AAC", true),
    HE_AAC(MediaRecorder.AudioEncoder.HE_AAC, "HE-AAC", true),
    AAC_ELD(MediaRecorder.AudioEncoder.AAC_ELD, "AAC-ELD", true),

    @RequiresApi(Build.VERSION_CODES.Q)
    OPUS(MediaRecorder.AudioEncoder.OPUS, "Opus", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q),

    PCM(
        value = -1,
        displayName = "PCM",
        isSupportedByDevice = true,
    );

    companion object {

        private val map by lazy {
            Codec.entries.associateBy { it.value }
        }

        fun getByValue(value: Int) = map[value]!!

    }

}
