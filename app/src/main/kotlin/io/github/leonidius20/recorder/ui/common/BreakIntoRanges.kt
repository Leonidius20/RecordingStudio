package io.github.leonidius20.recorder.ui.common

/**
 * given a list of unsigned integers without duplicates (unsorted), sort descending and
 * break it into ranges of continuous numbers
 * @return ranges sorted descending (but numbers within a range are sorted ascending).
 * Numbers that do not belong to a range are considered to be a 1-number range of their own.
 * Example output: [14], [8, 9, 10], [4, 5], [2]
 */
fun breakIntoRangesDescending(numbersUnsorted: List<Int>): List<List<Int>> {
    val numbers = ArrayList(numbersUnsorted.sortedDescending())

    val ranges: MutableList<List<Int>> = mutableListOf()

    while (numbers.isNotEmpty()) {
        val thisRange: MutableList<Int> = mutableListOf()

        var lastItem = numbers.removeLast()

        thisRange.add(lastItem)

        while (numbers.isNotEmpty()) {
            val penultimate = numbers.last()
            if (penultimate == lastItem + 1) { // since it is sorted descending
                thisRange.add(penultimate)
                lastItem = penultimate
                numbers.removeLast()
            } else break // end of range
        }

        ranges.add(thisRange) // because we were adding to end of list, the range will be in natural order
    }

    return ranges.reversed()
}