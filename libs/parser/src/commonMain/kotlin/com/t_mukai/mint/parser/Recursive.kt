package com.t_mukai.mint.parser

public fun <T, E> recursive(
  define: (Parser<T, E>) -> Parser<T, E>,
): Parser<T, E> =
  Recursive(define)

// region internal

internal class Recursive<T, E>(define: (Parser<T, E>) -> Parser<T, E>) :
  Parser<T, E> {
  val inner: Parser<T, E> = define(this)

  private var cachedStart: Start? = null

  fun start(): Start =
    cachedStart ?: computeStart()

  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    inner.parse(ctx, mode)

  private fun computeStart(): Start {
    cachedStart = Start.opaque
    return inner.start().also { cachedStart = it }
  }
}

// endregion
