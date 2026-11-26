package com.t_mukai.mint.unicode

import java.io.File
import kotlin.text.unicode.CodePoint

internal fun ucdLines(
  fileName: String,
): List<String> =
  File(System.getProperty("ucd.dir"), fileName)
    .readLines()
    .map { it.substringBefore('#').trim() }
    .filter { it.isNotEmpty() }

internal fun loadProperties(
  fileName: String,
): Map<String, BooleanArray> =
  propertiesOf(ucdLines(fileName))

internal fun mismatches(
  members: BooleanArray,
  predicate: (CodePoint) -> Boolean,
): List<String> =
  (0 until CODE_POINT_LIMIT)
    .filter { predicate(CodePoint(it)) != members[it] }
    .map { hex(it) }

internal const val CODE_POINT_LIMIT = 0x110000

// region internal

private fun propertiesOf(
  lines: List<String>,
): Map<String, BooleanArray> {
  val properties = mutableMapOf<String, BooleanArray>()
  for (line in lines) {
    val fields = line.split(';').map { it.trim() }
    val bounds = fields[0].split("..").map { it.toInt(16) }
    val members =
      properties.getOrPut(fields.drop(1).joinToString("=")) {
        BooleanArray(CODE_POINT_LIMIT)
      }
    for (value in bounds.first()..bounds.last()) {
      members[value] = true
    }
  }
  return properties
}

private fun hex(
  value: Int,
): String =
  "U+" +
    value
      .toString(16)
      .uppercase()
      .padStart(4, '0')

// endregion
