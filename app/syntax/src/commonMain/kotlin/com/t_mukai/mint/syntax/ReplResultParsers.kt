package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.labeled
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.map
import com.t_mukai.mint.parser.slice
import com.t_mukai.mint.parser.then
import com.t_mukai.mint.syntax.Expression.ReplResult

@Suppress("UnusedReceiverParameter")
public companion val Parser.ofReplResult: Parser<ReplResult, Nothing>
  get() = replResult

// region internal

private val decimalDigits =
  many(char('0'..'9'))
    .atLeast(1)
    .labeled("decimal digits")

private val replResult: Parser<ReplResult, Nothing> =
  char('$')
    .then(decimalDigits)
    .slice()
    .map(::ReplResult)

// endregion
