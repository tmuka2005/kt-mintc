package com.t_mukai.mint.syntax

import arrow.core.Option
import com.t_mukai.mint.parser.Diagnostic
import com.t_mukai.mint.parser.Expected
import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.syntax.Expression.Assignment
import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

@Suppress("unused")
class ExpressionParsersTest :
  FunSpec({
    test("val expression binds the whole right side") {
      val (text, designation, value) =
        parse("val x = 1 + 2")
          .shouldBeSome()
          .shouldBeInstanceOf<Assignment>()
      text.toString() shouldBe "val x = 1 + 2"
      designation.text
        .toString() shouldBe "x"
      value.shouldBeInstanceOf<Expression.Plus>()
    }

    test("val expression can be the right side") {
      parse("val x = val y = 1")
        .shouldBeSome()
        .shouldBeInstanceOf<Assignment>()
        .value
        .shouldBeInstanceOf<Assignment>()
        .designation
        .text
        .toString() shouldBe "y"
    }

    test("val without a binding is an identifier") {
      parse("val + 1")
        .shouldBeSome()
        .shouldBeInstanceOf<Expression.Plus>()
        .lhs
        .shouldBeInstanceOf<Expression.Identifier>()
        .text
        .toString() shouldBe "val"
    }

    test("val can be bound as a name") {
      parse("val val = 1")
        .shouldBeSome()
        .shouldBeInstanceOf<Assignment>()
        .designation
        .text
        .toString() shouldBe "val"
    }

    test("val is matched as a whole word") {
      parse("valx")
        .shouldBeSome()
        .shouldBeInstanceOf<Expression.Identifier>()
    }

    test("dollar and digits are a numbered result") {
      parse("$12")
        .shouldBeSome()
        .shouldBeInstanceOf<Expression.ReplResult>()
        .text
        .toString() shouldBe "$12"
    }

    test("dollar without digits expects decimal digits") {
      Parser.ofExpression
        .parse("$")
        .diagnostics
        .single()
        .shouldBeInstanceOf<Diagnostic.Unexpected>()
        .expected shouldBe setOf(Expected.Label("decimal digits"))
    }

    test("numbered result is an invalid designation") {
      invalidDesignation("val $1 = 2") shouldBe "$1"
    }

    test("expression on the left side is an invalid designation") {
      val (value, diagnostics) = Parser.ofExpression.parse("val 1 + 2 = 3")
      value
        .shouldBeSome()
        .shouldBeInstanceOf<Assignment>()
        .designation
        .shouldBeInstanceOf<Designation.Invalid>()
      diagnostics shouldBe
        [
          Diagnostic.Reported(
            Location(1, 5),
            Location(1, 10),
            SyntaxError.InvalidDesignation,
          ),
        ]
    }

    test("name followed by more expression is an invalid designation") {
      invalidDesignation("val x + 1 = 2") shouldBe "x + 1"
    }

    test("left side ends at the first equals") {
      invalidDesignation("val val x = y") shouldBe "val x"
    }

    test("unparsable left side is only an invalid designation") {
      val output = Parser.ofExpression.parse("val @ = 1")
      invalidDesignation("val @ = 1") shouldBe "@"
      output.diagnostics shouldBe
        [
          Diagnostic.Reported(
            Location(1, 5),
            Location(1, 6),
            SyntaxError.InvalidDesignation,
          ),
        ]
    }

    test("empty left side is an invalid designation") {
      invalidDesignation("val = 1") shouldBe ""
    }

    test("unparsable right side is an invalid expression") {
      val (value, diagnostics) = Parser.ofExpression.parse("val x = #")
      value
        .shouldBeSome()
        .shouldBeInstanceOf<Assignment>()
        .value
        .shouldBeInstanceOf<Expression.Invalid>()
        .text
        .toString() shouldBe "#"
      diagnostics
        .single()
        .shouldBeInstanceOf<Diagnostic.Unexpected>()
        .start shouldBe Location(1, 9)
    }

    test("val without equals is not a val expression") {
      parse("val - 1")
        .shouldBeSome()
        .shouldBeInstanceOf<Expression.Minus>()
    }
  })

private fun invalidDesignation(
  input: String,
): String =
  parse(input)
    .shouldBeSome()
    .shouldBeInstanceOf<Assignment>()
    .designation
    .shouldBeInstanceOf<Designation.Invalid>()
    .text
    .toString()

private fun parse(
  input: String,
): Option<Expression> =
  Parser.ofExpression
    .parse(input)
    .value
