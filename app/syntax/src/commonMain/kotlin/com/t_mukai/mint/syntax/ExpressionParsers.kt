package com.t_mukai.mint.syntax

import arrow.core.getOrElse
import com.t_mukai.mint.parser.Operator
import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.Seq
import com.t_mukai.mint.parser.Slice
import com.t_mukai.mint.parser.andIs
import com.t_mukai.mint.parser.any
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.choice
import com.t_mukai.mint.parser.commit
import com.t_mukai.mint.parser.empty
import com.t_mukai.mint.parser.ignoreThen
import com.t_mukai.mint.parser.keyword
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.map
import com.t_mukai.mint.parser.newline
import com.t_mukai.mint.parser.not
import com.t_mukai.mint.parser.padded
import com.t_mukai.mint.parser.pratt
import com.t_mukai.mint.parser.recursive
import com.t_mukai.mint.parser.reporting
import com.t_mukai.mint.parser.slice
import com.t_mukai.mint.parser.then
import com.t_mukai.mint.parser.withSlice
import com.t_mukai.mint.syntax.Expression.Assignment

@Suppress("UnusedReceiverParameter")
public companion val Parser.ofExpression: Parser<Expression, Any>
  get() = expression

// region internal

private val arithmetic: Parser<Expression, Any> =
  pratt(
    choice(Parser.ofNumber, Parser.ofIdentifier),
    [
      [Operator.unaryMinus],
      [Operator.times, Operator.div],
      [Operator.plus, Operator.minus],
    ],
  )

private val invalidDesignation: Parser<Designation, Any> =
  many(any().andIs(choice(newline(), char('=').padded()).not()))
    .slice()
    .reporting(SyntaxError.InvalidDesignation)
    .map(Designation::Invalid)

private val expression: Parser<Expression, Any> =
  recursive { expression -> choice(assignment(expression), arithmetic) }

private fun assignment(
  value: Parser<Expression, Any>,
): Parser<Expression, Any> =
  keyword("val")
    .slice()
    .padded()
    .then(binding(value))
    .map(::assignmentOf)

private fun binding(
  value: Parser<Expression, Any>,
): Parser<Seq<Designation, Expression>, Any> =
  choice(
    Parser.ofDesignation.then(rightSide(value)),
    invalidDesignation.then(rightSide(value)),
  )

private fun rightSide(
  value: Parser<Expression, Any>,
): Parser<Expression, Any> =
  char('=')
    .padded()
    .ignoreThen(
      empty()
        .commit()
        .then(value)
        .withSlice()
        .map { expression, text ->
          expression.getOrElse { Expression.Invalid(text) }
        },
    )

private fun assignmentOf(
  keyword: Slice,
  binding: Seq<Designation, Expression>,
): Expression {
  val (source, start) = keyword
  val (first, second) = binding
  return Assignment(Slice(source, start, second.text.end), first, second)
}

// endregion
