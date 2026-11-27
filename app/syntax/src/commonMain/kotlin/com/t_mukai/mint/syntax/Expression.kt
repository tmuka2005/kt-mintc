package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Slice
import com.t_mukai.mint.syntax.Expression.Assignment
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.syntax.Expression.Primitive.Number.FloatDec
import com.t_mukai.mint.syntax.Expression.Primitive.Number.IntDec
import com.t_mukai.mint.syntax.Expression.Primitive.Number.IntHex

public sealed value class Expression {
  public abstract val text: Slice

  final override fun equals(
    other: Any?,
  ): Boolean =
    other is Expression && expressionEquals(this to other)

  final override fun hashCode(): Int =
    expressionHashCode(this)

  final override fun toString(): String =
    expressionToString(this)

  public value class Identifier internal constructor(
    override val text: Slice,
    public val warnings: List<IdentifierWarning>,
  ) : Expression()

  public sealed value class Primitive : Expression() {
    public sealed value class Number : Primitive() {
      public abstract val warnings: List<NumberWarning>

      public value class IntDec internal constructor(
        override val text: Slice,
        override val warnings: List<NumberWarning>,
      ) : Number()

      public value class FloatDec internal constructor(
        override val text: Slice,
        override val warnings: List<NumberWarning>,
      ) : Number()

      public value class IntHex internal constructor(
        override val text: Slice,
        override val warnings: List<NumberWarning>,
      ) : Number()
    }
  }

  public value class Plus(
    override val text: Slice,
    public val lhs: Expression,
    public val rhs: Expression,
  ) : Expression()

  public value class Minus(
    override val text: Slice,
    public val lhs: Expression,
    public val rhs: Expression,
  ) : Expression()

  public value class Times(
    override val text: Slice,
    public val lhs: Expression,
    public val rhs: Expression,
  ) : Expression()

  public value class Div(
    override val text: Slice,
    public val lhs: Expression,
    public val rhs: Expression,
  ) : Expression()

  public value class UnaryMinus(
    override val text: Slice,
    public val operand: Expression,
  ) : Expression()

  public value class Assignment(
    override val text: Slice,
    public val designation: Designation,
    public val value: Expression,
  ) : Expression()

  public value class ReplResult(override val text: Slice) : Expression()

  public value class Invalid(override val text: Slice) : Expression()
}

// region internal

private typealias EqualsScope =
  DeepRecursiveScope<Pair<Expression, Expression>, Boolean>

private val expressionEquals:
  DeepRecursiveFunction<Pair<Expression, Expression>, Boolean> =
  DeepRecursiveFunction { [a, b] ->
    when (a) {
      is Identifier -> {
        b is Identifier && a.text == b.text && a.warnings == b.warnings
      }

      is IntDec -> {
        b is IntDec && a.text == b.text && a.warnings == b.warnings
      }

      is FloatDec -> {
        b is FloatDec && a.text == b.text && a.warnings == b.warnings
      }

      is IntHex -> {
        b is IntHex && a.text == b.text && a.warnings == b.warnings
      }

      is ReplResult -> {
        b is ReplResult && a.text == b.text
      }

      is Invalid -> {
        b is Invalid && a.text == b.text
      }

      is Plus -> {
        b is Plus && sameBinary(a.text, a.lhs, a.rhs, b.text, b.lhs, b.rhs)
      }

      is Minus -> {
        b is Minus && sameBinary(a.text, a.lhs, a.rhs, b.text, b.lhs, b.rhs)
      }

      is Times -> {
        b is Times && sameBinary(a.text, a.lhs, a.rhs, b.text, b.lhs, b.rhs)
      }

      is Div -> {
        b is Div && sameBinary(a.text, a.lhs, a.rhs, b.text, b.lhs, b.rhs)
      }

      is UnaryMinus -> {
        b is UnaryMinus &&
          a.text == b.text &&
          callRecursive(a.operand to b.operand)
      }

      is Assignment -> {
        b is Assignment &&
          a.text == b.text &&
          a.designation == b.designation &&
          callRecursive(a.value to b.value)
      }
    }
  }

private val expressionHashCode =
  DeepRecursiveFunction<Expression, Int> { e ->
    when (e) {
      is Identifier -> combine(e.text.hashCode(), e.warnings.hashCode())
      is Number -> combine(e.text.hashCode(), e.warnings.hashCode())
      is ReplResult -> e.text.hashCode()
      is Invalid -> e.text.hashCode()
      is Plus -> binaryHash(e.text, e.lhs, e.rhs)
      is Minus -> binaryHash(e.text, e.lhs, e.rhs)
      is Times -> binaryHash(e.text, e.lhs, e.rhs)
      is Div -> binaryHash(e.text, e.lhs, e.rhs)
      is UnaryMinus -> combine(e.text.hashCode(), callRecursive(e.operand))
      is Assignment -> assignmentHash(e)
    }
  }

private const val HASH_MULTIPLIER = 31

private val expressionToString =
  DeepRecursiveFunction<Expression, String> { e ->
    when (e) {
      is Identifier -> leafString("Identifier", e.text, e.warnings)
      is IntDec -> leafString("IntDec", e.text, e.warnings)
      is FloatDec -> leafString("FloatDec", e.text, e.warnings)
      is IntHex -> leafString("IntHex", e.text, e.warnings)
      is ReplResult -> "ReplResult(text=${e.text})"
      is Invalid -> "Invalid(text=${e.text})"
      is Plus -> binaryString("Plus", e.text, e.lhs, e.rhs)
      is Minus -> binaryString("Minus", e.text, e.lhs, e.rhs)
      is Times -> binaryString("Times", e.text, e.lhs, e.rhs)
      is Div -> binaryString("Div", e.text, e.lhs, e.rhs)
      is UnaryMinus -> unaryString(e.text, callRecursive(e.operand))
      is Assignment -> assignmentString(e, callRecursive(e.value))
    }
  }

private suspend fun EqualsScope.sameBinary(
  text: Slice,
  lhs: Expression,
  rhs: Expression,
  otherText: Slice,
  otherLhs: Expression,
  otherRhs: Expression,
): Boolean =
  text == otherText &&
    callRecursive(lhs to otherLhs) &&
    callRecursive(rhs to otherRhs)

private suspend fun DeepRecursiveScope<Expression, Int>.binaryHash(
  text: Slice,
  lhs: Expression,
  rhs: Expression,
): Int =
  combine(combine(text.hashCode(), callRecursive(lhs)), callRecursive(rhs))

private suspend fun DeepRecursiveScope<Expression, Int>.assignmentHash(
  assignment: Assignment,
): Int =
  combine(
    combine(assignment.text.hashCode(), assignment.designation.hashCode()),
    callRecursive(assignment.value),
  )

private fun combine(
  hash: Int,
  next: Int,
): Int =
  HASH_MULTIPLIER * hash + next

private fun leafString(
  name: String,
  text: Slice,
  warnings: List<*>,
): String =
  "$name(text=$text, warnings=$warnings)"

private suspend fun DeepRecursiveScope<Expression, String>.binaryString(
  name: String,
  text: Slice,
  lhs: Expression,
  rhs: Expression,
): String =
  "$name(text=$text, lhs=${callRecursive(lhs)}, rhs=${callRecursive(rhs)})"

private fun unaryString(
  text: Slice,
  operand: String,
): String =
  "UnaryMinus(text=$text, operand=$operand)"

private fun assignmentString(
  assignment: Assignment,
  value: String,
): String =
  "Assignment(text=${assignment.text}, " +
    "designation=${assignment.designation}, value=$value)"

// endregion
