package com.t_mukai.mint.parser

import kotlin.text.unicode.CodePoint
import kotlin.text.unicode.toCodePoint

public fun <T, E> Parser<T, E>.padded(): Parser<T, E> =
  Padded(this)

public fun <T, E> Parser<T, E>.delimitedBy(
  open: Char,
  close: Char,
  terminated: Boolean = false,
): Parser<T, E> =
  Delimited(this, open, Delimiter(close, terminated))

// region internal

internal class Padded<T, E>(val inner: Parser<T, E>) : Parser<T, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parsePadded(ctx, mode)

  private fun parsePadded(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    ctx.skipPadding()
    val result = inner.parse(ctx, mode)
    if (result.isSuccess) {
      ctx.skipPadding()
    }
    return result
  }
}

internal class Delimited<T, E>(
  private val inner: Parser<T, E>,
  val open: Char,
  private val delimiter: Delimiter,
) : Parser<T, E> {
  private val expectedOpen = Expected.Char(open)
  private val expectedClose = Expected.Char(delimiter.close)
  private val openCodePoint = open.toCodePoint()
  private val closeCodePoint = delimiter.close.toCodePoint()

  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseDelimited(ctx, mode)

  private fun parseDelimited(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    val result =
      if (ctx.acceptDelimiter(openCodePoint, expectedOpen)) {
        parseInner(ctx, mode)
      } else {
        ParseResult.failure
      }
    val closed =
      result.isSuccess && ctx.acceptDelimiter(closeCodePoint, expectedClose)
    return if (closed) {
      result
    } else {
      ParseResult.failure
    }
  }

  private fun parseInner(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    ctx.pushDelimiter(delimiter)
    val result = inner.parse(ctx, mode)
    ctx.popDelimiter()
    return result
  }
}

private fun Context<*>.acceptDelimiter(
  codePoint: CodePoint,
  expected: Expected.Char,
): Boolean {
  val accepted = accept(matchBase(codePoint))
  if (!accepted) {
    record(pos, expected)
  }
  return accepted
}

// endregion
