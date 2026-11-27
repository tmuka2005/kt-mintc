package com.t_mukai.mint.evaluation

internal expect fun Long.plusExact(
  other: Long,
): Long?

internal expect fun Long.minusExact(
  other: Long,
): Long?

internal expect fun Long.timesExact(
  other: Long,
): Long?

internal expect fun Long.divExact(
  other: Long,
): Long?
