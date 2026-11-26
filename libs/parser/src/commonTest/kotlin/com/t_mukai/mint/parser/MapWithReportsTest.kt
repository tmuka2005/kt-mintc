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
      mapWithReports { _, reports -> reports }

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
          .mapWithReports { _, _ ->
            error("transform must not be called")
          }.slice()
          .parse("o")
      value.shouldBeSome()
      diagnostics shouldBe
        [Diagnostic.Reported(Location(1, 1), Location(1, 2), "letter o")]
    }
  })
