package com.t_mukai.mint.parser

import io.kotest.assertions.arrow.core.shouldBeNone
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec

@Suppress("unused")
class PrattTest :
  FunSpec({
    val atom = char('0'..'9').slice().map { it.toString() }
    val group =
      InfixFold<String, Unit> { lhs, _, rhs ->
        "($lhs,$rhs:$slice@${slice.start}..${slice.end})"
      }

    fun binary(
      symbol: Char,
      associativity: Associativity,
    ): Infix<String, Nothing> {
      val operator = char(symbol)
      val fold = InfixFold<String, Unit> { lhs, _, rhs -> "($lhs$symbol$rhs)" }
      return when (associativity) {
        Associativity.Left -> infixLeft(operator, fold)
        Associativity.Right -> infixRight(operator, fold)
        Associativity.None -> infixNone(operator, fold)
      }
    }

    val arithmetic =
      pratt(
        atom,
        [
          [binary('^', Associativity.Right)],
          [binary('*', Associativity.Left), binary('/', Associativity.Left)],
          [binary('+', Associativity.Left), binary('-', Associativity.Left)],
          [binary('=', Associativity.None)],
        ],
      )

    test("earlier levels bind tighter") {
      arithmetic
        .parse("1+2*3")
        .value
        .shouldBeSome("(1+(2*3))")
      arithmetic
        .parse("1*2+3")
        .value
        .shouldBeSome("((1*2)+3)")
    }

    test("operators on the same level associate by their associativity") {
      arithmetic
        .parse("1-2+3")
        .value
        .shouldBeSome("((1-2)+3)")
      arithmetic
        .parse("1^2^3")
        .value
        .shouldBeSome("(1^(2^3))")
    }

    test("non-associative operators do not chain") {
      arithmetic
        .parse("1=2")
        .value
        .shouldBeSome("(1=2)")
      arithmetic
        .parse("1=2=3")
        .value
        .shouldBeNone()
      arithmetic
        .parse("1+2=3*4")
        .value
        .shouldBeSome("((1+2)=(3*4))")
    }

    test("an operator without a right operand is left unconsumed") {
      arithmetic
        .thenIgnore(char('+'))
        .parse("1*2+")
        .value
        .shouldBeSome("(1*2)")
    }

    val affixed =
      pratt(
        atom,
        [
          [postfix(char('!')) { operand, _ -> "($operand!)" }],
          [prefix(char('-')) { _, operand -> "(-$operand)" }],
          [binary('*', Associativity.Left)],
          [binary('+', Associativity.Left)],
          [prefix(char('~')) { _, operand -> "(~$operand)" }],
        ],
      )

    test("prefix operators take operands of their own level or tighter") {
      affixed
        .parse("-2*3")
        .value
        .shouldBeSome("((-2)*3)")
      affixed
        .parse("-3!")
        .value
        .shouldBeSome("(-(3!))")
      affixed
        .parse("~1+2")
        .value
        .shouldBeSome("(~(1+2))")
      affixed
        .parse("1*-2")
        .value
        .shouldBeSome("(1*(-2))")
      affixed
        .parse("--1")
        .value
        .shouldBeSome("(-(-1))")
    }

    test("postfix operators apply repeatedly to the left operand") {
      affixed
        .parse("3!!")
        .value
        .shouldBeSome("((3!)!)")
      affixed
        .parse("2*3!")
        .value
        .shouldBeSome("(2*(3!))")
    }

    test("fold scope covers the whole expression") {
      pratt(atom, [[infixLeft(char('+'), group)]])
        .parse("1+2+3")
        .value
        .shouldBeSome("((1,2:1+2@0..3),3:1+2+3@0..5)")
    }
  })
