package com.t_mukai.mint.parser

import kotlin.jvm.JvmName

public fun <A, B, E> Parser<A, E>.then(
  next: Parser<B, E>,
): Parser<Seq<A, B>, E> =
  Then(this, next)

public fun <A, E> Parser<A, E>.thenIgnore(
  next: Parser<*, E>,
): Parser<A, E> =
  ThenIgnore(this, next)

public fun <B, E> Parser<*, E>.ignoreThen(
  next: Parser<B, E>,
): Parser<B, E> =
  IgnoreThen(this, next)

public fun <T, R, E> Parser<T, E>.map(
  transform: (T) -> R,
): Parser<R, E> =
  Mapped(this, transform)

@JvmName("map2")
public fun <T1, T2, R, E> Parser<Seq<T1, T2>, E>.map(
  transform: (T1, T2) -> R,
): Parser<R, E> =
  Mapped(this) { (first, second) -> transform(first, second) }

@JvmName("map3")
public fun <T1, T2, T3, R, E> Parser<Seq<Seq<T1, T2>, T3>, E>.map(
  transform: (T1, T2, T3) -> R,
): Parser<R, E> =
  Mapped(this) { unpack3(it, transform) }

@JvmName("map4")
public fun <T1, T2, T3, T4, R, E> Parser<Seq<Seq<Seq<T1, T2>, T3>, T4>, E>.map(
  transform: (T1, T2, T3, T4) -> R,
): Parser<R, E> =
  Mapped(this) { unpack4(it, transform) }

@JvmName("map5")
public fun <T1, T2, T3, T4, T5, R, E> Parser<
  Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>,
  E,
>.map(
  transform: (T1, T2, T3, T4, T5) -> R,
): Parser<R, E> =
  Mapped(this) { unpack5(it, transform) }

@JvmName("map6")
public fun <T1, T2, T3, T4, T5, T6, R, E> Parser<
  Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>,
  E,
>.map(
  transform: (T1, T2, T3, T4, T5, T6) -> R,
): Parser<R, E> =
  Mapped(this) { unpack6(it, transform) }

@JvmName("map7")
public fun <T1, T2, T3, T4, T5, T6, T7, R, E> Parser<
  Seq<Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>, T7>,
  E,
>.map(
  transform: (T1, T2, T3, T4, T5, T6, T7) -> R,
): Parser<R, E> =
  Mapped(this) { unpack7(it, transform) }

@JvmName("map8")
public fun <T1, T2, T3, T4, T5, T6, T7, T8, R, E> Parser<
  Seq<Seq<Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>, T7>, T8>,
  E,
