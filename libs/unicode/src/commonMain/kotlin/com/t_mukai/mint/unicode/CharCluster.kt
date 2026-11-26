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
}

// region internal

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

private const val HASH_MULTIPLIER = 31

// endregion
