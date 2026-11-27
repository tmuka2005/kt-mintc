package com.t_mukai.mint.benchmark

import com.t_mukai.mint.parser.Parser
import com.t_mukai.mint.parser.char
import com.t_mukai.mint.parser.digit
import com.t_mukai.mint.parser.infixLeft
import com.t_mukai.mint.parser.many
import com.t_mukai.mint.parser.map
import com.t_mukai.mint.parser.parse
import com.t_mukai.mint.parser.pratt
import com.t_mukai.mint.parser.slice
import kotlinx.benchmark.Benchmark
import kotlinx.benchmark.BenchmarkMode
import kotlinx.benchmark.BenchmarkTimeUnit
import kotlinx.benchmark.Measurement
import kotlinx.benchmark.Mode
import kotlinx.benchmark.OutputTimeUnit
import kotlinx.benchmark.Param
import kotlinx.benchmark.Scope
import kotlinx.benchmark.Setup
import kotlinx.benchmark.State
import kotlinx.benchmark.Warmup

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(BenchmarkTimeUnit.MICROSECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
public class ClusterBenchmark {
  @Param("ascii", "fullwidth", "decorated")
  public var digits: String = "ascii"

  private var input: String = ""

  @Setup
  public fun setUp() {
    input = clusterInput(digits)
    check(parse() != null)
  }

  @Benchmark
  public fun parse(): Any? =
    digitExpression
      .parse(input)
      .value
      .getOrNull()
}

// region internal

private val digitNumber: Parser<Node, Nothing> =
  many(digit())
    .atLeast(1)
    .slice()
    .map { Node.Num(it.length.toLong()) }

private val digitExpression: Parser<Node, Nothing> =
  pratt(
    digitNumber,
    [
      [digitBinary('*')],
      [digitBinary('+'), digitBinary('-')],
    ],
  )

private fun clusterInput(
  digits: String,
): String {
  val ascii = benchmarkInput("flat")
  return when (digits) {
    "ascii" -> ascii
    "fullwidth" -> ascii.map(::toFullwidth).joinToString("")
    else -> ascii.map(::decorate).joinToString("")
  }
}

private fun toFullwidth(
  c: Char,
): String =
  if (c in '0'..'9') {
    (c - '0' + '０'.code).toChar().toString()
  } else {
    c.toString()
  }

private fun decorate(
  c: Char,
): String =
  if (c in '0'..'9') {
    "$ć"
  } else {
    c.toString()
  }

private fun digitBinary(
  operator: Char,
) =
  infixLeft<Node, Unit, Nothing>(char(operator)) { lhs, _, rhs ->
    Node.Bin(operator, lhs, rhs)
  }

// endregion
