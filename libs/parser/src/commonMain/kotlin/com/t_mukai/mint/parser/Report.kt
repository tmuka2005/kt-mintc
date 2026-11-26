package com.t_mukai.mint.parser

public fun <T, E> Parser<T, E>.reporting(
  kind: E,
): Parser<T, E> =
  Reporting(this, kind)

public fun <T, E> Parser<T, E>.or(
  other: Parser<T, E>,
  warn: E,
): Parser<T, E> =
  choice(this, other.reporting(warn))

public fun <T, R, E> Parser<T, E>.mapWithReports(
  transform: (T, List<E>) -> R,
): Parser<R, E> =
  MappedWithReports(this, transform)

// region internal

internal class MappedWithReports<T, R, E>(
  val inner: Parser<T, E>,
  private val transform: (T, List<E>) -> R,
) : Parser<R, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<R> =
    parseCollecting(ctx, mode)

  private fun parseCollecting(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<R> {
    val from = ctx.diagnostics.size
    val result = inner.parse(ctx, mode)
    return when {
      result.isFailure -> {
        ParseResult.failure
      }

      mode == Mode.Check -> {
        ParseResult.omitted
      }

      else -> {
        @Suppress("UNCHECKED_CAST")
        val reports = ctx.reportedKindsSince(from) as List<E>
        ParseResult.success(transform(result.value, reports))
      }
    }
  }
}

internal class Reporting<T, E>(
  val inner: Parser<T, E>,
  private val kind: E,
) : Parser<T, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseReporting(ctx, mode)

  private fun parseReporting(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    val start = ctx.pos
    val result = inner.parse(ctx, mode)
    if (result.isSuccess) {
      ctx.report(start, ctx.pos, kind)
    }
    return result
  }
}

// endregion
