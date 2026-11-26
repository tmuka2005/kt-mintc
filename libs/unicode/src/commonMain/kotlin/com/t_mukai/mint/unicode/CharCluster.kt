package com.t_mukai.mint.unicode

import kotlin.text.unicode.CodePoint
import kotlin.text.unicode.codePointAt
import kotlin.text.unicode.codePointCount

/** An extended grapheme cluster defined by UAX #29. */
public class CharCluster(
  private val input: CharSequence,
  public val start: Int,
  public val end: Int,
) {
  public fun codePoints(): IntArray =
    collectCodePoints()

  public fun matchBase(
    codePoint: CodePoint,
  ): BaseMatch =
    matchBaseAt(findBaseIndex(input, start, end), codePoint)

  public fun displayWidth(): Int =
    measureWidth()

  public fun isNewline(): Boolean =
    isLineTerminator()

  override fun toString(): String =
    input.subSequence(start, end).toString()

  override fun equals(
    other: Any?,
  ): Boolean =
    other is CharCluster && hasSameChars(other)

  override fun hashCode(): Int =
    hashChars()

  // region internal

  private fun collectCodePoints(): IntArray {
    val codePoints = IntArray(input.codePointCount(start, end))
    var index = start
    for (i in codePoints.indices) {
      val codePoint = input.codePointAt(index)
      codePoints[i] = codePoint.code
      index += codePoint.size
    }
    return codePoints
  }

  private fun matchBaseAt(
    baseIndex: Int,
    codePoint: CodePoint,
  ): BaseMatch {
    if (baseIndex < 0) {
      return BaseMatch.Mismatch
    }
    val base = input.codePointAt(baseIndex)
    return when {
      base != codePoint -> {
        BaseMatch.Mismatch
      }

      baseIndex == start && baseIndex + base.size == end -> {
        BaseMatch.Plain
      }

      else -> {
        BaseMatch.Decorated
      }
    }
  }

  private fun measureWidth(): Int {
    val baseIndex = findBaseIndex(input, start, end)
    val base =
      input.codePointAt(
        if (baseIndex < 0) {
          start
        } else {
          baseIndex
        },
      )
    return when {
      containsEmojiPresentationSelector() -> WIDE_WIDTH
      base.isDefaultIgnorable() -> 0
      isEastAsianWide(base) -> WIDE_WIDTH
      else -> 1
    }
  }

  private fun isLineTerminator(): Boolean =
    when (end - start) {
      1 -> input[start] in newlineChars
      2 -> input[start] == '\r' && input[start + 1] == '\n'
      else -> false
    }

  private fun containsEmojiPresentationSelector(): Boolean =
    (start until end).any { input[it] == EMOJI_PRESENTATION_SELECTOR }

  private fun hasSameChars(
    other: CharCluster,
  ): Boolean =
    other.end - other.start == end - start &&
      (0 until end - start).all {
        input[start + it] == other.input[other.start + it]
      }

  private fun hashChars(): Int {
    var hash = 0
    for (index in start until end) {
      hash = HASH_MULTIPLIER * hash + input[index].code
    }
    return hash
  }

  // endregion
}

public enum class BaseMatch { Plain, Decorated, Mismatch }

public class CharClusters(private val input: CharSequence) {
  private val boundaries = clusterBoundaries(input)

  public fun isBoundary(
    index: Int,
  ): Boolean =
    boundaries[index]

  public fun endOf(
    start: Int,
  ): Int =
    (start + 1..input.length).first { boundaries[it] }

  public fun baseIndexAt(
    start: Int,
  ): Int =
    findBaseIndex(input, start, endOf(start))

  public fun clusterAt(
    start: Int,
  ): CharCluster =
    CharCluster(input, start, endOf(start))

  public fun locationOf(
    index: Int,
  ): Location =
    locate(index)

  // region internal

  private fun locate(
    index: Int,
  ): Location {
    var row = 1
    var col = 1
    var start = 0
    while (start < index && endOf(start) <= index) {
      val cluster = clusterAt(start)
      if (cluster.isNewline()) {
        row++
        col = 1
      } else {
        col += columnsOf(cluster)
      }
      start = cluster.end
    }
    return Location(row, col)
  }

  private fun columnsOf(
    cluster: CharCluster,
  ): Int {
    val first = input.codePointAt(cluster.start)
    return when {
      first.code == TAB -> TAB_WIDTH
      directionControlRanges.any { first.code in it } -> 1
      else -> cluster.displayWidth()
    }
  }

  // endregion
}

// region internal

internal fun isEastAsianWide(
  codePoint: CodePoint,
): Boolean =
  wideRanges.rangesContain(codePoint) ||
    fullwidthRanges.rangesContain(codePoint)

private fun findBaseIndex(
  input: CharSequence,
  start: Int,
  end: Int,
): Int {
  var index = start
  while (index < end) {
    val current = input.codePointAt(index)
    if (graphemeBreakOf(current) != GraphemeBreak.Prepend) {
      return index
    }
    index += current.size
  }
  return -1
}

private val newlineChars: Set<Char> =
  ['\n', '\u000B', '\u000C', '\r', '\u0085', '\u2028', '\u2029']

private val directionControlRanges = [0x202A..0x202E, 0x2066..0x2069]

private const val EMOJI_PRESENTATION_SELECTOR = '\uFE0F'

private const val TAB = 0x09

private const val TAB_WIDTH = 4

private const val WIDE_WIDTH = 2

private const val HASH_MULTIPLIER = 31

// endregion
