package com.t_mukai.mint.parser

public fun <E> Parser<*, E>.not(): Parser<Unit, E> =
  Not(this)

// region internal

internal class Not<E>(private val inner: Parser<*, E>) : Parser<Unit, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Unit> =
    if (ctx.matches(inner)) {
      ParseResult.failure
    } else {
      ParseResult.unit
    }
}

// endregion
