package com.t_mukai.mint.evaluation

internal actual fun Long.plusExact(
  other: Long,
): Long? =
  exactOrNull { Math.addExact(this, other) }

internal actual fun Long.minusExact(
  other: Long,
): Long? =
  exactOrNull { Math.subtractExact(this, other) }

internal actual fun Long.timesExact(
  other: Long,
): Long? =
  exactOrNull { Math.multiplyExact(this, other) }

internal actual fun Long.divExact(
  other: Long,
): Long? =
  exactOrNull { Math.divideExact(this, other) }

// region internal

private inline fun exactOrNull(
  operation: () -> Long,
): Long? =
  try {
    operation()
  } catch (_: ArithmeticException) {
    null
  }

// endregion
