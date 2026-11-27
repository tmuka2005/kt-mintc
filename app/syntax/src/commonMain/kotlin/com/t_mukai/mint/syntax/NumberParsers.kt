package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.choice
import com.t_mukai.mint.parser.digit
import com.t_mukai.mint.parser.just
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.map
import com.t_mukai.mint.parser.mapWithReports
import com.t_mukai.mint.parser.or
import com.t_mukai.mint.parser.reporting
import com.t_mukai.mint.parser.slice
import com.t_mukai.mint.parser.then
import com.t_mukai.mint.syntax.Expression.Primitive.Number
import com.t_mukai.mint.syntax.NumberWarning.DecoratedChar
import com.t_mukai.mint.syntax.NumberWarning.LowerHexDigit
import com.t_mukai.mint.syntax.NumberWarning.MissingHexDigits
import com.t_mukai.mint.syntax.NumberWarning.NonAsciiChar
import com.t_mukai.mint.syntax.NumberWarning.UpperHexPrefix

@Suppress("UnusedReceiverParameter")
public companion val Parser.ofNumber: Parser<Number, NumberWarning>
  get() = number

// region internal

private val decimalDigit = char('0'..'9').or(digit(), NonAsciiChar)

private val decimalDigits = many(decimalDigit).atLeast(1)

private val decimalPoint =
  char('.').or(
    choice(char('．'), char('。'), char('｡')),
    NonAsciiChar,
  )

private val hexZero = char('0').or(char('０'), NonAsciiChar)

private val hexX =
  char('x')
    .or(char('X'), UpperHexPrefix)
    .or(choice(char('ｘ'), char('Ｘ')), NonAsciiChar)

private val hexDigit =
  choice(
    decimalDigit,
    char('A'..'F'),
    char('a'..'f').reporting(LowerHexDigit),
    char('Ａ'..'Ｆ').reporting(NonAsciiChar),
    char('ａ'..'ｆ')
      .reporting(LowerHexDigit)
      .reporting(NonAsciiChar),
  )

private val hexDigits =
  many(hexDigit)
    .atLeast(1)
    .map { }
    .or(just(""), MissingHexDigits)

private val intHex =
  hexZero
    .then(hexX)
    .then(hexDigits)
    .slice()
    .mapWithReports(DecoratedChar, Number::IntHex)

private val floatDec =
  decimalDigits
    .then(decimalPoint)
    .then(decimalDigits)
    .slice()
    .mapWithReports(DecoratedChar, Number::FloatDec)

private val intDec =
  decimalDigits
    .slice()
    .mapWithReports(DecoratedChar, Number::IntDec)

private val number: Parser<Number, NumberWarning> =
  choice(intHex, floatDec, intDec, label = "number")

// endregion
