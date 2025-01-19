package io.github.leonidius20.recorder.data.recorder.amplitudes

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.max

class Float32BitMaxAmpExtractor : MaxAmplitudeExtractor {

    override fun extractFrom(buffer: ByteBuffer, numberOfChannels: Int): Int {
        var amp = 0.0f
        val bufferAsFloats = (buffer
            .position(0) as ByteBuffer) // resetting position, otherwise .asFloatBuffer() will have its position at the limit since it was recorder to file and all
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer() // endianness should be built in
            .limit(buffer.limit() / 4) as FloatBuffer // there are 4 bytes (32bit) in float

        if (numberOfChannels == 1) {
            // mono
            for (index in 0 until bufferAsFloats.limit()) {
                amp = max(amp, abs(bufferAsFloats.get(index)))
            }
        } else {
            // stereo
            for (index in 0 until bufferAsFloats.limit() step 2) {
                val leftAndRightAvg =
                    (bufferAsFloats[index] / 2) + (bufferAsFloats[index + 1] / 2)
                amp = max(amp, abs(leftAndRightAvg))
            }
        }

        val ampScaled = (amp * Short.MAX_VALUE).toInt()

        return ampScaled
    }

}