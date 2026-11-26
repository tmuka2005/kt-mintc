package com.t_mukai.mint.unicode

import kotlin.text.unicode.CodePoint
import kotlin.text.unicode.category

public fun CodePoint.isCombiningMark(): Boolean =
  category in combiningMarkCategories

public fun CodePoint.isVariationSelector(): Boolean =
  variationSelectorRanges.any { code in it }

public fun CodePoint.isXidStart(): Boolean =
  xidStartRanges.rangesContain(this)

public fun CodePoint.isXidContinue(): Boolean =
  xidContinueRanges.rangesContain(this)

public fun CodePoint.isDefaultIgnorable(): Boolean =
  defaultIgnorableRanges.rangesContain(this)

public fun CodePoint.isJoinControl(): Boolean =
  code == ZERO_WIDTH_NON_JOINER || code == ZERO_WIDTH_JOINER

/** Returns -1 when this code point is not a digit in [radix]. */
public expect fun CodePoint.digitValue(
  radix: Int,
): Int

// region internal

internal fun IntArray.rangesContain(
  codePoint: CodePoint,
): Boolean =
  binarySearchRanges(this, codePoint.code)

private fun binarySearchRanges(
  ranges: IntArray,
  value: Int,
): Boolean {
  var low = 0
  var high = ranges.size / 2 - 1
  while (low <= high) {
    val middle = low + (high - low) / 2
    when {
      value < ranges[middle * 2] -> high = middle - 1
      value > ranges[middle * 2 + 1] -> low = middle + 1
      else -> return true
    }
  }
  return false
}

private val combiningMarkCategories =
  setOf(
    CharCategory.NON_SPACING_MARK,
    CharCategory.COMBINING_SPACING_MARK,
  )

private val variationSelectorRanges =
  [
    0x180B..0x180D,
    0x180F..0x180F,
    0xFE00..0xFE0F,
    0xE0100..0xE01EF,
  ]

private const val ZERO_WIDTH_NON_JOINER = 0x200C

private const val ZERO_WIDTH_JOINER = 0x200D

// endregion
