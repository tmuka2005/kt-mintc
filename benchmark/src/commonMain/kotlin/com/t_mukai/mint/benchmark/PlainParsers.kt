package com.t_mukai.mint.benchmark

internal abstract class PlainParser {
  abstract fun parse(
    scanner: Scanner,
  ): Int
}

internal fun plainExpression(): PlainParser =
  PlainGroup().inner

// region internal

private class PlainDigits : PlainParser() {
  override fun parse(
    scanner: Scanner,
  ): Int =
    scanner.readDigits()
}

private class PlainChain(
  private val operand: PlainParser,
  private val operators: String,
) : PlainParser() {
  override fun parse(
    scanner: Scanner,
  ): Int {
    var value = operand.parse(scanner)
    if (value == FAIL) {
      return FAIL
    }
    while (scanner.current in operators) {
      val mark = scanner.pos
      val operator = scanner.current
      scanner.advance()
      val rhs = operand.parse(scanner)
      if (rhs == FAIL) {
        scanner.pos = mark
        break
      }
      value = apply(operator, value, rhs)
    }
    return value
  }
}

private class PlainChoice(
  private val first: PlainParser,
  private val second: PlainParser,
) : PlainParser() {
  override fun parse(
    scanner: Scanner,
  ): Int {
    val mark = scanner.pos
    val value = first.parse(scanner)
    if (value != FAIL) {
      return value
    }
    scanner.pos = mark
    return second.parse(scanner)
  }
}

private class PlainGroup : PlainParser() {
  val inner: PlainParser =
    PlainChain(PlainChain(PlainChoice(PlainDigits(), this), "*"), "+-")

  override fun parse(
    scanner: Scanner,
  ): Int {
    if (scanner.current != '(') {
      return FAIL
    }
    val mark = scanner.pos
    scanner.advance()
    val value = inner.parse(scanner)
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
