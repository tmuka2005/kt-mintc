package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

@Suppress("unused")
class MapWithReportsTest :
  FunSpec({
    val zero = char('0').or(char('o'), warn = "letter o")

    fun <T> Parser<T, String>.reports(): Parser<List<String>, String> =
      mapWithReports(null) { _, reports -> reports }

    fun <T> Parser<T, Any>.decorations(
      onDecorated: Any?,
    ): Parser<List<Any>, Any> =
      mapWithReports(onDecorated) { _, reports -> reports }

    test("no reports give an empty list") {
      many(zero)
        .reports()
        .parse("00")
        .value
        .shouldBeSome()
        .shouldBeEmpty()
    }

    test("adjacent reports of the same kind are merged") {
      many(zero)
        .reports()
        .parse("oo0o")
        .value shouldBeSome ["letter o", "letter o"]
    }

    test("reports outside are not collected") {
      val (value, diagnostics) =
        zero
          .then(zero.reports())
          .map { _, reports -> reports }
          .parse("o0")
      value
        .shouldBeSome()
        .shouldBeEmpty()
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o")]
    }

    test("reports stay in the diagnostics") {
      zero
        .reports()
        .parse("o")
        .diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o")]
    }

    test("failure of the inner parser fails") {
      zero
        .then(char('!'))
        .reports()
        .parse("o")
        .value
        .shouldBeNone()
    }

    test("check mode does not call transform") {
      val (value, diagnostics) =
        zero
          .mapWithReports(null) { _, _ ->
            error("transform must not be called")
          }.slice()
          .parse("o")
      value.shouldBeSome()
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o")]
    }

    test("onDecorated overrides onDecorated of parse") {
      val (value, diagnostics) =
        char('"')
          .decorations(Override)
          .parse("\"\u0301", onDecorated = Decorated)
      value shouldBeSome [Override]
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), Override)]
    }

    test("null onDecorated reports no decoration") {
      val (value, diagnostics) =
        char('"')
          .decorations(null)
          .parse("\"\u0301", onDecorated = Decorated)
      value
        .shouldBeSome()
        .shouldBeEmpty()
      diagnostics.shouldBeEmpty()
    }

    test("innermost onDecorated wins") {
      char('"')
        .decorations(Override)
        .decorations(Decorated)
        .parse("\"\u0301")
        .value shouldBeSome [Override]
    }

    test("onDecorated applies only inside its parser") {
      char('"')
        .decorations(Override)
        .then(char('"'))
        .parse("\"\u0301\"\u0301", onDecorated = Decorated)
        .diagnostics shouldBe
        [
          Diagnostic.Reported(Location(1, 1), Location(1, 2), Override),
          Diagnostic.Reported(Location(1, 2), Location(1, 3), Decorated),
        ]
    }
  })

private data object Override
