package com.t_mukai.mint.benchmark

internal fun benchmarkInput(
  shape: String,
): String =
  when (shape) {
    "flat" -> (1..TERMS).joinToString("+") { "$it*3" }
    "mixed" -> (1..TERMS).joinToString("+") { "($it-1)*(2+$it)" }
    else -> "(".repeat(NESTING) + "1" + ")".repeat(NESTING)
  }

// region internal

private const val TERMS = 1_000

private const val NESTING = 1_000

// endregion
