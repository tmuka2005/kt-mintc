package com.t_mukai.mint.evaluation

import arrow.core.Either
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import com.t_mukai.mint.evaluation.EvaluationError.DivisionByZero
import com.t_mukai.mint.evaluation.EvaluationError.FloatLiteral
import com.t_mukai.mint.evaluation.EvaluationError.IntLiteral
import com.t_mukai.mint.evaluation.EvaluationError.IntOverflow
import com.t_mukai.mint.evaluation.EvaluationError.InvalidDesignation
import com.t_mukai.mint.evaluation.EvaluationError.InvalidExpression
import com.t_mukai.mint.evaluation.EvaluationError.MixedNumberTypes
import com.t_mukai.mint.evaluation.EvaluationError.NonFiniteFloat
import com.t_mukai.mint.evaluation.EvaluationError.NotANumber
import com.t_mukai.mint.evaluation.EvaluationError.RejectedIdentifierWarnings
import com.t_mukai.mint.evaluation.EvaluationError.RejectedNumberWarnings
import com.t_mukai.mint.evaluation.EvaluationError.ReplResultIndexOverflow
import com.t_mukai.mint.evaluation.EvaluationError.UndefinedReplResult
import com.t_mukai.mint.evaluation.EvaluationError.UndefinedVariable
import com.t_mukai.mint.syntax.Designation
import com.t_mukai.mint.syntax.Expression
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.syntax.toDouble
import com.t_mukai.mint.syntax.toLong

public fun Expression.evaluate(
  environment: Environment,
  numberPolicy: NumberWarningPolicy,
  identifierPolicy: IdentifierWarningPolicy,
): Either<EvaluationError, Value> =
  evaluateIn(this, environment, numberPolicy, identifierPolicy)

// region internal

private fun evaluateIn(
  expression: Expression,
  environment: Environment,
  numberPolicy: NumberWarningPolicy,
  identifierPolicy: IdentifierWarningPolicy,
): Either<EvaluationError, Value> =
  either {
    Evaluator(this, environment, numberPolicy, identifierPolicy)
      .evaluate(expression)
  }

private class Evaluator(
  raise: Raise<EvaluationError>,
  private val environment: Environment,
  private val numberPolicy: NumberWarningPolicy,
  private val identifierPolicy: IdentifierWarningPolicy,
) : Raise<EvaluationError> by raise {
  val evaluate =
    DeepRecursiveFunction<Expression, Value> { expression ->
      when (expression) {
        is Expression.Identifier -> {
          accept(expression)
          environment[expression.text.toString()]
            ?: raise(UndefinedVariable(expression))
        }

        is Expression.ReplResult -> {
          val index =
            expression.text
              .drop(1)
              .toString()
              .toIntOrNull()
              ?: raise(ReplResultIndexOverflow(expression))
          environment[replResultName(index)]
            ?: raise(UndefinedReplResult(expression))
        }

        is Number.IntDec -> {
          intDec(expression, negated = false)
        }

        is Number.IntHex -> {
          accept(expression)
          Value.Int(
            expression
              .toLong()
              .mapLeft { IntLiteral(expression, it) }
              .bind(),
          )
        }

        is Number.FloatDec -> {
          accept(expression)
          Value.Float(
            expression
              .toDouble()
              .mapLeft { FloatLiteral(expression, it) }
              .bind(),
          )
        }

        is Expression.Plus -> {
          arithmetic(
            expression,
            expression.lhs,
            expression.rhs,
            { lhs, rhs -> lhs.plusExact(rhs) },
            { lhs, rhs -> lhs + rhs },
          )
        }

        is Expression.Minus -> {
          arithmetic(
            expression,
            expression.lhs,
            expression.rhs,
            { lhs, rhs -> lhs.minusExact(rhs) },
            { lhs, rhs -> lhs - rhs },
          )
        }

        is Expression.Times -> {
          arithmetic(
            expression,
            expression.lhs,
            expression.rhs,
            { lhs, rhs -> lhs.timesExact(rhs) },
            { lhs, rhs -> lhs * rhs },
          )
        }

        is Expression.Div -> {
          arithmetic(
            expression,
            expression.lhs,
            expression.rhs,
            { lhs, rhs ->
              ensure(rhs != 0L) { DivisionByZero(expression) }
              lhs.divExact(rhs)
            },
            { lhs, rhs -> lhs / rhs },
          )
        }

        is Expression.UnaryMinus -> {
          negate(expression)
        }

        is Expression.Assignment -> {
          bind(expression, callRecursive(expression.value))
          Value.Unit
        }

        is Expression.Invalid -> {
          raise(InvalidExpression(expression))
        }
      }
    }

  private suspend fun DeepRecursiveScope<Expression, Value>.negate(
    expression: Expression.UnaryMinus,
  ): Value {
    val operand = expression.operand
    if (operand is Number.IntDec) {
      return intDec(operand, negated = true)
    }
    return when (val value = callRecursive(operand)) {
      is Value.Int -> {
        Value.Int(
          0L.minusExact(value.value) ?: raise(IntOverflow(expression)),
        )
      }

      is Value.Float -> {
        Value.Float(-value.value)
      }

      is Value.Unit -> {
        raise(NotANumber(expression))
      }
    }
  }

  private suspend inline fun DeepRecursiveScope<Expression, Value>.arithmetic(
    expression: Expression,
    lhs: Expression,
    rhs: Expression,
    int: (Long, Long) -> Long?,
    float: (Double, Double) -> Double,
  ): Value {
    val left = callRecursive(lhs)
    val right = callRecursive(rhs)
    return if (left is Value.Int && right is Value.Int) {
      Value.Int(
        int(left.value, right.value) ?: raise(IntOverflow(expression)),
      )
    } else if (left is Value.Float && right is Value.Float) {
      val value = float(left.value, right.value)
      ensure(value.isFinite()) { NonFiniteFloat(expression) }
      Value.Float(value)
    } else if (left is Value.Unit || right is Value.Unit) {
      raise(NotANumber(expression))
    } else {
      raise(MixedNumberTypes(expression))
    }
  }

  private fun bind(
    assignment: Expression.Assignment,
    value: Value,
  ) {
    when (val designation = assignment.designation) {
      is Designation.Name -> {
        accept(designation.identifier)
        environment[designation.text.toString()] = value
      }

      is Designation.Invalid -> {
        raise(InvalidDesignation(assignment))
      }
    }
  }

  private fun intDec(
    expression: Number.IntDec,
    negated: Boolean,
  ): Value {
    accept(expression)
    return Value.Int(
      expression
        .toLong(negated)
        .mapLeft { IntLiteral(expression, it) }
        .bind(),
    )
  }

  private fun accept(
    expression: Number,
  ) {
    val rejected = expression.warnings.filter(numberPolicy::isError)
    ensure(rejected.isEmpty()) { RejectedNumberWarnings(expression, rejected) }
  }

  private fun accept(
    expression: Expression.Identifier,
  ) {
    val rejected = expression.warnings.filter(identifierPolicy::isError)
    ensure(rejected.isEmpty()) {
      RejectedIdentifierWarnings(expression, rejected)
    }
  }
}

// endregion
