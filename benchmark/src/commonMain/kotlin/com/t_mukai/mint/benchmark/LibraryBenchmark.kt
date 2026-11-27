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
public class LibraryBenchmark {
  @Param("flat", "mixed", "nested")
  public var shape: String = "flat"

  private var input: String = ""

  @Setup
  public fun setUp() {
    input = benchmarkInput(shape)
    check(parseWithBetterParse(input) == parseWithMyParser(input))
    check(parseWithParsus(input) == parseWithMyParser(input))
  }

  @Benchmark
  public fun myParser(): Any? =
    parseWithMyParser(input)

  @Benchmark
  public fun betterParse(): Any =
    parseWithBetterParse(input)

  @Benchmark
  public fun parsus(): Any =
    parseWithParsus(input)
}
