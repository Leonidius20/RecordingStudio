package io.github.leonidius20.recorder.ui.common

import junit.framework.TestCase.assertEquals
import org.junit.Test

class BreakIntoRangesKtTest {

    @Test
    fun `breaking numbers into ranges`() {
        // i want [14], [8, 9, 10], [4, 5], [2]

        val input = listOf(10, 14, 9, 8, 2, 5, 4)

        val expected = listOf(
            listOf(14), listOf(8, 9, 10), listOf(4, 5), listOf(2),
        )

        val actual = breakIntoRangesDescending(input)

        assertEquals(expected, actual)
    }

}