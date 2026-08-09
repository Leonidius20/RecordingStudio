package io.github.leonidius20.recorder.data.recorder.amplitudes

import java.nio.ByteBuffer

/**
 * will extract max amplitude value (from 0 to Short.MAX_VALUE) from a PCM
 * byte buffer. Various implementations will allow to work with ints and floats
 * of various lengths
 */
interface MaxAmplitudeExtractor {

    /**
     * @param buffer the assumption is that the limit is set correctly on the buffer
     *               so that garbage values don't take part in the computation
     */
    fun extractFrom(buffer: ByteBuffer, numberOfChannels: Int): Int

}