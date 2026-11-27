package com.t_mukai.mint.benchmark

internal const val FAIL = Int.MIN_VALUE

internal class Scanner(private val input: String) {
  var pos: Int = 0

  val current: Char
    get() =
      if (pos < input.length) {
        input[pos]
      } else {
        Char.MIN_VALUE
      }

  fun advance() {
    pos++
  }
}

internal fun Scanner.readDigits(): Int =
  digitsValue(this)

internal fun apply(
  operator: Char,
  lhs: Int,
  rhs: Int,
): Int =
  when (operator) {
    '+' -> lhs + rhs
    '-' -> lhs - rhs
    else -> lhs * rhs
  } and VALUE_MASK

// region internal

private fun digitsValue(
  scanner: Scanner,
): Int {
  if (scanner.current !in '0'..'9') {
    return FAIL
  }
  var value = 0
  while (scanner.current in '0'..'9') {
    value = value * RADIX + (scanner.current - '0') and VALUE_MASK
    scanner.advance()
  }
  return value
}

private const val VALUE_MASK = 0x3FFF_FFFF

private const val RADIX = 10

// endregion
