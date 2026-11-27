package com.t_mukai.mint.syntax

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.t_mukai.mint.parser.Slice
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.unicode.CharClusters
import com.t_mukai.mint.unicode.digitValue
import kotlin.text.unicode.codePointAt

public sealed value class IntLiteralParseError {
  public value object Overflow : IntLiteralParseError()

  public value object MissingDigits : IntLiteralParseError()
}

public sealed value class FloatLiteralParseError {
  public value object NonFinite : FloatLiteralParseError()
}

public fun Number.IntDec.toLong(
  negated: Boolean,
): Either<IntLiteralParseError, Long> =
  decimalLong(text, negated)

public fun Number.IntHex.toLong(): Either<IntLiteralParseError, Long> =
  hexLong(text)

public fun Number.FloatDec.toDouble(): Either<FloatLiteralParseError, Double> =
  decimalDouble(text)

// region internal

private fun decimalLong(
  text: Slice,
  negated: Boolean,
): Either<IntLiteralParseError, Long> {
  val limit =
    if (negated) {
      Long.MIN_VALUE.toULong()
    } else {
      Long.MAX_VALUE.toULong()
    }
  val magnitude =
    text.baseDigits(DECIMAL).digitsValue(DECIMAL, limit, 0)
      ?: return IntLiteralParseError.Overflow.left()
  return if (negated) {
    (0uL - magnitude).toLong().right()
  } else {
    magnitude.toLong().right()
  }
}

private fun hexLong(
  text: Slice,
): Either<IntLiteralParseError, Long> {
  val digits = text.baseDigits(HEX)
  if (digits.size <= HEX_PREFIX_LENGTH) {
    return IntLiteralParseError.MissingDigits.left()
  }
  return digits
    .digitsValue(HEX, ULong.MAX_VALUE, HEX_PREFIX_LENGTH)
    ?.toLong()
    ?.right()
    ?: IntLiteralParseError.Overflow.left()
}

private fun decimalDouble(
  text: Slice,
): Either<FloatLiteralParseError, Double> {
  val ascii = StringBuilder(text.length)
  for (digit in text.baseDigits(DECIMAL)) {
    ascii.append(
      if (digit < 0) {
        '.'
      } else {
        '0' + digit
      },
    )
  }
  val value = ascii.toString().toDouble()
  return if (value.isFinite()) {
    value.right()
  } else {
    FloatLiteralParseError.NonFinite.left()
  }
}

private fun Slice.baseDigits(
  radix: Int,
): IntArray {
  val clusters = CharClusters(this)
  val digits = IntArray(length)
  var count = 0
  var start = 0
  while (start < length) {
    digits[count++] =
      codePointAt(clusters.baseIndexAt(start)).digitValue(radix)
    start = clusters.endOf(start)
  }
  return digits.copyOf(count)
}

private fun IntArray.digitsValue(
  radix: Int,
  limit: ULong,
  from: Int,
): ULong? {
  val base = radix.toULong()
  var value = 0uL
  for (index in from until size) {
    val digit = this[index].toULong()
    if (value > (limit - digit) / base) {
      return null
    }
    value = value * base + digit
  }
  return value
}

private const val DECIMAL = 10

private const val HEX = 16

private const val HEX_PREFIX_LENGTH = 2

// endregion
