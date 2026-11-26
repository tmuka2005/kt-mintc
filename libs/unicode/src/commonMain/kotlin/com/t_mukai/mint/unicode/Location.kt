package com.t_mukai.mint.unicode

import kotlin.jvm.JvmInline

@JvmInline
public value class Location private constructor(private val packed: Long) {
  public constructor(row: Int, col: Int) : this(
    (row.toLong() shl Int.SIZE_BITS) or (col.toLong() and 0xFFFF_FFFFL),
  )

  public val row: Int
    get() = (packed ushr Int.SIZE_BITS).toInt()

  public val col: Int
    get() = packed.toInt()

  public operator fun compareTo(
    other: Location,
  ): Int =
    locationComparison(other)

  override fun toString(): String =
    "Location(row=$row, col=$col)"
}

// region internal

private fun Location.locationComparison(
  other: Location,
): Int {
  val rowComparison = row.compareTo(other.row)
  return if (rowComparison != 0) {
    rowComparison
  } else {
    col.compareTo(other.col)
  }
}

// endregion
