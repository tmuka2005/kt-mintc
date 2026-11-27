package com.t_mukai.mint.repl

import com.t_mukai.mint.evaluation.EvaluationError
import com.t_mukai.mint.evaluation.EvaluationError.RejectedIdentifierWarnings
import com.t_mukai.mint.evaluation.EvaluationError.RejectedNumberWarnings
import com.t_mukai.mint.evaluation.EvaluationError.ReplResultIndexOverflow
import com.t_mukai.mint.evaluation.EvaluationError.UndefinedReplResult
import com.t_mukai.mint.evaluation.EvaluationError.UndefinedVariable
import com.t_mukai.mint.evaluation.Value
import com.t_mukai.mint.repl.ReplOutput.Result.Bound
import com.t_mukai.mint.repl.ReplOutput.Result.Discarded
import com.t_mukai.mint.repl.ReplOutput.Result.Failed
import com.t_mukai.mint.repl.ReplOutput.Result.NotEvaluated
import com.t_mukai.mint.syntax.NumberWarning
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

@Suppress("unused")
class ReplTest :
  FunSpec({
    test("expression result is bound to a numbered name") {
      ReplSession().run("1 + 2").bound() shouldBe
        Bound("$1", Value.Int(3))
    }

    test("numbered result is referenced in later lines") {
      val session = ReplSession()
      session.run("1 + 2")
      session.run("$1 * 10").bound() shouldBe Bound("$2", Value.Int(30))
    }

    test("val expression shows the bound name and value") {
      ReplSession().run("val x = 1 + 2").bound() shouldBe
        Bound("x", Value.Int(3))
    }

    test("nested val expression shows only the outer name") {
      ReplSession().run("val x = val y = 1").bound() shouldBe
        Bound("x", Value.Unit)
    }

    test("val expressions and failures do not take a number") {
      val session = ReplSession()
      session.run("val x = 2")
      session.run("1 / 0")
      session.run("x + 1").bound() shouldBe Bound("$1", Value.Int(3))
    }

    test("Unit result is discarded without a number") {
      val session = ReplSession()
      session.run("val u = val w = 1")
      session.run("u").result shouldBe Discarded
      session.run("1").bound() shouldBe Bound("$1", Value.Int(1))
    }

    test("variable reference shows the variable without a number") {
      val session = ReplSession()
      session.run("val a = 1")
      session.run("a").bound() shouldBe Bound("a", Value.Int(1))
      session.run("2").bound() shouldBe Bound("$1", Value.Int(2))
    }

    test("numbered result reference shows the same number") {
      val session = ReplSession()
      session.run("1")
      session.run("$1").bound() shouldBe Bound("$1", Value.Int(1))
      session.run("2").bound() shouldBe Bound("$2", Value.Int(2))
    }

    test("undefined numbered result is an error") {
      ReplSession()
        .run("$1")
        .error()
        .shouldBeInstanceOf<UndefinedReplResult>()
    }

    test("numbered result beyond Int is an error") {
      ReplSession()
        .run("$99999999999")
        .error()
        .shouldBeInstanceOf<ReplResultIndexOverflow>()
    }

    test("spaces at the start and end of the line are skipped") {
      ReplSession()
        .run(" a")
        .error()
        .shouldBeInstanceOf<UndefinedVariable>()
      ReplSession()
        .run("1 ")
        .bound()
        .value shouldBe Value.Int(1)
    }

    test("line with unexpected diagnostics is not evaluated") {
      ReplSession()
        .run("1 +")
        .result shouldBe NotEvaluated
    }

    test("line with an invalid designation is not evaluated") {
      ReplSession()
        .run("val 1 = 2")
        .result shouldBe NotEvaluated
    }

    test("hex prefix and digit case are allowed") {
      ReplSession()
        .run("0Xff")
        .bound()
        .value shouldBe Value.Int(255)
    }

    test("missing hex digits, decorations and non-ASCII are rejected") {
      [
        "0x" to NumberWarning.MissingHexDigits,
        "1\u0301" to NumberWarning.DecoratedChar,
        "１" to NumberWarning.NonAsciiChar,
      ].forEach { [line, warning] ->
        ReplSession()
          .run(line)
          .error()
          .shouldBeInstanceOf<RejectedNumberWarnings>()
          .warnings shouldBe [warning]
      }
    }

    test("identifier warnings are rejected") {
      ReplSession()
        .run("ｘ")
        .error()
        .shouldBeInstanceOf<RejectedIdentifierWarnings>()
    }
  })

private fun ReplOutput.bound(): Bound =
  result.shouldBeInstanceOf<Bound>()

private fun ReplOutput.error(): EvaluationError =
  result.shouldBeInstanceOf<Failed>().error
