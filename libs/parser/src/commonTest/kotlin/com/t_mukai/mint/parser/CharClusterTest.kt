package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

@Suppress("unused")
class CharClusterTest :
  FunSpec({
    test("xidStart consumes a surrogate pair") {
      xidStart()
        .parse("\uD842\uDFB7")
        .value
        .shouldBeSome()
    }

    test("xidStart rejects a lone high surrogate") {
      xidStart()
        .parse("\uD842")
        .value
        .shouldBeNone()
    }

    test("xidStart rejects a high surrogate followed by a non-surrogate") {
      xidStart()
        .then(any())
        .parse("\uD842a")
        .value
        .shouldBeNone()
    }

    test("xidStart rejects a lone low surrogate") {
      xidStart()
        .parse("\uDFB7")
        .value
        .shouldBeNone()
    }

    test("digit consumes a surrogate pair") {
      digit()
        .parse("\uD835\uDFD8")
        .value
        .shouldBeSome()
    }

    test("plain char reports nothing") {
      char('"')
        .parse("\"")
        .diagnostics
        .shouldBeEmpty()
    }

    test("decorated char is accepted with onDecorated") {
      char('"')
        .parse("\"\u0301", onDecorated = Decorated)
        .diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), Decorated)]
    }

    test("prepended char is accepted with onDecorated") {
      char('"')
        .parse("\u0600\"", onDecorated = Decorated)
        .diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), Decorated)]
    }

    test("decorated xidContinue is accepted with onDecorated") {
      xidContinue()
        .parse("e\u0301", onDecorated = Decorated)
        .diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), Decorated)]
    }

    test("any consumes a whole cluster") {
      any()
        .parse("e\u0301")
        .value
        .shouldBeSome()
    }

    test("keyword is not a whole word before a supplementary letter") {
      keyword("if")
        .then(many(any()))
        .parse("if\uD842\uDFB7")
        .value
        .shouldBeNone()
    }

    test("keyword accepts a decorated last letter with onDecorated") {
      keyword("if")
        .parse("if\u0301", onDecorated = Decorated)
        .diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 2), Location(1, 3), Decorated)]
    }

    test("padding skips CRLF") {
      char('a')
        .padded()
        .then(char('b'))
        .parse("a\r\nb")
        .value
        .shouldBeSome()
    }

    test("padding does not skip decorated whitespace") {
      char('a')
        .padded()
        .then(char('b'))
        .parse("a \u0301b")
        .value
        .shouldBeNone()
    }

    test("decorated char records nothing without onDecorated") {
      val (value, diagnostics) = char('"').parse("\"\u0301")
      value.shouldBeSome()
      diagnostics.shouldBeEmpty()
    }

    test("newline reads CRLF as one line break") {
      char('a')
        .then(newline())
        .then(char('b'))
        .parse("a\r\nb")
        .value
        .shouldBeSome()
    }

    test("newline rejects other characters") {
      newline()
        .parse("a")
        .value
        .shouldBeNone()
    }
  })
