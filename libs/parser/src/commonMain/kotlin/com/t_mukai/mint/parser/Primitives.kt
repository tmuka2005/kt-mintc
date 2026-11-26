package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.BaseMatch
import com.t_mukai.mint.unicode.isDefaultIgnorable
import com.t_mukai.mint.unicode.isXidContinue
import com.t_mukai.mint.unicode.isXidStart
import kotlin.text.unicode.CodePoint
import kotlin.text.unicode.codePointAt
import kotlin.text.unicode.isDigit
import kotlin.text.unicode.toCodePoint

public fun char(
  c: Char,
): Parser<Unit, Nothing> =
  CharParser(c)

public fun char(
  range: CharRange,
): Parser<Unit, Nothing> =
  RangeParser(range)

public fun digit(): Parser<Unit, Nothing> =
  digitParser

public fun xidStart(): Parser<Unit, Nothing> =
  xidStartParser

public fun xidContinue(): Parser<Unit, Nothing> =
  xidContinueParser

public fun defaultIgnorable(): Parser<Unit, Nothing> =
  defaultIgnorableParser

public fun just(
  text: String,
): Parser<Unit, Nothing> =
  LiteralParser(text, wholeWord = false)

public fun keyword(
  text: String,
): Parser<Unit, Nothing> =
  LiteralParser(text, wholeWord = true)

public fun any(): Parser<Unit, Nothing> =
  AnyParser

public fun end(): Parser<Unit, Nothing> =
  EndParser

public fun newline(): Parser<Unit, Nothing> =
  NewlineParser

public fun empty(): Parser<Unit, Nothing> =
  EmptyParser

// region internal

internal fun interface CodePointPredicate {
  fun test(
    codePoint: CodePoint,
  ): Boolean
}

internal class CharParser(val c: Char) : Parser<Unit, Nothing> {
  private val codePoint = c.toCodePoint()
  private val expected = Expected.Char(c)

  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ctx.acceptOrRecord(codePoint, expected)
}

internal class RangeParser(val range: CharRange) : Parser<Unit, Nothing> {
  private val expected = Expected.Range(range)
  private val predicate =
    CodePointPredicate { it.code in range.first.code..range.last.code }

  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ctx.acceptOrRecord(predicate, expected)
}

internal class CodePointParser(
  val label: Expected.Label,
  private val predicate: CodePointPredicate,
) : Parser<Unit, Nothing> {
  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ctx.acceptOrRecord(predicate, label)
}

internal class LiteralParser(
  val text: String,
  private val wholeWord: Boolean,
) : Parser<Unit, Nothing> {
  private val expected = Expected.Literal(text)

  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    parseLiteral(ctx)

  private fun parseLiteral(
    ctx: Context<in Nothing>,
  ): ParseResult<Unit> {
    val start = ctx.mark()
    if (acceptsText(ctx) && !(wholeWord && continuesWord(ctx))) {
      return ParseResult.unit
    }
    ctx.rewind(start)
    ctx.record(start.pos, expected)
    return ParseResult.failure
  }

  private fun acceptsText(
    ctx: Context<in Nothing>,
  ): Boolean {
    var index = 0
    while (index < text.length) {
      val codePoint = text.codePointAt(index)
      if (!ctx.accept(ctx.matchBase(codePoint))) {
        return false
      }
      index += codePoint.size
    }
    return true
  }

  private fun continuesWord(
    ctx: Context<in Nothing>,
  ): Boolean =
    ctx.matchBase(xidContinuePredicate) != BaseMatch.Mismatch
}

internal object AnyParser : Parser<Unit, Nothing> {
  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ctx.advanceOrRecord(!ctx.isAtEnd, Expected.AnyChar)
}

internal object NewlineParser : Parser<Unit, Nothing> {
  val label = Expected.Label("newline")

  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ctx.advanceOrRecord(ctx.peek()?.isNewline() == true, label)
}

internal object EndParser : Parser<Unit, Nothing> {
  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ctx.succeedOrRecord(ctx.isAtEnd, Expected.EndOfInput)
}

internal object EmptyParser : Parser<Unit, Nothing> {
  override fun parse(
    ctx: Context<in Nothing>,
    mode: Mode,
  ): ParseResult<Unit> =
    ParseResult.unit
}

private fun Context<*>.acceptOrRecord(
  codePoint: CodePoint,
  expected: Expected,
): ParseResult<Unit> =
  succeedOrRecord(accept(matchBase(codePoint)), expected)

private fun Context<*>.acceptOrRecord(
  predicate: CodePointPredicate,
  expected: Expected,
): ParseResult<Unit> =
  succeedOrRecord(accept(matchBase(predicate)), expected)

private fun Context<*>.advanceOrRecord(
  matches: Boolean,
  expected: Expected,
): ParseResult<Unit> {
  if (matches) {
    advanceCluster()
  }
  return succeedOrRecord(matches, expected)
}

private fun Context<*>.succeedOrRecord(
  matches: Boolean,
  expected: Expected,
): ParseResult<Unit> {
  if (matches) {
    return ParseResult.unit
  }
  record(pos, expected)
  return ParseResult.failure
}

private val xidContinuePredicate = CodePointPredicate { it.isXidContinue() }

private val digitParser =
  CodePointParser(Expected.Label("digit")) { it.isDigit() }

private val xidStartParser =
  CodePointParser(Expected.Label("XID_Start")) { it.isXidStart() }

private val xidContinueParser =
  CodePointParser(Expected.Label("XID_Continue"), xidContinuePredicate)

private val defaultIgnorableParser =
  CodePointParser(Expected.Label("default ignorable")) {
    it.isDefaultIgnorable()
  }

// endregion
