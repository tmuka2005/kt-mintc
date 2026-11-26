package com.t_mukai.mint.parser

public fun <T, E> choice(
  vararg candidates: Parser<T, E>,
  label: String? = null,
): Parser<T, E> =
  choiceOf(candidates, label)

public fun <T, E> Parser<T, E>.labeled(
  name: String,
): Parser<T, E> =
  Labeled(this, Expected.Label(name))

// region internal

internal class Choice<T, E>(val candidates: List<Parser<T, E>>) :
  Parser<T, E> {
  private var dispatch: Dispatch<T, E>? = null

  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseFirst(ctx, mode)

  private fun parseFirst(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    val dispatch = dispatch ?: Dispatch(candidates).also { dispatch = it }
    val start = ctx.mark()
    val result =
      if (ctx.isAtEnd) {
        tryEach(dispatch.atEnd, ctx, mode, start)
      } else {
        tryAt(ctx.current, dispatch, ctx, mode, start)
      }
    if (result.isFailure) {
      ctx.record(start.pos, dispatch.expected)
    }
    return result
  }

  private fun tryAt(
    c: Char,
    dispatch: Dispatch<T, E>,
    ctx: Context<in E>,
    mode: Mode,
    start: Mark,
  ): ParseResult<T> =
    if (c.code < ASCII_SIZE) {
      tryEach(dispatch.ascii[c.code], ctx, mode, start)
    } else {
      tryEachStartingWith(c, dispatch, ctx, mode, start)
    }

  private fun tryEach(
    parsers: List<Parser<T, E>>,
    ctx: Context<in E>,
    mode: Mode,
    start: Mark,
  ): ParseResult<T> {
    for (i in parsers.indices) {
      val result = parsers[i].parse(ctx, mode)
      if (result.isSuccess) {
        return result
      }
      ctx.rewind(start)
    }
    return ParseResult.failure
  }

  private fun tryEachStartingWith(
    c: Char,
    dispatch: Dispatch<T, E>,
    ctx: Context<in E>,
    mode: Mode,
    start: Mark,
  ): ParseResult<T> {
    val parsers = dispatch.nonAscii
    val starts = dispatch.nonAsciiStarts
    for (i in parsers.indices) {
      if (!starts[i].mayStartWith(c)) {
        continue
      }
      val result = parsers[i].parse(ctx, mode)
      if (result.isSuccess) {
        return result
      }
      ctx.rewind(start)
    }
    return ParseResult.failure
  }
}

internal class Labeled<T, E>(
  val inner: Parser<T, E>,
  val label: Expected.Label,
) : Parser<T, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseLabeled(ctx, mode)

  private fun parseLabeled(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    val start = ctx.pos
    val outer = ctx.insideLabel
    ctx.insideLabel = true
    val result = inner.parse(ctx, mode)
    ctx.insideLabel = outer
    if (result.isFailure) {
      ctx.record(start, label)
    }
    return result
  }
}

private fun <T, E> choiceOf(
  candidates: Array<out Parser<T, E>>,
  label: String?,
): Parser<T, E> {
  val choice = Choice(candidates.toList())
  return if (label == null) {
    choice
  } else {
    choice.labeled(label)
  }
}

private class Dispatch<T, E>(candidates: List<Parser<T, E>>) {
  private val starts = candidates.map { it.start() }

  val ascii: List<List<Parser<T, E>>> =
    List(ASCII_SIZE) { code ->
      candidates.filterIndexed { i, _ ->
        starts[i].mayStartWith(code.toChar())
      }
    }

  val atEnd: List<Parser<T, E>> =
    candidates.filterIndexed { i, _ -> starts[i].mayMatchAtEnd }

  private val nonAsciiIndices =
    candidates.indices.filter { starts[it].mayStartWithNonAscii }

  val nonAscii: List<Parser<T, E>> = nonAsciiIndices.map { candidates[it] }

  val nonAsciiStarts: List<Start> = nonAsciiIndices.map { starts[it] }

  val expected: Set<Expected> = starts.flatMapTo(HashSet()) { it.expected }
}

// endregion