>.map(
  transform: (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Parser<R, E> =
  Mapped(this) { unpack8(it, transform) }

@JvmName("map9")
public fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R, E> Parser<
  Seq<Seq<Seq<Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>, T7>, T8>, T9>,
  E,
>.map(
  transform: (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R,
): Parser<R, E> =
  Mapped(this) { unpack9(it, transform) }

public fun <E> Parser<*, E>.slice(): Parser<Slice, E> =
  Sliced(this)

public fun <T, E> Parser<T, E>.withSlice(): Parser<Seq<T, Slice>, E> =
  WithSlice(this)

// region internal

internal class Then<A, B, E>(
  val first: Parser<A, E>,
  val second: Parser<B, E>,
) : Parser<Seq<A, B>, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Seq<A, B>> =
    parseBoth(ctx, mode)

  private fun parseBoth(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Seq<A, B>> {
    val a = first.parse(ctx, mode)
    val b =
      if (a.isSuccess) {
        second.parse(ctx, mode)
      } else {
        ParseResult.failure
      }
    return when {
      b.isFailure -> ParseResult.failure
      mode == Mode.Check -> ParseResult.omitted
      else -> ParseResult.success(Seq(a.value, b.value))
    }
  }
}

internal class ThenIgnore<A, E>(
  val first: Parser<A, E>,
  val second: Parser<*, E>,
) : Parser<A, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<A> =
    parseBoth(ctx, mode)

  private fun parseBoth(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<A> {
    val a = first.parse(ctx, mode)
    if (a.isSuccess && second.parse(ctx, Mode.Check).isSuccess) {
      return a
    }
    return ParseResult.failure
  }
}

internal class IgnoreThen<B, E>(
  val first: Parser<*, E>,
  val second: Parser<B, E>,
) : Parser<B, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<B> =
    parseBoth(ctx, mode)

  private fun parseBoth(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<B> {
    if (first.parse(ctx, Mode.Check).isFailure) {
      return ParseResult.failure
    }
    return second.parse(ctx, mode)
  }
}

internal class Mapped<T, R, E>(
  val inner: Parser<T, E>,
  private val transform: (T) -> R,
) : Parser<R, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<R> =
    inner.parse(ctx, mode).emit(mode) { transform(it) }
}

internal class Sliced<E>(val inner: Parser<*, E>) : Parser<Slice, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Slice> =
    parseSlice(ctx, mode)

  private fun parseSlice(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Slice> {
    val start = ctx.pos
    return inner
      .parse(ctx, Mode.Check)
      .emit(mode) { Slice(ctx.input, start, ctx.pos) }
  }
}

internal class WithSlice<T, E>(val inner: Parser<T, E>) :
  Parser<Seq<T, Slice>, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Seq<T, Slice>> =
    parseWithSlice(ctx, mode)

  private fun parseWithSlice(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Seq<T, Slice>> {
    val start = ctx.pos
    return inner
      .parse(ctx, mode)
      .emit(mode) { Seq(it, Slice(ctx.input, start, ctx.pos)) }
  }
}

private inline fun <T, R> ParseResult<T>.emit(
  mode: Mode,
  transform: (T) -> R,
): ParseResult<R> =
  when {
    isFailure -> ParseResult.failure
    mode == Mode.Check -> ParseResult.omitted
    else -> ParseResult.success(transform(value))
  }

private operator fun <A> Seq<A, *>.component1(): A =
  first

private operator fun <A, B> Seq<A, B>.component2(): B =
  second

private inline fun <T1, T2, T3, R> unpack3(
  values: Seq<Seq<T1, T2>, T3>,
  transform: (T1, T2, T3) -> R,
): R {
  val [ab, c] = values
  return transform(ab.first, ab.second, c)
}

private inline fun <T1, T2, T3, T4, R> unpack4(
  values: Seq<Seq<Seq<T1, T2>, T3>, T4>,
  transform: (T1, T2, T3, T4) -> R,
): R {
  val [abc, d] = values
  return unpack3(abc) { a, b, c -> transform(a, b, c, d) }
}

private inline fun <T1, T2, T3, T4, T5, R> unpack5(
  values: Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>,
  transform: (T1, T2, T3, T4, T5) -> R,
): R {
  val [abcd, e] = values
  return unpack4(abcd) { a, b, c, d -> transform(a, b, c, d, e) }
}

private inline fun <T1, T2, T3, T4, T5, T6, R> unpack6(
  values: Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>,
  transform: (T1, T2, T3, T4, T5, T6) -> R,
): R {
  val [abcde, f] = values
  return unpack5(abcde) { a, b, c, d, e -> transform(a, b, c, d, e, f) }
}

private inline fun <T1, T2, T3, T4, T5, T6, T7, R> unpack7(
  values: Seq<Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>, T7>,
  transform: (T1, T2, T3, T4, T5, T6, T7) -> R,
): R {
  val [abcdef, g] = values
  return unpack6(abcdef) { a, b, c, d, e, f -> transform(a, b, c, d, e, f, g) }
}

private inline fun <T1, T2, T3, T4, T5, T6, T7, T8, R> unpack8(
  values: Seq<Seq<Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>, T7>, T8>,
  transform: (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): R {
  val [abcdefg, h] = values
  return unpack7(abcdefg) { a, b, c, d, e, f, g ->
    transform(a, b, c, d, e, f, g, h)
  }
}

private inline fun <T1, T2, T3, T4, T5, T6, T7, T8, T9, R> unpack9(
  values:
    Seq<Seq<Seq<Seq<Seq<Seq<Seq<Seq<T1, T2>, T3>, T4>, T5>, T6>, T7>, T8>, T9>,
  transform: (T1, T2, T3, T4, T5, T6, T7, T8, T9) -> R,
): R {
  val [abcdefgh, i] = values
  return unpack8(abcdefgh) { a, b, c, d, e, f, g, h ->
    transform(a, b, c, d, e, f, g, h, i)
  }
}

// endregion
