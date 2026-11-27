package com.t_mukai.mint.benchmark

import com.github.h0tk3y.betterParse.combinators.leftAssociative
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.times
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.use
import com.github.h0tk3y.betterParse.grammar.Grammar
import com.github.h0tk3y.betterParse.grammar.parseToEnd
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser

internal fun parseWithBetterParse(
  input: String,
): Node =
  BetterParseExpression.parseToEnd(input)

// region internal

private object BetterParseExpression : Grammar<Node>() {
  private val digits by regexToken("\\d+")
  private val plus by literalToken("+")
  private val minus by literalToken("-")
  private val times by literalToken("*")
  private val open by literalToken("(")
  private val close by literalToken(")")

  private val number: Parser<Node> by digits use { Node.Num(text.toLong()) }

  private val group: Parser<Node> by
    -open * parser { expression } * -close

  private val product: Parser<Node> by
    leftAssociative(number or group, times use { '*' }) { lhs, operator, rhs ->
      Node.Bin(operator, lhs, rhs)
    }

  private val expression: Parser<Node> by
    leftAssociative(
      product,
      (plus use { '+' }) or (minus use { '-' }),
    ) { lhs, operator, rhs -> Node.Bin(operator, lhs, rhs) }

  override val rootParser: Parser<Node> by expression
}

// endregion
