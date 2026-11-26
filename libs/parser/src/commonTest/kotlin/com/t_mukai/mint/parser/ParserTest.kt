package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class ParserTest :
  FunSpec({
    val digit = char('0'..'9')

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

    test("empty succeeds without consuming") {
      empty()
        .then(char('a'))
        .slice()
        .parse("a")
        .value
        .shouldBeSome()
        .toString() shouldBe "a"
    }

    test("just matches the literal regardless of what follows") {
      just("let")
        .then(char('t'))
        .parse("lett")
        .value
        .shouldBeSome()
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

    test("trailing input reports end of input") {
      digit
        .parse("12")
        .errorSummary() shouldBe
        [Location(1, 2) to setOf(Expected.EndOfInput)]
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
