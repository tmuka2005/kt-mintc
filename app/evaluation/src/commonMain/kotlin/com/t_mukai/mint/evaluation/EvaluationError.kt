package com.t_mukai.mint.evaluation

import com.t_mukai.mint.syntax.Expression
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.syntax.FloatLiteralParseError
import com.t_mukai.mint.syntax.IdentifierWarning
import com.t_mukai.mint.syntax.IntLiteralParseError
import com.t_mukai.mint.syntax.NumberWarning

public sealed value class EvaluationError {
  public abstract val node: Expression

  public value class RejectedNumberWarnings(
    override val node: Number,
    public val warnings: List<NumberWarning>,
  ) : EvaluationError()

  public value class RejectedIdentifierWarnings(
    override val node: Expression.Identifier,
    public val warnings: List<IdentifierWarning>,
  ) : EvaluationError()

  public value class IntLiteral(
    override val node: Number,
    public val error: IntLiteralParseError,
  ) : EvaluationError()

  public value class FloatLiteral(
    override val node: Number.FloatDec,
    public val error: FloatLiteralParseError,
  ) : EvaluationError()

  public value class MixedNumberTypes(override val node: Expression) :
    EvaluationError()

  public value class NotANumber(override val node: Expression) :
    EvaluationError()

  public value class IntOverflow(override val node: Expression) :
    EvaluationError()

  public value class DivisionByZero(override val node: Expression.Div) :
    EvaluationError()

  public value class NonFiniteFloat(override val node: Expression) :
    EvaluationError()

  public value class UndefinedVariable(
    override val node: Expression.Identifier,
  ) : EvaluationError()

  public value class ReplResultIndexOverflow(
    override val node: Expression.ReplResult,
  ) : EvaluationError()

  public value class InvalidDesignation(
    override val node: Expression.Assignment,
  ) : EvaluationError()

  public value class InvalidExpression(override val node: Expression.Invalid) :
    EvaluationError()

  public value class UndefinedReplResult(
    override val node: Expression.ReplResult,
  ) : EvaluationError()
}
