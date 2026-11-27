package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Diagnostic
import com.t_mukai.mint.parser.Expected
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
import com.t_mukai.mint.syntax.Expression.Identifier
import com.t_mukai.mint.syntax.IdentifierWarning.DecoratedChar
import com.t_mukai.mint.syntax.IdentifierWarning.InvisibleLetter
import com.t_mukai.mint.syntax.IdentifierWarning.NonAsciiChar
import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class IdentifierParsersTest :
  FunSpec({
    // === ASCII identifiers ===
    // | input  | consumed |
    // |--------|----------|
    // | "a"    | 1        |
    // | "Abc"  | 3        |
    // | "_"    | 1        |
    // | "_x1"  | 3        |
    // | "a1b2" | 4        |

    test("single lowercase letter") {
      assertIdentifier("a", 1)
    }

    test("mixed case letters") {
      assertIdentifier("Abc", 3)
    }

    test("underscore alone") {
      assertIdentifier("_", 1)
    }

    test("leading underscore with digit") {
      assertIdentifier("_x1", 3)
    }

    test("digits after first letter") {
      assertIdentifier("a1b2", 4)
    }

    // === Stops ===
    // | input     | consumed |
    // |-----------|----------|
    // | "abc def" | 3        |
    // | "a+b"     | 1        |
    // | "a-b"     | 1        |

    test("stops at whitespace") {
      assertIdentifier("abc def", 3)
    }

    test("stops at plus") {
      assertIdentifier("a+b", 1)
    }

    test("stops at minus") {
      assertIdentifier("a-b", 1)
    }

    // === Failures ===
    // | input | output |
    // |-------|--------|
    // | "1a"  | Fail   |
    // | ""    | Fail   |
    // | " a"  | Fail   |
    // | "+a"  | Fail   |

    test("leading digit rejected") {
      assertNotIdentifier("1a")
    }

    test("empty string") {
      assertNotIdentifier("")
    }

    test("leading whitespace") {
      assertNotIdentifier(" a")
    }

    test("leading symbol rejected") {
      assertNotIdentifier("+a")
    }

    // === Non-ASCII tolerance ===
    // | input         | consumed | warnings |
    // |---------------|----------|----------|
    // | U+3042        | 1        | NonAscii |
    // | U+5909 U+6570 | 2        | NonAscii |
    // | U+00E9        | 1        | NonAscii |
    // | "a" U+3042 "b"| 3        | NonAscii |
    // | "a" U+FF11    | 2        | NonAscii |
    // | "x" U+0669    | 2        | NonAscii |
    // | U+FF11 "a"    | Fail     |          |
    // | U+FF21        | 1        | NonAscii |
    // | U+20BB7       | 2        | NonAscii |
    // | "a" U+1D465   | 3        | NonAscii |
    // | "cafe" U+0301 | 5        | Decorated |
    // | U+0301 "a"    | Fail     |          |
    // | "a" U+FE0F    | 2        | Decorated |
    // | U+845B U+E0100| 3        | NonAscii+Decorated |
    // | U+FE0F "a"    | Fail     |          |

    test("hiragana parses with warning") {
      assertIdentifier("あ", 1, [NonAsciiChar])
    }

    test("kanji parses with warning") {
      assertIdentifier("変数", 2, [NonAsciiChar])
    }

    test("latin letter with accent parses with warning") {
      assertIdentifier("é", 1, [NonAsciiChar])
    }

    test("non-ascii letter in the middle parses with warning") {
      assertIdentifier("aあb", 3, [NonAsciiChar])
    }

    test("fullwidth digit after letter parses with warning") {
      assertIdentifier("a１", 2, [NonAsciiChar])
    }

    test("arabic-indic digit after letter parses with warning") {
      assertIdentifier("x٩", 2, [NonAsciiChar])
    }

    test("leading fullwidth digit rejected") {
      assertNotIdentifier("１a")
    }

    test("fullwidth latin letter parses with warning") {
      assertIdentifier("Ａ", 1, [NonAsciiChar])
    }

    test("supplementary letter parses with warning") {
      assertIdentifier("𠮷", 2, [NonAsciiChar])
    }

    test("supplementary letter after ascii parses with warning") {
      assertIdentifier("a𝑥", 3, [NonAsciiChar])
    }

    test("combining mark after letter parses with warning") {
      assertIdentifier("cafe\u0301", 5, [DecoratedChar])
    }

    test("leading combining mark rejected") {
      assertNotIdentifier("\u0301a")
    }

    test("variation selector after letter parses with warning") {
      assertIdentifier("a\uFE0F", 2, [DecoratedChar])
    }

    test("supplementary variation selector carries both warnings") {
      assertIdentifier("葛\uDB40\uDD00", 3, [NonAsciiChar, DecoratedChar])
    }

    test("leading variation selector rejected") {
      assertNotIdentifier("\uFE0Fa")
    }

    test("letter number starts identifier") {
      assertIdentifier("\u3007\u3007\u8AB2", 3, [NonAsciiChar])
      assertIdentifier("\u2160", 1, [NonAsciiChar])
    }

    test("letter outside XID_Start rejected at head") {
      assertNotIdentifier("\uFF9Ea")
      assertNotIdentifier("\u037Aa")
    }

    test("character outside XID_Continue stops identifier") {
      assertIdentifier("a\uFDFA", 1)
      assertIdentifier("a\u200B", 1)
    }

    test("connector punctuation and middle dots continue identifier") {
      assertIdentifier("a\uFF3Fb", 3, [NonAsciiChar])
      assertIdentifier("a\u203Fb", 3, [NonAsciiChar])
      assertIdentifier("a\u00B7b", 3, [NonAsciiChar])
      assertIdentifier("a\u30FBb", 3, [NonAsciiChar])
    }

    test("join control after letter parses with warning") {
      assertIdentifier("a\u200Cb", 3, [DecoratedChar])
      assertIdentifier("a\u200Db", 3, [DecoratedChar])
    }

    test("leading join control rejected") {
      assertNotIdentifier("\u200Da")
    }

    test("invisible letter carries both warnings") {
      assertIdentifier("\u3164", 1, [NonAsciiChar, InvisibleLetter])
      assertIdentifier("a\uFFA0", 2, [NonAsciiChar, InvisibleLetter])
    }

    test("invisible mark decorates the previous letter") {
      assertIdentifier("a\u034F", 2, [DecoratedChar])
    }

    // === Label ===

    test("failure is reported as identifier") {
      Parser.ofIdentifier
        .parse("1")
        .diagnostics shouldBe
        [
          Diagnostic.Unexpected(
            Location(1, 1),
            Location(1, 1),
            setOf(Expected.Label("identifier")),
            "1",
          ),
        ]
    }

    // === Non-zero offset ===
    // | input  | offset | consumed |
    // |--------|--------|----------|
    // | "+abc" | 1      | 3        |

    test("parses identifier at non-zero offset") {
      assertIdentifier("+abc", 3, offset = 1)
    }

    // === Stack safety ===
    // | input      | consumed |
    // |------------|----------|
    // | "a" * 10^5 | 100000   |

    test("large identifier input is stack safe") {
      assertIdentifier("a".repeat(100_000), 100_000)
    }
  })

private fun probe(
  input: String,
  offset: Int,
): Output<Pair<Identifier, Int>, IdentifierWarning> =
  many(any())
    .exactly(offset)
    .ignoreThen(
      Parser.ofIdentifier.then(custom { ParseResult.success(pos) }),
    ).thenIgnore(many(any()))
    .map { identifier, end -> identifier to end - offset }
    .parse(input)

private fun assertIdentifier(
  input: String,
  expectedCharLen: Int,
  expectedWarnings: Set<IdentifierWarning> = [],
  offset: Int = 0,
) {
  val (value, diagnostics) = probe(input, offset)
  val [payload, consumed] = value.shouldBeSome()
  payload.text.toString() shouldBe
    input.substring(offset, offset + expectedCharLen)
  consumed shouldBe expectedCharLen
  payload.warnings.toSet() shouldBe expectedWarnings
  diagnostics
    .filterIsInstance<Diagnostic.Reported<IdentifierWarning>>()
    .map { it.kind }
    .toSet() shouldBe expectedWarnings
}

private fun assertNotIdentifier(
  input: String,
  offset: Int = 0,
) {
  probe(input, offset).value.shouldBeNone()
}
