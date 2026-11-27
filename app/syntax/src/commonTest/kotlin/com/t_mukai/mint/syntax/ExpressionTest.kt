package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Slice
import com.t_mukai.mint.syntax.Expression.Primitive.Number.IntDec
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

@Suppress("unused")
class ExpressionTest :
  FunSpec({
    test("equal trees are equal and have the same hash") {
      val one = plus(intDec("1"), intDec("2"))
      val other = plus(intDec("1"), intDec("2"))
      one shouldBe other
      one.hashCode() shouldBe other.hashCode()
    }

    test("trees with different nodes are not equal") {
      plus(intDec("1"), intDec("2")) shouldNotBe
        Expression.Minus(slice("1"), intDec("1"), intDec("2"))
      plus(intDec("1"), intDec("2")) shouldNotBe plus(intDec("1"), intDec("3"))
    }

    test("toString shows the tree") {
      plus(intDec("1"), intDec("2")).toString() shouldBe
        "Plus(text=1, lhs=IntDec(text=1, warnings=[]), " +
        "rhs=IntDec(text=2, warnings=[]))"
    }

    test("deep trees do not overflow the stack") {
      val one = deep()
      val other = deep()
      (one == other) shouldBe true
      one.hashCode() shouldBe other.hashCode()
      one.toString().length shouldBe other.toString().length
    }
  })

private fun slice(
  text: String,
): Slice =
  Slice(text.toCharArray(), 0, text.length)

private fun intDec(
  text: String,
): Expression =
  IntDec(slice(text), emptyList())

private fun plus(
  lhs: Expression,
  rhs: Expression,
): Expression =
  Expression.Plus(slice("1"), lhs, rhs)

private fun deep(): Expression =
  (1..100_000).fold(intDec("1")) { operand, _ ->
    Expression.UnaryMinus(slice("-"), operand)
  }
