package io.github.leonidius20.recorder.data.recorder.amplitudes

import java.nio.ByteBuffer

/**
 * MaxAmplitudeExtractor for PCM 8 bit int
 */
class Int8BitMaxAmpExtractor : MaxAmplitudeExtractor {

    override fun extractFrom(
        buffer: ByteBuffer,
        numberOfChannels: Int,
    ): Int {
        var amp: Byte = 0

        if (numberOfChannels == 1) {
            // mono
            for (index in 0 until buffer.limit()) {
                amp = max(amp, abs(buffer.get(index)))
            }
        } else {
            // stereo
            for (index in 0 until buffer.limit() step 2) {
                val leftAndRightAvg =
                    ((buffer[index] / 2) + (buffer[index + 1] / 2)).toByte()
                amp = max(amp, abs(leftAndRightAvg))
            }
        }

        val ampScaled = ((amp / Byte.MAX_VALUE) * Short.MAX_VALUE).toInt()

        return ampScaled
    }

    private fun abs(byte: Byte): Byte {
        return if (byte < 0)
            (-byte).toByte()
        else
            byte
    }

    private fun max(byte1: Byte, byte2: Byte): Byte {
        return if (byte1 > byte2) byte1 else byte2
    }

}