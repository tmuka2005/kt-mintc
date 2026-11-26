package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

@Suppress("unused")
class DiagnosticTest :
  FunSpec({
    val zero =
      char('0').or(char('o'), warn = "letter o")

    test("strict alternative reports nothing") {
      zero
        .parse("0")
        .diagnostics
        .shouldBeEmpty()
    }

    test("tolerated alternative reports its span") {
      val (value, diagnostics) =
        just("ab")
          .then(zero)
          .parse("abo")
      value.shouldBeSome()
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 3), Location(1, 4), "letter o")]
    }

    test("adjacent reports of the same kind are merged") {
      val loud = char('O').reporting("upper").reporting("letter")
      val (value, diagnostics) = many(choice(zero, loud)).parse("oOO0o")
      value.shouldBeSome()
      diagnostics shouldBe
        [
          Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o"),
          Diagnostic.Reported(Location(1, 2), Location(1, 4), "upper"),
          Diagnostic.Reported(Location(1, 2), Location(1, 4), "letter"),
          Diagnostic.Reported(Location(1, 5), Location(1, 6), "letter o"),
        ]
    }

    test("zero-width reports are never merged") {
      val missing = end().reporting("missing")
      val (value, diagnostics) = missing.then(missing).parse("")
      value.shouldBeSome()
      diagnostics shouldBe
        [
          Diagnostic.Reported(Location(1, 1), Location(1, 1), "missing"),
          Diagnostic.Reported(Location(1, 1), Location(1, 1), "missing"),
        ]
    }

    test("backtracked branch discards its diagnostics") {
      val (value, diagnostics) =
        choice(
          zero.then(char('!')).map { _, _ -> "bang" },
          zero.map { "plain" },
        ).parse("o")
      value.shouldBeSome("plain")
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o")]
    }

    test("many discards diagnostics of the failed last item") {
      val (value, diagnostics) =
        many(zero.then(char(';')))
          .thenIgnore(char('o'))
          .parse("o;o")
      value.shouldBeSome()
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o")]
    }

    test("diagnostics are located by row and display column") {
      val (value, diagnostics) =
        char('\u3042')
          .then(newline())
          .then(char('\u3042'))
          .then(zero)
          .parse("\u3042\n\u3042o")
      value.shouldBeSome()
      diagnostics shouldBe
        [Diagnostic.Reported(Location(2, 3), Location(2, 4), "letter o")]
    }
  })
