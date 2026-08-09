package io.github.leonidius20.recorder.data.recorder.amplitudes

import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.max

class Int16BitMaxAmpExtractor : MaxAmplitudeExtractor {

    override fun extractFrom(buffer: ByteBuffer, numberOfChannels: Int): Int {
        val bitsPerSample = 16
        val bytesPerSample = (bitsPerSample / 8)
        val bytesPerInstant = bytesPerSample * numberOfChannels

        var amp = 0

        for (offset in 0 until buffer.limit() step bytesPerInstant) {
            if (numberOfChannels == 1) {
                var sample = 0

                // convert little endian number to int
                // most significant byte is at highest address
                for (position in bytesPerSample - 1 downTo 0) {
                    sample = sample shl 8
                    sample += buffer[offset + position]
                }

                amp = max(amp, abs(sample))
            } else {

                val secondSampleOffset = bytesPerSample

                var leftSample = 0
                for (position in bytesPerSample - 1 downTo 0) {
                    leftSample = leftSample shl 8
                    leftSample += buffer[offset + position]
                }

                var rightSample = 0
                for (position in bytesPerSample - 1 downTo 0) {
                    rightSample = rightSample shl 8
                    rightSample += buffer[offset + secondSampleOffset + position]
                }

                val leftAndRightAverage =
                    (leftSample / 2) + (rightSample / 2) // making sure they don't overflow

                amp = max(amp, abs(leftAndRightAverage))
            }
        }

        val ampScaled = amp // we are already working with Signed Short values, no need for scaling

        return ampScaled
    }

}