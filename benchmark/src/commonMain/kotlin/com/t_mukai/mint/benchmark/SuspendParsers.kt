package com.t_mukai.mint.benchmark

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.RestrictsSuspension
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn
import kotlin.coroutines.resume

@RestrictsSuspension
internal class ParseScope(
  val scanner: Scanner,
  private val directDepthLimit: Int,
) : Continuation<Int> {
  override val context: CoroutineContext
    get() = EmptyCoroutineContext

  private var depth = 0

  private var nextParser: SuspendParser? = null

  private var nextContinuation: Continuation<Int> = this

  private var finished = FAIL

  fun run(
    root: SuspendParser,
  ): Int =
    trampoline(root)

  override fun resumeWith(
    result: Result<Int>,
  ) {
    finished = result.getOrThrow()
  }

  // region internal

  suspend fun descend(
    parser: SuspendParser,
  ): Int {
    if (depth < directDepthLimit) {
      depth++
      val value = with(parser) { parse() }
      depth--
      return value
    }
    val saved = depth
    depth = 0
    val value =
      suspendCoroutineUninterceptedOrReturn { continuation ->
        nextParser = parser
        nextContinuation = continuation
        COROUTINE_SUSPENDED
      }
    depth = saved
    return value
  }

  private fun trampoline(
    root: SuspendParser,
  ): Int {
    nextParser = root
    nextContinuation = this
    while (true) {
      val parser = nextParser ?: return finished
      val continuation = nextContinuation
      nextParser = null
      val value =
        parser.entry.startCoroutineUninterceptedOrReturn(
          this,
          continuation,
        )
      if (value !== COROUTINE_SUSPENDED) {
        continuation.resume(value as Int)
      }
    }
  }

  // endregion
}

internal abstract class SuspendParser {
  val entry: suspend ParseScope.() -> Int = { parse() }

  abstract suspend fun ParseScope.parse(): Int
}

internal fun suspendExpression(): SuspendParser =
  SuspendGroup().inner

// region internal

private class SuspendDigits : SuspendParser() {
  override suspend fun ParseScope.parse(): Int =
    scanner.readDigits()
}

private class SuspendChain(
  private val operand: SuspendParser,
  private val operators: String,
) : SuspendParser() {
  override suspend fun ParseScope.parse(): Int {
    var value = descend(operand)
    if (value == FAIL) {
      return FAIL
    }
    while (scanner.current in operators) {
      val mark = scanner.pos
      val operator = scanner.current
      scanner.advance()
      val rhs = descend(operand)
      if (rhs == FAIL) {
        scanner.pos = mark
        break
      }
      value = apply(operator, value, rhs)
    }
    return value
  }
}

private class SuspendChoice(
  private val first: SuspendParser,
  private val second: SuspendParser,
) : SuspendParser() {
  override suspend fun ParseScope.parse(): Int {
    val mark = scanner.pos
    val value = descend(first)
    if (value != FAIL) {
      return value
    }
    scanner.pos = mark
    return descend(second)
  }
}

private class SuspendGroup : SuspendParser() {
  val inner: SuspendParser =
    SuspendChain(SuspendChain(SuspendChoice(SuspendDigits(), this), "*"), "+-")

  override suspend fun ParseScope.parse(): Int {
    if (scanner.current != '(') {
      return FAIL
    }
    val mark = scanner.pos
    scanner.advance()
    val value = descend(inner)
    return if (value == FAIL || scanner.current != ')') {
      scanner.pos = mark
      FAIL
    } else {
      scanner.advance()
      value
    }
  }
}

// endregion
