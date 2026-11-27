package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Diagnostic
import com.t_mukai.mint.parser.Output
import com.t_mukai.mint.parser.ParseResult
import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.any
import com.t_mukai.mint.parser.custom
import com.t_mukai.mint.parser.ignoreThen
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.map
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.parser.then
import com.t_mukai.mint.parser.thenIgnore
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.syntax.Expression.Primitive.Number.FloatDec
import com.t_mukai.mint.syntax.Expression.Primitive.Number.IntDec
import com.t_mukai.mint.syntax.Expression.Primitive.Number.IntHex
import com.t_mukai.mint.syntax.NumberWarning.LowerHexDigit
import com.t_mukai.mint.syntax.NumberWarning.MissingHexDigits
import com.t_mukai.mint.syntax.NumberWarning.NonAsciiChar
import com.t_mukai.mint.syntax.NumberWarning.UpperHexPrefix
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

@Suppress("unused")
class NumberParsersTest :
  FunSpec({
    // === Decimal integers ===
    // | input | type   | consumed |
    // |-------|--------|----------|
    // | "5"   | IntDec | 1        |
    // | "123" | IntDec | 3        |
    // | "0"   | IntDec | 1        |
    // | "00"  | IntDec | 2        |

    test("decimal integer single digit") {
      assertNumber<IntDec>("5", 1)
    }

    test("decimal integer multiple digits") {
      assertNumber<IntDec>("123", 3)
    }

    test("zero alone") {
      assertNumber<IntDec>("0", 1)
    }

    test("leading zeros") {
      assertNumber<IntDec>("00", 2)
    }

    // === Hexadecimal integers ===
    // | input  | type   | consumed | warnings         |
    // |--------|--------|----------|------------------|
    // | "0x1F" | IntHex | 4        |                  |
    // | "0x0"  | IntHex | 3        |                  |
    // | "0X1F" | IntHex | 4        | UpperHexPrefix   |
    // | "0x1f" | IntHex | 4        | LowerHexDigit    |
    // | "0xFf" | IntHex | 4        | LowerHexDigit    |
    // | "0x"   | IntHex | 2        | MissingHexDigits |

    test("hex integer") {
      assertNumber<IntHex>("0x1F", 4)
    }

    test("hex with zero digit") {
      assertNumber<IntHex>("0x0", 3)
    }

    test("hex with uppercase prefix") {
      assertNumber<IntHex>("0X1F", 4, [UpperHexPrefix])
    }

    test("hex with lower digit") {
      assertNumber<IntHex>("0x1f", 4, [LowerHexDigit])
    }

    test("hex mixed case digits") {
      assertNumber<IntHex>("0xFf", 4, [LowerHexDigit])
    }

    test("hex prefix without digits at EOF") {
      assertNumber<IntHex>("0x", 2, [MissingHexDigits])
    }

    // === Floats ===
    // | input  | type     | consumed |
    // |--------|----------|----------|
    // | "3.14" | FloatDec | 4        |
    // | "0.5"  | FloatDec | 3        |

    test("float basic") {
      assertNumber<FloatDec>("3.14", 4)
    }

    test("float with zero integer part") {
      assertNumber<FloatDec>("0.5", 3)
    }

    // === Fallbacks (greedy / or) ===
    // | input    | type     | consumed |
    // |----------|----------|----------|
    // | "3."     | IntDec   | 1        |
    // | "0x1F.5" | IntHex   | 4        |
    // | "12abc"  | IntDec   | 2        |
    // | "0xg"    | IntHex   | 2        |
    // | "3.1.2"  | FloatDec | 3        |

    test("trailing dot falls back to integer") {
      assertNumber<IntDec>("3.", 1)
    }

    test("hex takes priority over decimal") {
      assertNumber<IntHex>("0x1F.5", 4)
    }

    test("stops at trailing garbage") {
      assertNumber<IntDec>("12abc", 2)
    }

    test("hex prefix followed by non-hex is accepted with warning") {
      assertNumber<IntHex>("0xg", 2, [MissingHexDigits])
    }

    test("only one fractional part") {
      assertNumber<FloatDec>("3.1.2", 3)
    }

    // === Failures ===
    // | input  | output |
    // |--------|--------|
    // | ".5"   | Fail   |
    // | "abc"  | Fail   |
    // | ""     | Fail   |
    // | " 123" | Fail   |
    // | "+1"   | Fail   |
    // | "-5"   | Fail   |

    test("leading dot is not float") {
      assertNotNumber(".5")
    }

    test("non number") {
      assertNotNumber("abc")
    }

    test("empty string") {
      assertNotNumber("")
    }

    test("leading whitespace") {
      assertNotNumber(" 123")
    }

    test("plus sign rejected") {
      assertNotNumber("+1")
    }

    test("minus sign rejected") {
      assertNotNumber("-5")
    }

    // === Non-ASCII tolerance ===
    // | input                       | type     | consumed | warnings         |
    // |-----------------------------|----------|----------|------------------|
    // | "123"                       | IntDec   | 3        |                  |
    // | U+FF13                      | IntDec   | 1        | NonAscii         |
    // | U+0669                      | IntDec   | 1        | NonAscii         |
    // | U+1D7D8                     | IntDec   | 2        | NonAscii         |
    // | "3" U+FF0E "14"             | FloatDec | 4        | NonAscii         |
    // | "3" U+3002 "14"             | FloatDec | 4        | NonAscii         |
    // | "3" U+FF61 "14"             | FloatDec | 4        | NonAscii         |
    // | "3." U+FF13                 | FloatDec | 3        | NonAscii         |
    // | U+FF11 ".5"                 | FloatDec | 3        | NonAscii         |
    // | "0x1F"                      | IntHex   | 4        |                  |
    // | U+FF10 "x1F"                | IntHex   | 4        | NonAscii         |
    // | "0" U+FF58 "1F"             | IntHex   | 4        | NonAscii         |
    // | "0" U+FF38 "1F"             | IntHex   | 4        | NonAscii         |
    // | "0x" U+FF26 "F"             | IntHex   | 4        | NonAscii         |
    // | "0x" U+FF46                 | IntHex   | 3        | NonAscii+Lower   |
    // | "0x" U+FF11 "F"             | IntHex   | 4        | NonAscii         |
    // | U+FF10 U+FF58 U+FF26 U+FF26 | IntHex   | 4        | NonAscii         |
    // | U+FF10 "x"                  | IntHex   | 2        | NonAscii+Missing |

    test("ascii decimal has no warnings") {
      assertNumber<IntDec>("123", 3)
    }

    test("fullwidth decimal digit parses with warning") {
      assertNumber<IntDec>("３", 1, [NonAsciiChar])
    }

    test("arabic-indic digit parses with warning") {
      assertNumber<IntDec>("٩", 1, [NonAsciiChar])
    }

    test("supplementary digit parses with warning") {
      assertNumber<IntDec>("𝟘", 2, [NonAsciiChar])
    }

    test("fullwidth period parses as float") {
      assertNumber<FloatDec>("3．14", 4, [NonAsciiChar])
    }

    test("ideographic period parses as float") {
      assertNumber<FloatDec>("3。14", 4, [NonAsciiChar])
    }

    test("halfwidth ideographic period parses as float") {
      assertNumber<FloatDec>("3｡14", 4, [NonAsciiChar])
    }

    test("fullwidth digit in fraction parses with warning") {
      assertNumber<FloatDec>("3.３", 3, [NonAsciiChar])
    }

    test("non-ascii integer part carries over to float") {
      assertNumber<FloatDec>("１.5", 3, [NonAsciiChar])
    }

    test("ascii hex has no warnings") {
      assertNumber<IntHex>("0x1F", 4)
    }

    test("fullwidth hex zero prefix parses with warning") {
      assertNumber<IntHex>("０x1F", 4, [NonAsciiChar])
    }

    test("fullwidth hex letter x parses with warning") {
      assertNumber<IntHex>("0ｘ1F", 4, [NonAsciiChar])
    }

    test("fullwidth capital X is not an uppercase prefix") {
      assertNumber<IntHex>("0Ｘ1F", 4, [NonAsciiChar])
    }

    test("fullwidth hex digit letter parses with warning") {
      assertNumber<IntHex>("0xＦF", 4, [NonAsciiChar])
    }

    test("fullwidth lower hex letter carries both warnings") {
      assertNumber<IntHex>("0xｆ", 3, [NonAsciiChar, LowerHexDigit])
    }

    test("fullwidth digit inside hex parses with warning") {
      assertNumber<IntHex>("0x１F", 4, [NonAsciiChar])
    }

    test("all fullwidth hex parses with warning") {
      assertNumber<IntHex>("０ｘＦＦ", 4, [NonAsciiChar])
    }

    test("fullwidth incomplete hex prefix parses with warnings") {
      assertNumber<IntHex>("０x", 2, [NonAsciiChar, MissingHexDigits])
    }

    // === Non-zero offset ===
    // | input   | offset | type     | consumed |
    // |---------|--------|----------|----------|
    // | "x0x1F" | 1      | IntHex   | 4        |
    // | "x3.14" | 1      | FloatDec | 4        |

    test("parses hex at non-zero offset") {
      assertNumber<IntHex>("x0x1F", 4, offset = 1)
    }

    test("parses float at non-zero offset") {
      assertNumber<FloatDec>("x3.14", 4, offset = 1)
    }

    // === Stack safety ===
    // | input      | type   | consumed |
    // |------------|--------|----------|
    // | "1" * 10^5 | IntDec | 100000   |

    test("large decimal input is stack safe") {
      assertNumber<IntDec>("1".repeat(100_000), 100_000)
    }
  })

private fun probe(
  input: String,
  offset: Int,
): Output<Pair<Number, Int>, NumberWarning> =
  many(any())
    .exactly(offset)
    .ignoreThen(
      Parser.ofNumber.then(custom { ParseResult.success(pos) }),
    ).thenIgnore(many(any()))
    .map { number, end -> number to end - offset }
    .parse(input)

private inline fun <reified T : Number> assertNumber(
  input: String,
  expectedCharLen: Int,
  expectedWarnings: Set<NumberWarning> = [],
  offset: Int = 0,
) {
  val (value, diagnostics) = probe(input, offset)
  val [payload, consumed] = value.shouldBeSome()
  payload.shouldBeInstanceOf<T>()
  consumed shouldBe expectedCharLen
  payload.warnings.toSet() shouldBe expectedWarnings
  diagnostics
    .filterIsInstance<Diagnostic.Reported<NumberWarning>>()
    .map { it.kind }
    .toSet() shouldBe expectedWarnings
}

private fun assertNotNumber(
  input: String,
  offset: Int = 0,
) {
  probe(input, offset).value.shouldBeNone()
}
