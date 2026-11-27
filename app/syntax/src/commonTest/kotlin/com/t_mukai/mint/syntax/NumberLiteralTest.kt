package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.syntax.FloatLiteralParseError.NonFinite
import com.t_mukai.mint.syntax.IntLiteralParseError.MissingDigits
import com.t_mukai.mint.syntax.IntLiteralParseError.Overflow
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf

@Suppress("unused")
class NumberLiteralTest :
  FunSpec({
    test("decimal integer is read") {
      intDec("123").toLong(negated = false) shouldBeRight 123L
    }

    test("fullwidth and other decimal digits are read") {
      intDec("１２３").toLong(negated = false) shouldBeRight 123L
      intDec("٣").toLong(negated = false) shouldBeRight 3L
    }

    test("decorations are skipped") {
      intDec("1\u03012").toLong(negated = false) shouldBeRight 12L
    }

    test("decimal integer up to Long.MAX_VALUE is read") {
      intDec("9223372036854775807").toLong(negated = false) shouldBeRight
        Long.MAX_VALUE
    }

    test("decimal integer over Long.MAX_VALUE overflows") {
      intDec("9223372036854775808").toLong(negated = false) shouldBeLeft
        Overflow
    }

    test("negated decimal integer reaches Long.MIN_VALUE") {
      intDec("9223372036854775808").toLong(negated = true) shouldBeRight
        Long.MIN_VALUE
    }

    test("negated decimal integer under Long.MIN_VALUE overflows") {
      intDec("9223372036854775809").toLong(negated = true) shouldBeLeft
        Overflow
    }

    test("hex integer is read") {
      intHex("0xFF").toLong() shouldBeRight 255L
      intHex("０ｘｆｆ").toLong() shouldBeRight 255L
    }

    test("hex integer fitting in 64 bits is read as two's complement") {
      intHex("0xFFFFFFFFFFFFFFFF").toLong() shouldBeRight -1L
    }

    test("leading zeros of hex integer do not overflow") {
      intHex("0x00000000000000001").toLong() shouldBeRight 1L
    }

    test("hex integer over 64 bits overflows") {
      intHex("0x10000000000000000").toLong() shouldBeLeft Overflow
    }

    test("hex integer without digits misses digits") {
      intHex("0x").toLong() shouldBeLeft MissingDigits
    }

    test("float is read") {
      floatDec("1.5").toDouble() shouldBeRight 1.5
      floatDec("１。５").toDouble() shouldBeRight 1.5
    }

    test("float is rounded to the nearest double") {
      floatDec("0.1").toDouble() shouldBeRight 0.1
    }

    test("float may be rounded to zero") {
      floatDec("0.${"0".repeat(400)}1").toDouble() shouldBeRight 0.0
    }

    test("float too large is not finite") {
      floatDec("1${"0".repeat(400)}.0").toDouble() shouldBeLeft NonFinite
    }
  })

private fun number(
  input: String,
): Number =
  Parser.ofNumber
    .parse(input)
    .value
    .shouldBeSome()

private fun intDec(
  input: String,
): Number.IntDec =
  number(input).shouldBeInstanceOf<Number.IntDec>()

private fun intHex(
  input: String,
): Number.IntHex =
  number(input).shouldBeInstanceOf<Number.IntHex>()

private fun floatDec(
  input: String,
): Number.FloatDec =
  number(input).shouldBeInstanceOf<Number.FloatDec>()
