package com.t_mukai.mint.parser

public fun <T, E> Parser<T, E>.andIs(
  other: Parser<*, E>,
): Parser<T, E> =
  AndIs(this, other)

// region internal

internal class AndIs<T, E>(
  val inner: Parser<T, E>,
  private val other: Parser<*, E>,
) : Parser<T, E> {
  private val expected by lazy { inner.start().expected }

  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseIfMatches(ctx, mode)

  private fun parseIfMatches(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    if (!ctx.matches(other)) {
      ctx.record(ctx.pos, expected)
      return ParseResult.failure
    }
    return inner.parse(ctx, mode)
  }
}

// endregion
