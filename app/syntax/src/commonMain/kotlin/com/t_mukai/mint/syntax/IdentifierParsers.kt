package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.andIs
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.choice
import com.t_mukai.mint.parser.defaultIgnorable
import com.t_mukai.mint.parser.labeled
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.mapWithReports
import com.t_mukai.mint.parser.reporting
import com.t_mukai.mint.parser.slice
import com.t_mukai.mint.parser.then
import com.t_mukai.mint.parser.xidContinue
import com.t_mukai.mint.parser.xidStart
import com.t_mukai.mint.syntax.Expression.Identifier
import com.t_mukai.mint.syntax.IdentifierWarning.DecoratedChar
import com.t_mukai.mint.syntax.IdentifierWarning.InvisibleLetter
import com.t_mukai.mint.syntax.IdentifierWarning.NonAsciiChar

@Suppress("UnusedReceiverParameter")
public companion val Parser.ofIdentifier: Parser<Identifier, IdentifierWarning>
  get() = identifier

// region internal

private val asciiHead = choice(char('a'..'z'), char('A'..'Z'), char('_'))

private val head =
  choice(
    asciiHead,
    invisible(xidStart()),
    xidStart().reporting(NonAsciiChar),
  )

private val tail =
  choice(
    asciiHead,
    char('0'..'9'),
    invisible(xidContinue()),
    xidContinue().reporting(NonAsciiChar),
  )

private fun invisible(
  parser: Parser<Unit, IdentifierWarning>,
): Parser<Unit, IdentifierWarning> =
  parser
    .andIs(defaultIgnorable())
    .reporting(InvisibleLetter)
    .reporting(NonAsciiChar)

private val identifier: Parser<Identifier, IdentifierWarning> =
  head
    .then(many(tail))
    .slice()
    .mapWithReports(DecoratedChar, ::Identifier)
    .labeled("identifier")

// endregion
