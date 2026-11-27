package com.t_mukai.mint.benchmark

import me.alllex.parsus.parser.Grammar
import me.alllex.parsus.parser.Parser
import me.alllex.parsus.parser.leftAssociative
import me.alllex.parsus.parser.map
import me.alllex.parsus.parser.or
import me.alllex.parsus.parser.ref
import me.alllex.parsus.parser.times
import me.alllex.parsus.parser.unaryMinus
import me.alllex.parsus.token.literalToken
import me.alllex.parsus.token.regexToken

internal fun parseWithParsus(
  input: String,
): Node =
  ParsusExpression.parseOrThrow(input)

// region internal

private object ParsusExpression : Grammar<Node>() {
  private val digits by regexToken("\\d+")
  private val plus by literalToken("+")
  private val minus by literalToken("-")
  private val times by literalToken("*")
  private val open by literalToken("(")
  private val close by literalToken(")")

  private val number: Parser<Node> by
    digits map { Node.Num(it.text.toLong()) }

  private val group: Parser<Node> by -open * ref(::expression) * -close

  private val product: Parser<Node> by
    leftAssociative(number or group, times map '*') { lhs, operator, rhs ->
      Node.Bin(operator, lhs, rhs)
    }

  private val expression: Parser<Node> by
    leftAssociative(product, (plus map '+') or (minus map '-')) {
      lhs,
      operator,
      rhs,
      ->
      Node.Bin(operator, lhs, rhs)
    }

  override val root: Parser<Node> by expression
}

// endregion
