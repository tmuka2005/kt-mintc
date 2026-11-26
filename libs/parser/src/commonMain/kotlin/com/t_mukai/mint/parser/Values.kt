package com.t_mukai.mint.parser

import arrow.core.Option
import com.t_mukai.mint.unicode.Location

public value class Seq<out A, out B>(
  public val first: A,
  public val second: B,
)

public value class Slice(
  public val source: CharArray,
  public val start: Int,
  public val end: Int,
) : CharSequence {
  override val length: Int
    get() = end - start

  override fun get(
    index: Int,
  ): Char =
    checkedCharAt(index)

  override fun subSequence(
    startIndex: Int,
    endIndex: Int,
  ): Slice =
    slice(startIndex, endIndex)

  override fun equals(
    other: Any?,
  ): Boolean =
    other is Slice && hasSameChars(other)

  override fun hashCode(): Int =
    hashChars()

  override fun toString(): String =
    source.concatToString(start, end)
}

public sealed value class Expected {
  public value class Char(public val c: kotlin.Char) : Expected()

  public value class Range(public val range: CharRange) : Expected()

  public value class Literal(public val text: String) : Expected()

  public value class Label(public val name: String) : Expected()

  public value object AnyChar : Expected()

  public value object EndOfInput : Expected()
}

public sealed value class Diagnostic<out E> {
  public abstract val start: Location
  public abstract val end: Location

  init {
    require(start <= end) { "start must be less than or equal to end" }
  }

  public value class Unexpected(
    override val start: Location,
    override val end: Location,
    public val expected: Set<Expected>,
    public val found: String?,
  ) : Diagnostic<Nothing>()

  public value class Reported<out E>(
    override val start: Location,
    override val end: Location,
    public val kind: E,
  ) : Diagnostic<E>()
}

public value class Output<out T, out E> internal constructor(
  public val value: Option<T>,
  public val diagnostics: List<Diagnostic<E>>,
)

// region internal

internal sealed value class Recorded<out E> {
  abstract val start: Int
  abstract val end: Int

  init {
    require(start <= end) { "start must be less than or equal to end" }
  }

  value class Unexpected(
    override val start: Int,
    override val end: Int,
    val expected: Set<Expected>,
  ) : Recorded<Nothing>()

  value class Reported<out E>(
    override val start: Int,
    override val end: Int,
    val kind: E,
  ) : Recorded<E>()
}

private fun Slice.checkedCharAt(
  index: Int,
): Char {
  if (index !in indices) {
    throw IndexOutOfBoundsException("$index")
  }
  return source[start + index]
}

private fun Slice.slice(
  startIndex: Int,
  endIndex: Int,
): Slice {
  if (startIndex !in 0..endIndex || endIndex > length) {
    throw IndexOutOfBoundsException("$startIndex..<$endIndex")
  }
  return Slice(source, start + startIndex, start + endIndex)
}

private fun Slice.hasSameChars(
  other: Slice,
): Boolean =
  other.length == length &&
    indices.all { source[start + it] == other.source[other.start + it] }

private fun Slice.hashChars(): Int {
  var hash = 0
  for (i in start..<end) {
    hash = HASH_MULTIPLIER * hash + source[i].code
  }
  return hash
}

private const val HASH_MULTIPLIER = 31

// endregion
