package com.t_mukai.mint.evaluation

import arrow.core.Either
import com.t_mukai.mint.evaluation.EvaluationError.DivisionByZero
import com.t_mukai.mint.evaluation.EvaluationError.IntLiteral
import com.t_mukai.mint.evaluation.EvaluationError.IntOverflow
import com.t_mukai.mint.evaluation.EvaluationError.MixedNumberTypes
import com.t_mukai.mint.evaluation.EvaluationError.NonFiniteFloat
import com.t_mukai.mint.evaluation.EvaluationError.NotANumber
import com.t_mukai.mint.evaluation.EvaluationError.RejectedIdentifierWarnings
import com.t_mukai.mint.evaluation.EvaluationError.RejectedNumberWarnings
import com.t_mukai.mint.evaluation.EvaluationError.UndefinedVariable
import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.Slice
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.syntax.Designation
import com.t_mukai.mint.syntax.Expression
import com.t_mukai.mint.syntax.IdentifierWarning
import com.t_mukai.mint.syntax.IntLiteralParseError
import com.t_mukai.mint.syntax.NumberWarning
import com.t_mukai.mint.syntax.ofExpression
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

@Suppress("unused")
class EvaluateTest :
  FunSpec({
    test("int arithmetic follows precedence") {
      evaluate("1 + 2 * 3 - 4") shouldBeRight Value.Int(3)
    }

    test("int division truncates") {
      evaluate("7 / 2") shouldBeRight Value.Int(3)
    }

    test("float arithmetic is evaluated") {
      evaluate("1.5 * 2.0 - 0.5") shouldBeRight Value.Float(2.5)
    }

    test("negation is evaluated") {
      evaluate("- -3") shouldBeRight Value.Int(3)
      evaluate("-1.5") shouldBeRight Value.Float(-1.5)
    }

    test("mixing int and float is an error") {
      evaluate("1 + 2.0")
        .shouldBeLeft()
        .shouldBeInstanceOf<MixedNumberTypes>()
    }

    test("negated literal reaches Long.MIN_VALUE") {
      evaluate("-9223372036854775808") shouldBeRight Value.Int(Long.MIN_VALUE)
    }

    test("int overflow is an error") {
      evaluate("9223372036854775807 + 1")
        .shouldBeLeft()
        .shouldBeInstanceOf<IntOverflow>()
    }

    test("negating Long.MIN_VALUE overflows") {
      evaluate("- -9223372036854775808")
        .shouldBeLeft()
        .shouldBeInstanceOf<IntOverflow>()
      evaluate("-0x8000000000000000")
        .shouldBeLeft()
        .shouldBeInstanceOf<IntOverflow>()
    }

    test("Long.MIN_VALUE divided by -1 overflows") {
      evaluate("-9223372036854775808 / -1")
        .shouldBeLeft()
        .shouldBeInstanceOf<IntOverflow>()
    }

    test("int division by zero is an error") {
      evaluate("1 / 0")
        .shouldBeLeft()
        .shouldBeInstanceOf<DivisionByZero>()
    }

    test("non-finite float is an error") {
      evaluate("1.0 / 0.0")
        .shouldBeLeft()
        .shouldBeInstanceOf<NonFiniteFloat>()
      evaluate("0.0 / 0.0")
        .shouldBeLeft()
        .shouldBeInstanceOf<NonFiniteFloat>()
    }

    test("literal error is an error") {
      evaluate("99999999999999999999")
        .shouldBeLeft()
        .shouldBeInstanceOf<IntLiteral>()
        .error shouldBe IntLiteralParseError.Overflow
      evaluate("0x")
        .shouldBeLeft()
        .shouldBeInstanceOf<IntLiteral>()
        .error shouldBe IntLiteralParseError.MissingDigits
    }

    test("identifier is undefined") {
      evaluate("x")
        .shouldBeLeft()
        .shouldBeInstanceOf<UndefinedVariable>()
    }

    test("number warning rejected by the policy is an error") {
      evaluate(
        "0xff",
        numberPolicy = { it == NumberWarning.LowerHexDigit },
      ).shouldBeLeft()
        .shouldBeInstanceOf<RejectedNumberWarnings>()
        .warnings shouldBe [NumberWarning.LowerHexDigit]
    }

    test("identifier warning rejected by the policy is an error") {
      evaluate("ｘ", identifierPolicy = { true })
        .shouldBeLeft()
        .shouldBeInstanceOf<RejectedIdentifierWarnings>()
        .warnings shouldBe [IdentifierWarning.NonAsciiChar]
    }

    test("val expression evaluates to Unit") {
      evaluate("val x = 1 + 2") shouldBeRight Value.Unit
    }

    test("val expression evaluates its right side") {
      evaluate("val x = 1 / 0")
        .shouldBeLeft()
        .shouldBeInstanceOf<DivisionByZero>()
    }

    test("bound variable is referenced") {
      val environment = Environment()
      evaluate("val x = 1 + 2", environment)
      evaluate("x * 2", environment) shouldBeRight Value.Int(6)
    }

    test("binding the same name shadows the previous one") {
      val environment = Environment()
      evaluate("val x = 1", environment)
      evaluate("val x = 2.0", environment)
      evaluate("x", environment) shouldBeRight Value.Float(2.0)
    }

    test("nested val expressions bind both names") {
      val environment = Environment()
      evaluate("val x = val y = 1", environment)
      environment["x"] shouldBe Value.Unit
      environment["y"] shouldBe Value.Int(1)
    }

    test("failed right side binds nothing") {
      val environment = Environment()
      evaluate("val x = 1 / 0", environment)
      environment["x"] shouldBe null
    }

    test("name warning rejected by the policy is an error") {
      val environment = Environment()
      evaluate("val ｘ = 1", environment, identifierPolicy = { true })
        .shouldBeLeft()
        .shouldBeInstanceOf<RejectedIdentifierWarnings>()
        .node
        .text
        .toString() shouldBe "ｘ"
      environment["ｘ"] shouldBe null
    }

    test("invalid designation is an error") {
      val one = expressionOf("1")
      Expression
        .Assignment(slice("val 1 = 1"), Designation.Invalid(slice("1")), one)
        .evaluate(
          Environment(),
          { false },
          { false },
        ).shouldBeLeft()
        .shouldBeInstanceOf<EvaluationError.InvalidDesignation>()
    }

    test("invalid expression is an error") {
      Expression
        .Invalid(slice("#"))
        .evaluate(
          Environment(),
          { false },
          { false },
        ).shouldBeLeft()
        .shouldBeInstanceOf<EvaluationError.InvalidExpression>()
    }

    test("Unit is not a number") {
      val one = expressionOf("1")
      val unit =
        Expression.Assignment(
          slice("val x = 1"),
          Designation.Name(expressionOf("x").shouldBeInstanceOf()),
          one,
        )
      [
        Expression.UnaryMinus(slice("-"), unit),
        Expression.Plus(slice("+"), unit, one),
        Expression.Times(slice("*"), one, unit),
      ].forEach {
        it
          .evaluate(
            Environment(),
            { false },
            { false },
          ).shouldBeLeft()
          .shouldBeInstanceOf<NotANumber>()
      }
    }

    test("deep trees do not overflow the stack") {
      val one = expressionOf("1")
      val sum =
        (1..DEPTH).fold<Int, Expression>(one) { lhs, _ ->
          Expression.Plus(slice("+"), lhs, one)
        }
      val negation =
        (1..DEPTH).fold<Int, Expression>(one) { operand, _ ->
          Expression.UnaryMinus(slice("-"), operand)
        }
      sum.evaluate(
        Environment(),
        { false },
        { false },
      ) shouldBeRight Value.Int(DEPTH + 1L)
      negation.evaluate(
        Environment(),
        { false },
        { false },
      ) shouldBeRight Value.Int(1)
    }

    test("error has the node where it happened") {
      evaluate("1 + 2 * 2.0")
        .shouldBeLeft()
        .node
        .text
        .toString() shouldBe "2 * 2.0"
    }
  })

private fun evaluate(
  input: String,
  environment: Environment = Environment(),
  numberPolicy: NumberWarningPolicy = { false },
  identifierPolicy: IdentifierWarningPolicy =
    { false },
): Either<EvaluationError, Value> =
  expressionOf(input).evaluate(environment, numberPolicy, identifierPolicy)

private fun expressionOf(
  input: String,
): Expression =
  Parser.ofExpression
    .parse(input)
    .value
    .shouldBeSome()

private fun slice(
  text: String,
): Slice =
  Slice(text.toCharArray(), 0, text.length)

private const val DEPTH = 100_000
