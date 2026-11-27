package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Infix
import com.t_mukai.mint.parser.Operator
import com.t_mukai.mint.parser.Prefix
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.infixLeft
import com.t_mukai.mint.parser.padded
import com.t_mukai.mint.parser.prefix
import com.t_mukai.mint.syntax.Expression.Div
import com.t_mukai.mint.syntax.Expression.Minus
import com.t_mukai.mint.syntax.Expression.Plus
import com.t_mukai.mint.syntax.Expression.Times
import com.t_mukai.mint.syntax.Expression.UnaryMinus

@Suppress("UnusedReceiverParameter")
public companion val Operator.plus: Infix<Expression, Nothing>
  get() = plusOperator

@Suppress("UnusedReceiverParameter")
public companion val Operator.minus: Infix<Expression, Nothing>
  get() = minusOperator

@Suppress("UnusedReceiverParameter")
public companion val Operator.times: Infix<Expression, Nothing>
  get() = timesOperator

@Suppress("UnusedReceiverParameter")
public companion val Operator.div: Infix<Expression, Nothing>
  get() = divOperator

@Suppress("UnusedReceiverParameter")
public companion val Operator.unaryMinus: Prefix<Expression, Nothing>
  get() = unaryMinusOperator

// region internal

private val plusOperator: Infix<Expression, Nothing> =
  infixLeft(char('+').padded()) { lhs, _, rhs -> Plus(slice, lhs, rhs) }

private val minusOperator: Infix<Expression, Nothing> =
  infixLeft(char('-').padded()) { lhs, _, rhs -> Minus(slice, lhs, rhs) }

private val timesOperator: Infix<Expression, Nothing> =
  infixLeft(char('*').padded()) { lhs, _, rhs -> Times(slice, lhs, rhs) }

private val divOperator: Infix<Expression, Nothing> =
  infixLeft(char('/').padded()) { lhs, _, rhs -> Div(slice, lhs, rhs) }

private val unaryMinusOperator: Prefix<Expression, Nothing> =
  prefix(char('-').padded()) { _, operand -> UnaryMinus(slice, operand) }

// endregion
