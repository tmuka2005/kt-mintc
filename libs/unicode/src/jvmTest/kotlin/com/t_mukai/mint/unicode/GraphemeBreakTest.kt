package com.t_mukai.mint.unicode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

@Suppress("unused")
class GraphemeBreakTest :
  FunSpec({
    test("graphemeBreakOf matches GraphemeBreakProperty") {
      val properties = loadProperties("GraphemeBreakProperty.txt")
      for ((key, value) in graphemeBreakNames) {
        mismatches(properties.getValue(key)) {
          graphemeBreakOf(it) == value
        }.shouldBeEmpty()
      }
    }

    test("isExtendedPictographic matches Extended_Pictographic") {
      val properties = loadProperties("emoji-data.txt")
      mismatches(properties.getValue("Extended_Pictographic")) {
        isExtendedPictographic(it)
      }.shouldBeEmpty()
    }

    test("CharClusters matches GraphemeBreakTest") {
      ucdLines("GraphemeBreakTest.txt")
        .filterNot { line -> conformsTo(line) }
        .shouldBeEmpty()
    }
  })

private val graphemeBreakNames =
  mapOf(
    "CR" to GraphemeBreak.Cr,
    "LF" to GraphemeBreak.Lf,
    "Control" to GraphemeBreak.Control,
    "Extend" to GraphemeBreak.Extend,
    "ZWJ" to GraphemeBreak.Zwj,
    "Regional_Indicator" to GraphemeBreak.RegionalIndicator,
    "Prepend" to GraphemeBreak.Prepend,
    "SpacingMark" to GraphemeBreak.SpacingMark,
    "L" to GraphemeBreak.L,
    "V" to GraphemeBreak.V,
    "T" to GraphemeBreak.T,
    "LV" to GraphemeBreak.Lv,
    "LVT" to GraphemeBreak.Lvt,
  )

private fun conformsTo(
  line: String,
): Boolean {
  val text = StringBuilder()
  val expected = mutableListOf<Boolean>()
  for (token in line.split(' ')) {
    when (token) {
      "÷" -> expected += true
      "×" -> expected += false
      else -> text.appendCodePoint(token.toInt(16))
    }
  }
  val input = text.toString()
  val clusters = CharClusters(input)
  val actual =
    (0..input.length)
      .filterNot { it in 1 until input.length && input[it].isLowSurrogate() }
      .map { clusters.isBoundary(it) }
  return actual == expected
}
