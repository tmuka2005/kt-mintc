package com.t_mukai.mint.parser

import kotlin.jvm.JvmInline

public enum class Mode { Emit, Check }

@JvmInline
public value class ParseResult<out T>
  @PublishedApi
  internal constructor(
    private val raw: Any?,
  ) {
    public val isFailure: Boolean
      get() = raw === Failure

    public val isSuccess: Boolean
      get() = raw !== Failure

    @Suppress("UNCHECKED_CAST")
    public val value: T
      get() = raw as T

    companion {
      public val failure: ParseResult<Nothing> = ParseResult(Failure)

      public val unit: ParseResult<Unit> = ParseResult(Unit)

      internal val omitted: ParseResult<Nothing> = ParseResult(Unit)

      public fun <T> success(
        value: T,
      ): ParseResult<T> =
        ParseResult(value)
    }
  }

// region internal

@JvmInline
internal value class Mark private constructor(private val packed: Long) {
  constructor(
    pos: Int,
    diagnosticCount: Int,
  ) : this(
    (pos.toLong() shl Int.SIZE_BITS) or diagnosticCount.toUInt().toLong(),
  )

  val pos: Int
    get() = (packed ushr Int.SIZE_BITS).toInt()

  val diagnosticCount: Int
    get() = packed.toInt()
}

private object Failure

// endregion
