package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class ParserTest :
  FunSpec({
    val digit = char('0'..'9').labeled("digit")
    val letter = choice(char('a'..'z'), char('A'..'Z'), char('_'))
    val ident =
      letter
        .then(many(choice(letter, char('0'..'9'))))
        .slice()
        .map { it.toString() }
        .labeled("identifier")

    fun <T> Output<T, *>.errorSummary() =
      diagnostics
        .filterIsInstance<Diagnostic.Unexpected>()
        .map { (start, expected) -> start to expected }

    test("keyword matches only a whole word") {
      keyword("let")
        .parse("let")
        .value
        .shouldBeSome(Unit)
      keyword("let")
        .then(any())
        .parse("let_")
        .value
        .shouldBeNone()
      keyword("let")
        .then(char('('))
        .parse("let(")
        .value
        .shouldBeSome()
    }

    test("andIs requires both parsers to match at the same position") {
      val lowerHex =
        char('a'..'z').andIs(char('a'..'f'))
      lowerHex
        .then(any())
        .slice()
        .parse("cz")
        .value
        .shouldBeSome()
        .toString() shouldBe "cz"
      lowerHex
        .parse("g")
        .value
        .shouldBeNone()
    }

    test("andIs reports the expected set of the inner parser") {
      char('a'..'z')
        .andIs(char('a'..'f'))
        .parse("g")
        .errorSummary() shouldBe
        [Location(1, 1) to setOf(Expected.Range('a'..'z'))]
    }

    test("not succeeds without consuming when the inner parser fails") {
      many(any().andIs(char('=').not()))
        .slice()
        .thenIgnore(char('='))
        .parse("ab=")
        .value
        .shouldBeSome()
        .toString() shouldBe "ab"
      char('=')
        .not()
        .parse("=")
        .value
        .shouldBeNone()
    }

    test("not reports nothing from the inner parser") {
      char('=')
        .reporting("equals")
        .not()
        .then(any())
        .parse("x")
        .diagnostics shouldBe emptyList()
    }

    test("empty succeeds without consuming") {
      empty()
        .then(char('a'))
        .slice()
        .parse("a")
        .value
        .shouldBeSome()
        .toString() shouldBe "a"
    }

    test("withSlice keeps both the value and the consumed text") {
      digit
        .slice()
        .map { it.toString().toInt() }
        .padded()
        .withSlice()
        .map { value, text -> value to text.toString() }
        .parse("1 ")
        .value
        .shouldBeSome(1 to "1 ")
    }

    test("just matches the literal regardless of what follows") {
      just("let")
        .then(char('t'))
        .parse("lett")
        .value
        .shouldBeSome()
    }

    test("then and map unpack by lambda arity") {
      val pair =
        ident
          .thenIgnore(char('='))
          .then(digit.slice())
          .then(char(';').slice())
          .map { name, value, end -> "$name:$value$end" }
      pair
        .parse("x1=5;")
        .value
        .shouldBeSome("x1:5;")
    }

    test("map unpacks up to nine values") {
      val one = digit.slice().map { it.toString() }
      val nine =
        one
          .then(one)
          .then(one)
          .then(one)
          .then(one)
          .then(one)
          .then(one)
          .then(one)
          .then(one)
          .map { a, b, c, d, e, f, g, h, i ->
            a + b + c + d + e + f + g + h + i
          }
      nine
        .parse("123456789")
        .value
        .shouldBeSome("123456789")
    }

    test("many respects atLeast and exactly") {
      val digits =
        many(digit)
          .atLeast(2)
          .slice()
          .map { it.toString() }
      digits
        .parse("123")
        .value
        .shouldBeSome("123")
      digits
        .parse("1")
        .value
        .shouldBeNone()
      many(digit)
        .exactly(2)
        .map { it.size }
        .parse("12")
        .value
        .shouldBeSome(2)
    }

    test("choice tries overlapping candidates in order") {
      val parser =
        choice(
          keyword("let").map { "let" },
          ident,
        )
      parser
        .parse("let")
        .value
        .shouldBeSome("let")
      parser
        .parse("letter")
        .value
        .shouldBeSome("letter")
    }

    test("choice reports the expectations of skipped candidates") {
      val p = choice(ident, digit.slice().map { it.toString() })
      p.parse("+").errorSummary() shouldBe
        [
          Location(1, 1) to
            setOf(Expected.Label("identifier"), Expected.Label("digit")),
        ]
    }

    test("choice handles non-ASCII candidates") {
      val parser =
        choice(
          char('あ').map { 1 },
          char('い').map { 2 },
        )
      parser
        .parse("い")
        .value
        .shouldBeSome(2)
      parser
        .parse("う")
        .errorSummary() shouldBe
        [
          Location(1, 1) to
            setOf(
              Expected.Char('あ'),
              Expected.Char('い'),
            ),
        ]
    }

    test("the furthest failure wins") {
      val parser =
        choice(
          just("ab").then(char('c')).map { 1 },
          just("a").map { 2 },
        )
      parser
        .parse("abx")
        .errorSummary() shouldBe
        [Location(1, 3) to setOf(Expected.Char('c'))]
    }

    test("labeled replaces expectations inside it") {
      val call =
        char('f')
          .thenIgnore(char('('))
          .labeled("call")
      call
        .parse("1")
        .errorSummary() shouldBe
        [Location(1, 1) to setOf(Expected.Label("call"))]

      call
        .parse("f")
        .errorSummary() shouldBe
        [Location(1, 1) to setOf(Expected.Label("call"))]
    }

    test("trailing input reports end of input") {
      digit
        .parse("12")
        .errorSummary() shouldBe
        [Location(1, 2) to setOf(Expected.EndOfInput)]
    }

    test("recursive parsers can refer to themselves") {
      val nested =
        recursive { self ->
          choice(
            char('(')
              .ignoreThen(self)
              .thenIgnore(char(')'))
              .map { it + 1 },
            char('x').map { 0 },
          )
        }
      nested
        .parse("((x))")
        .value
        .shouldBeSome(2)
      nested.parse("((x)").errorSummary() shouldBe
        [Location(1, 5) to setOf(Expected.Char(')'))]
    }

    test("custom parsers can drive the context") {
      val upToX =
        custom { _ ->
          while (peek() != null && peek()?.toString() != "x") {
            advance()
          }
          if (peek() == null) {
            error(Expected.Char('x'))
            ParseResult.failure
          } else {
            ParseResult.unit
          }
        }
      upToX
        .slice()
        .map { it.toString() }
        .thenIgnore(char('x'))
        .parse("abcx")
        .value
        .shouldBeSome("abc")
      upToX
        .parse("abc")
        .errorSummary() shouldBe
        [Location(1, 4) to setOf(Expected.Char('x'))]
    }
  })
