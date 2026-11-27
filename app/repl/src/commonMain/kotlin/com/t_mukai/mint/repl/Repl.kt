package com.t_mukai.mint.repl

import com.t_mukai.mint.evaluation.Environment
import com.t_mukai.mint.evaluation.EvaluationError
import com.t_mukai.mint.evaluation.IdentifierWarningPolicy
import com.t_mukai.mint.evaluation.NumberWarningPolicy
import com.t_mukai.mint.evaluation.Value
import com.t_mukai.mint.evaluation.evaluate
import com.t_mukai.mint.evaluation.replResultName
import com.t_mukai.mint.parser.Diagnostic
import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.padded
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.repl.ReplOutput.Result
import com.t_mukai.mint.repl.ReplOutput.Result.Bound
import com.t_mukai.mint.repl.ReplOutput.Result.Discarded
import com.t_mukai.mint.repl.ReplOutput.Result.Failed
import com.t_mukai.mint.repl.ReplOutput.Result.NotEvaluated
import com.t_mukai.mint.syntax.Designation
import com.t_mukai.mint.syntax.Expression
import com.t_mukai.mint.syntax.Expression.Assignment
import com.t_mukai.mint.syntax.Expression.Identifier
import com.t_mukai.mint.syntax.NumberWarning
import com.t_mukai.mint.syntax.SyntaxError
import com.t_mukai.mint.syntax.ofExpression

public value class ReplOutput(
  public val diagnostics: List<Diagnostic<Any>>,
  public val result: Result,
) {
  public sealed value class Result {
    public value object NotEvaluated : Result()

    public value class Failed(public val error: EvaluationError) : Result()

    public value class Bound(
      public val name: String,
      public val value: Value,
    ) : Result()

    public value object Discarded : Result()
  }
}

public class ReplSession {
  private val environment = Environment()
  private var resultCount = 0

  public fun run(
    line: String,
  ): ReplOutput =
    outputOf(line)

  // region internal

  private fun outputOf(
    line: String,
  ): ReplOutput {
    val (value, diagnostics) = lineParser.parse(line)
    val evaluable =
      value
        .getOrNull()
        ?.takeIf { diagnostics.none(::isError) }
    return ReplOutput(diagnostics, evaluable?.let(::bind) ?: NotEvaluated)
  }

  private fun bind(
    expression: Expression,
  ): Result =
    expression
      .evaluate(environment, numberPolicy, identifierPolicy)
      .fold(::Failed) { value ->
        when {
          expression is Assignment -> {
            boundOf(expression.designation)
          }

          value is Value.Unit -> {
            Discarded
          }

          expression is Identifier -> {
            Bound(expression.text.toString(), value)
          }

          expression is Expression.ReplResult -> {
            Bound(nameOf(expression), value)
          }

          else -> {
            bindResult(value)
          }
        }
      }

  private fun boundOf(
    designation: Designation,
  ): Bound =
    when (designation) {
      is Designation.Name -> {
        val name = designation.text.toString()
        Bound(name, checkNotNull(environment[name]))
      }

      is Designation.Invalid -> {
        error("invalid designation was bound")
      }
    }

  private fun nameOf(
    reference: Expression.ReplResult,
  ): String =
    replResultName(
      reference.text
        .drop(1)
        .toString()
        .toInt(),
    )

  private fun bindResult(
    value: Value,
  ): Bound {
    resultCount += 1
    val name = replResultName(resultCount)
    environment[name] = value
    return Bound(name, value)
  }

  // endregion
}

// region internal

private val lineParser: Parser<Expression, Any> = Parser.ofExpression.padded()

private val numberPolicy =
  NumberWarningPolicy {
    it == NumberWarning.MissingHexDigits ||
      it == NumberWarning.DecoratedChar ||
      it == NumberWarning.NonAsciiChar
  }

private val identifierPolicy = IdentifierWarningPolicy { true }

private fun isError(
  diagnostic: Diagnostic<Any>,
): Boolean =
  diagnostic is Diagnostic.Unexpected ||
    (diagnostic is Diagnostic.Reported && diagnostic.kind is SyntaxError)

// endregion
