package com.t_mukai.mint.unicode

import kotlin.text.unicode.CodePoint
import kotlin.text.unicode.codePointAt

internal enum class GraphemeBreak {
  Cr,
  Lf,
  Control,
  Extend,
  Zwj,
  RegionalIndicator,
  Prepend,
  SpacingMark,
  L,
  V,
  T,
  Lv,
  Lvt,
  Other,
}

internal enum class ConjunctBreak { Consonant, Extend, Linker, None }

internal fun graphemeBreakOf(
  codePoint: CodePoint,
): GraphemeBreak =
  lookUpGraphemeBreak(codePoint)

internal fun conjunctBreakOf(
  codePoint: CodePoint,
): ConjunctBreak =
  lookUpConjunctBreak(codePoint)

internal fun isExtendedPictographic(
  codePoint: CodePoint,
): Boolean =
  codePoint.code >= DELETE &&
    extendedPictographicRanges.rangesContain(codePoint)

internal fun clusterBoundaries(
  input: CharSequence,
): BooleanArray =
  scanClusterBoundaries(input)

private fun lookUpGraphemeBreak(
  codePoint: CodePoint,
): GraphemeBreak {
  val value = codePoint.code
  return when {
    value == CARRIAGE_RETURN -> GraphemeBreak.Cr
    value == LINE_FEED -> GraphemeBreak.Lf
    value < ' '.code || value == DELETE -> GraphemeBreak.Control
    value < DELETE -> GraphemeBreak.Other
    value == ZERO_WIDTH_JOINER -> GraphemeBreak.Zwj
    value in REGIONAL_INDICATORS -> GraphemeBreak.RegionalIndicator
    value in HANGUL_SYLLABLES -> hangulSyllableBreakOf(value)
    else -> rangedGraphemeBreakOf(codePoint)
  }
}

private fun rangedGraphemeBreakOf(
  codePoint: CodePoint,
): GraphemeBreak =
  when {
    controlRanges.rangesContain(codePoint) -> GraphemeBreak.Control
    extendRanges.rangesContain(codePoint) -> GraphemeBreak.Extend
    spacingMarkRanges.rangesContain(codePoint) -> GraphemeBreak.SpacingMark
    prependRanges.rangesContain(codePoint) -> GraphemeBreak.Prepend
    hangulLeadingRanges.rangesContain(codePoint) -> GraphemeBreak.L
    hangulVowelRanges.rangesContain(codePoint) -> GraphemeBreak.V
    hangulTrailingRanges.rangesContain(codePoint) -> GraphemeBreak.T
    else -> GraphemeBreak.Other
  }

private fun lookUpConjunctBreak(
  codePoint: CodePoint,
): ConjunctBreak =
  when {
    codePoint.code < DELETE -> ConjunctBreak.None
    conjunctConsonantRanges.rangesContain(codePoint) -> ConjunctBreak.Consonant
    conjunctExtendRanges.rangesContain(codePoint) -> ConjunctBreak.Extend
    conjunctLinkerRanges.rangesContain(codePoint) -> ConjunctBreak.Linker
    else -> ConjunctBreak.None
  }

