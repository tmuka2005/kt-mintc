package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.BaseMatch
import com.t_mukai.mint.unicode.CharCluster
import com.t_mukai.mint.unicode.CharClusters
import kotlin.text.unicode.CodePoint
import kotlin.text.unicode.codePointAt
import kotlin.text.unicode.toCodePoint

public class Context<E> internal constructor(internal val input: CharArray) {
  public var pos: Int = 0
    private set

  internal val diagnostics: MutableList<Recorded<E>> = []

  private var furthestPos: Int = -1

  private val furthestExpected: MutableSet<Expected> = HashSet()

  internal var insideLabel: Boolean = false

  private var silence: Int = 0

  private val text = Slice(input, 0, input.size)

  internal val clusters = CharClusters(text)

  public fun peek(): CharCluster? =
    clusterAtPos()

  public fun advance() {
    check(!isAtEnd) { "Cannot advance past the end of input" }
    advanceCluster()
  }

  public fun <T> run(
    parser: Parser<T, E>,
    mode: Mode,
  ): ParseResult<T> =
    runRewinding(parser, mode)

  public fun error(
    expected: Expected,
  ) {
    record(pos, expected)
  }

  // region internal

  internal val isAtEnd: Boolean
    get() = pos >= input.size

  internal val current: Char
    get() = baseCharAtPos()

  internal fun unexpected(
    failure: Recorded.Unexpected,
  ) {
    diagnostics += failure
  }

  internal fun mark(): Mark =
    Mark(pos, diagnostics.size)

  internal fun rewind(
    to: Mark,
  ) {
    rewindTo(to)
  }

  internal fun advanceCluster() {
    pos = clusters.endOf(pos)
  }

  internal fun matchBase(
    predicate: CodePointPredicate,
  ): BaseMatch =
    matchBaseWith { predicate.test(it) }

  internal fun matchBase(
    codePoint: CodePoint,
  ): BaseMatch =
    matchBaseWith { it == codePoint }

  internal fun accept(
    match: BaseMatch,
  ): Boolean =
    acceptMatch(match)

  internal fun record(
    at: Int,
    expected: Expected,
  ) {
    if (reaches(at)) {
      furthestExpected += expected
    }
  }

  internal fun record(
    at: Int,
    expected: Set<Expected>,
  ) {
    if (expected.isNotEmpty() && reaches(at)) {
      furthestExpected += expected
    }
  }

  internal fun matches(
    parser: Parser<*, E>,
  ): Boolean =
    lookAhead(parser)

  internal fun furthestFailure(): Recorded.Unexpected? =
    furthestPos.takeIf { it >= 0 }?.let {
      Recorded.Unexpected(it, it, furthestExpected.toSet())
    }

  private fun clusterAtPos(): CharCluster? =
    if (isAtEnd) {
      null
    } else {
      clusters.clusterAt(pos)
    }

  private fun lookAhead(
    parser: Parser<*, E>,
  ): Boolean {
    val start = pos
    val matched = quietly { parser.parse(this, Mode.Check).isSuccess }
    pos = start
    return matched
  }

  private fun baseCharAtPos(): Char {
    val c = input[pos]
    val baseIndex =
      if (c.code < ASCII_SIZE) {
        -1
      } else {
        clusters.baseIndexAt(pos)
      }
    return input.getOrElse(baseIndex) { c }
  }

  private fun <T> runRewinding(
    parser: Parser<T, E>,
    mode: Mode,
  ): ParseResult<T> {
    val start = mark()
    val result = parser.parse(this, mode)
    if (result.isFailure) {
      rewind(start)
    }
    return result
  }

  private fun rewindTo(
    to: Mark,
  ) {
    pos = to.pos
    if (diagnostics.size > to.diagnosticCount) {
      diagnostics.subList(to.diagnosticCount, diagnostics.size).clear()
    }
  }

  private fun acceptMatch(
    match: BaseMatch,
  ): Boolean =
    when (match) {
      BaseMatch.Plain, BaseMatch.Decorated -> {
        advanceCluster()
        true
      }

      BaseMatch.Mismatch -> {
        false
      }
    }

  private inline fun matchBaseWith(
    test: (CodePoint) -> Boolean,
  ): BaseMatch {
    val head = input.getOrNull(pos)
    return when {
      head == null -> BaseMatch.Mismatch
      head.code < ASCII_SIZE -> matchAsciiBase(head, test)
      else -> matchClusterBase(test)
    }
  }

  private inline fun matchAsciiBase(
    head: Char,
    test: (CodePoint) -> Boolean,
  ): BaseMatch =
    when {
      !test(head.toCodePoint()) -> BaseMatch.Mismatch
      clusters.isBoundary(pos + 1) -> BaseMatch.Plain
      else -> BaseMatch.Decorated
    }

  private inline fun matchClusterBase(
    test: (CodePoint) -> Boolean,
  ): BaseMatch {
    val baseIndex = clusters.baseIndexAt(pos)
    val base = baseIndex.takeIf { it >= 0 }?.let { text.codePointAt(it) }
    return when {
      base == null || !test(base) -> {
        BaseMatch.Mismatch
      }

      baseIndex == pos && baseIndex + base.size == clusters.endOf(pos) -> {
        BaseMatch.Plain
      }

      else -> {
        BaseMatch.Decorated
      }
    }
  }

  private inline fun <R> quietly(
    block: () -> R,
  ): R {
    silence++
    try {
      return block()
    } finally {
      silence--
    }
  }

  private fun reaches(
    at: Int,
  ): Boolean {
    if (silence > 0 || insideLabel || at < furthestPos) {
      return false
    }
    if (at > furthestPos) {
      furthestPos = at
      furthestExpected.clear()
    }
    return true
  }

  // endregion
}

// region internal

internal const val ASCII_SIZE = 128

// endregion
