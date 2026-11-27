package com.t_mukai.mint.benchmark

internal sealed interface Node {
  data class Num(val value: Long) : Node

  data class Bin(
    val operator: Char,
    val lhs: Node,
    val rhs: Node,
  ) : Node
}
