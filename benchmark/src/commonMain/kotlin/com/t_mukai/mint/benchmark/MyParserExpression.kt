package com.t_mukai.mint.benchmark

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.choice
import com.t_mukai.mint.parser.delimitedBy
import com.t_mukai.mint.parser.infixLeft
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.map
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.parser.pratt
import com.t_mukai.mint.parser.recursive
import com.t_mukai.mint.parser.slice

internal fun parseWithMyParser(
  input: String,
): Node? =
  myParserExpression
    .parse(input)
    .value
    .getOrNull()

// region internal

private val number: Parser<Node, Nothing> =
  many(char('0'..'9'))
    .atLeast(1)
    .slice()
    .map { Node.Num(it.toString().toLong()) }

private val myParserExpression: Parser<Node, Nothing> =
  recursive { expression ->
    pratt(
      choice(number, expression.delimitedBy('(', ')')),
      [
        [binary('*')],
        [binary('+'), binary('-')],
      ],
    )
  }

private fun binary(
  operator: Char,
) =
  infixLeft<Node, Unit, Nothing>(char(operator)) { lhs, _, rhs ->
    Node.Bin(operator, lhs, rhs)
  }

// endregion