private fun scanClusterBoundaries(
  input: CharSequence,
): BooleanArray {
  val boundaries = BooleanArray(input.length + 1)
  if (input.isEmpty()) {
    return boundaries
  }
  boundaries[0] = true
  boundaries[input.length] = true
  val first = input.codePointAt(0)
  var previousBreak = graphemeBreakOf(first)
  var afterPictographic = isExtendedPictographic(first)
  var afterEmojiJoiner = false
  var afterLinker = conjunctBreakOf(first) == ConjunctBreak.Linker
  var regionalIndicatorCount =
    if (previousBreak == GraphemeBreak.RegionalIndicator) {
      1
    } else {
      0
    }
  var index = first.size
  while (index < input.length) {
    val current = input.codePointAt(index)
    val currentBreak = graphemeBreakOf(current)
    val pictographic = isExtendedPictographic(current)
    val conjunct = conjunctBreakOf(current)
    boundaries[index] =
      isBoundary(
        previousBreak,
        currentBreak,
        joinsConjunct = afterLinker && conjunct == ConjunctBreak.Consonant,
        joinsEmoji = afterEmojiJoiner && pictographic,
        regionalIndicatorCount,
      )
    afterEmojiJoiner = afterPictographic && currentBreak == GraphemeBreak.Zwj
    afterPictographic =
      pictographic ||
      (afterPictographic && currentBreak == GraphemeBreak.Extend)
    afterLinker =
      conjunct == ConjunctBreak.Linker ||
      (afterLinker && conjunct == ConjunctBreak.Extend)
    regionalIndicatorCount =
      if (currentBreak == GraphemeBreak.RegionalIndicator) {
        regionalIndicatorCount + 1
      } else {
        0
      }
    previousBreak = currentBreak
    index += current.size
  }
  return boundaries
}

private fun isBoundary(
  previous: GraphemeBreak,
  current: GraphemeBreak,
  joinsConjunct: Boolean,
  joinsEmoji: Boolean,
  regionalIndicatorCount: Int,
): Boolean =
  when {
    previous == GraphemeBreak.Cr && current == GraphemeBreak.Lf -> {
      false
    }

    previous.isControl() || current.isControl() -> {
      true
    }

    continuesHangulSyllable(previous, current) -> {
      false
    }

    current.isExtending() || previous == GraphemeBreak.Prepend -> {
      false
    }

    joinsConjunct || joinsEmoji -> {
      false
    }

    previous == GraphemeBreak.RegionalIndicator &&
      current == GraphemeBreak.RegionalIndicator -> {
      regionalIndicatorCount % 2 == 0
    }

    else -> {
      true
    }
  }

private fun continuesHangulSyllable(
  previous: GraphemeBreak,
  current: GraphemeBreak,
): Boolean =
  (previous == GraphemeBreak.L && current.followsLeading()) ||
    (previous.followedByVowel() && current.isVowelOrTrailing()) ||
    (previous.followedByTrailing() && current == GraphemeBreak.T)

private fun GraphemeBreak.isControl(): Boolean =
  this == GraphemeBreak.Control ||
    this == GraphemeBreak.Cr ||
    this == GraphemeBreak.Lf

private fun GraphemeBreak.followsLeading(): Boolean =
  this == GraphemeBreak.L ||
    this == GraphemeBreak.V ||
    this == GraphemeBreak.Lv ||
    this == GraphemeBreak.Lvt

private fun GraphemeBreak.followedByVowel(): Boolean =
  this == GraphemeBreak.Lv || this == GraphemeBreak.V

private fun GraphemeBreak.isVowelOrTrailing(): Boolean =
  this == GraphemeBreak.V || this == GraphemeBreak.T

private fun GraphemeBreak.followedByTrailing(): Boolean =
  this == GraphemeBreak.Lvt || this == GraphemeBreak.T

private fun GraphemeBreak.isExtending(): Boolean =
  this == GraphemeBreak.Extend ||
    this == GraphemeBreak.Zwj ||
    this == GraphemeBreak.SpacingMark

private fun hangulSyllableBreakOf(
  value: Int,
): GraphemeBreak =
  if ((value - HANGUL_SYLLABLES.first) % HANGUL_TRAILING_COUNT == 0) {
    GraphemeBreak.Lv
  } else {
    GraphemeBreak.Lvt
  }

private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF

private val HANGUL_SYLLABLES = 0xAC00..0xD7A3

private const val HANGUL_TRAILING_COUNT = 28

private const val CARRIAGE_RETURN = 0x0D

private const val LINE_FEED = 0x0A

private const val DELETE = 0x7F

private const val ZERO_WIDTH_JOINER = 0x200D
