package com.t_mukai.mint.benchmark

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
public class ParserBenchmark {
  @Param("flat", "mixed", "nested")
  public var shape: String = "flat"

  private var input: String = ""

  private val plain = plainExpression()

  private val suspended = suspendExpression()

  @Setup
  public fun setUp() {
    input = benchmarkInput(shape)
    check(plain() == alwaysBounce())
    check(plain() == hybrid())
  }

  @Benchmark
  public fun plain(): Int =
    plain.parse(Scanner(input))

  @Benchmark
  public fun alwaysBounce(): Int =
    ParseScope(Scanner(input), 0).run(suspended)

  @Benchmark
  public fun hybrid(): Int =
    ParseScope(Scanner(input), HYBRID_DEPTH).run(suspended)
}

// region internal

private const val HYBRID_DEPTH = 256

// endregion
