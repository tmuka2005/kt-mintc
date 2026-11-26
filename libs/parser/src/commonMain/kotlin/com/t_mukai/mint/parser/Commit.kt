package com.t_mukai.mint.parser

import arrow.core.None
import arrow.core.Option
import arrow.core.Some

public fun <E> Parser<*, E>.commit(): Committed<E> =
  Committed(this, emptyList())

public fun <B, E> Committed<E>.then(
  next: Parser<B, E>,
): CommittedSeq<Option<B>, E> =
  CommittedSeq(head, steps + Step(next, keep = true))

public fun <E> Committed<E>.thenIgnore(
  next: Parser<*, E>,
): Committed<E> =
  Committed(head, steps + Step(next, keep = false))

public fun <T, B, E> CommittedSeq<T, E>.then(
  next: Parser<B, E>,
): CommittedSeq<Seq<T, Option<B>>, E> =
  CommittedSeq(head, steps + Step(next, keep = true))

public fun <T, E> CommittedSeq<T, E>.thenIgnore(
  next: Parser<*, E>,
): CommittedSeq<T, E> =
  CommittedSeq(head, steps + Step(next, keep = false))

public class Committed<out E> internal constructor(
  internal val head: Parser<*, E>,
  internal val steps: List<Step<E>>,
) : Parser<Unit, E> {
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Unit> =
    parseUnit(ctx, mode)

  // region internal

  private fun parseUnit(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<Unit> =
    if (parseCommitted(ctx, mode, head, steps).isFailure) {
      ParseResult.failure
    } else {
      ParseResult.unit
    }

  // endregion
}

public class CommittedSeq<out T, out E> internal constructor(
  internal val head: Parser<*, E>,
  internal val steps: List<Step<E>>,
) : Parser<T, E> {
  @Suppress("UNCHECKED_CAST")
  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseCommitted(ctx, mode, head, steps) as ParseResult<T>
}

// region internal

internal class Step<out E>(
  val parser: Parser<*, E>,
  val keep: Boolean,
)

private val present: Option<Unit> = Some(Unit)

private fun <E> parseCommitted(
  ctx: Context<in E>,
  mode: Mode,
  head: Parser<*, E>,
  steps: List<Step<E>>,
): ParseResult<Any?> {
  if (head.parse(ctx, Mode.Check).isFailure) {
    return ParseResult.failure
  }
  val values = collectSteps(ctx, mode, steps)
  return if (mode == Mode.Check) {
    ParseResult.omitted
  } else {
    ParseResult.success(values)
  }
}

private fun <E> collectSteps(
  ctx: Context<in E>,
  mode: Mode,
  steps: List<Step<E>>,
): Any? {
  var values: Any? = null
  var hasValue = false
  for (i in steps.indices) {
    val keep = steps[i].keep
    val stepMode =
      if (keep) {
        mode
      } else {
        Mode.Check
      }
    val value = expect(ctx, stepMode, steps, i)
    if (keep && mode == Mode.Emit) {
      values =
        if (hasValue) {
          Seq(values, value)
        } else {
          value
        }
      hasValue = true
    }
  }
  return values
}

private fun <E> expect(
  ctx: Context<in E>,
  mode: Mode,
  steps: List<Step<E>>,
  index: Int,
): Option<Any?> {
  val parser = steps[index].parser
  ctx.forgetFailures()
  var failure: Recorded.Unexpected? = null
  while (true) {
    val start = ctx.mark()
    val result = parser.parse(ctx, mode)
    if (result.isSuccess) {
      failure?.let {
        ctx.unexpected(it.endingAt(start.pos), at = start.diagnosticCount)
      }
      return if (mode == Mode.Check) {
        present
      } else {
        Some(result.value)
      }
    }
    ctx.rewind(start)
    if (failure == null) {
      failure = ctx.furthestFailure()
        ?: Recorded.Unexpected(start.pos, start.pos, emptySet())
    }
    if (shouldGiveUp(ctx, steps, index) || !ctx.skipOneUnit()) {
      ctx.unexpected(failure.endingAt(ctx.pos))
      ctx.forgetFailures()
      return None
    }
  }
}

private fun Recorded.Unexpected.endingAt(
  pos: Int,
): Recorded.Unexpected =
  Recorded.Unexpected(
    start = start,
    end = maxOf(start, pos),
    expected = expected,
  )

private fun <E> shouldGiveUp(
  ctx: Context<in E>,
  steps: List<Step<E>>,
  index: Int,
): Boolean =
  ctx.isAtEnd ||
    ctx.isAtCloser() ||
    ctx.isAtTerminator() ||
    (index + 1..<steps.size).any { ctx.matches(steps[it].parser) }

// endregion
