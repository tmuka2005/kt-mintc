package com.t_mukai.mint.parser

import arrow.core.None
import arrow.core.Some

public sealed interface Parser<out T, out E> {
  public fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T>
}

public fun <T, E : Any> Parser<T, E>.parse(
  input: String,
  onDecorated: E? = null,
): Output<T, E> =
  parseAll(Context(input.toCharArray(), onDecorated))

public fun <T> custom(
  block: Context<in Nothing>.(Mode) -> ParseResult<T>,
): Parser<T, Nothing> =
  Custom(block)

public fun <T, E> customReporting(
  block: Context<in E>.(Mode) -> ParseResult<T>,
): Parser<T, E> =
  Custom(block)

// region internal

internal fun <E> List<Recorded<E>>.merged(): List<Recorded<E>> =
  mergeAdjacent(this)

internal class Custom<T, E>(
  private val block: Context<in E>.(Mode) -> ParseResult<T>,
) : Parser<T, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseBlock(ctx, mode)

  private fun parseBlock(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    val start = ctx.mark()
    val result = ctx.block(mode)
    if (result.isFailure) {
      ctx.rewind(start)
    }
    return result
  }
}

private fun <T, E> Parser<T, E>.parseAll(
  ctx: Context<E>,
): Output<T, E> {
  val result = parse(ctx, Mode.Emit)
  if (result.isSuccess && ctx.isAtEnd) {
    return Output(Some(result.value), ctx.locatedDiagnostics())
  }
  if (result.isSuccess) {
    ctx.record(ctx.pos, Expected.EndOfInput)
  }
  ctx.furthestFailure()?.let { ctx.unexpected(it) }
  return Output(None, ctx.locatedDiagnostics())
}

private fun <E> Context<E>.locatedDiagnostics(): List<Diagnostic<E>> =
  diagnostics.merged().map { it.locatedIn(this) }

private fun <E> Recorded<E>.locatedIn(
  ctx: Context<E>,
): Diagnostic<E> {
  val start = ctx.clusters.locationOf(start)
  val end = ctx.clusters.locationOf(end)
  return when (this) {
    is Recorded.Unexpected -> {
      Diagnostic.Unexpected(start, end, expected, ctx.foundAt(this.start))
    }

    is Recorded.Reported -> {
      Diagnostic.Reported(start, end, kind)
    }
  }
}

private fun Context<*>.foundAt(
  index: Int,
): String? =
  if (index < input.size) {
    clusters.clusterAt(index).toString()
  } else {
    null
  }

private fun <E> mergeAdjacent(
  recorded: List<Recorded<E>>,
): List<Recorded<E>> {
  val merged = ArrayList<Recorded<E>>(recorded.size)
  val openEnds = HashMap<Recorded<E>, Int>()
  for (next in recorded) {
    if (next.start == next.end) {
      merged += next
      continue
    }
    val index = openEnds.remove(next.pointAt(next.start))
    if (index == null) {
      openEnds[next.pointAt(next.end)] = merged.size
      merged += next
    } else {
      merged[index] = merged[index].spanning(merged[index].start, next.end)
      openEnds[next.pointAt(next.end)] = index
    }
  }
  return merged
}

private fun <E> Recorded<E>.pointAt(
  pos: Int,
): Recorded<E> =
  spanning(pos, pos)

private fun <E> Recorded<E>.spanning(
  start: Int,
  end: Int,
): Recorded<E> =
  when (this) {
    is Recorded.Unexpected -> {
      Recorded.Unexpected(
        start = start,
        end = end,
        expected = expected,
      )
    }

    is Recorded.Reported -> {
      Recorded.Reported(
        start = start,
        end = end,
        kind = kind,
      )
    }
  }

// endregion
