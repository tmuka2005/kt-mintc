package com.t_mukai.mint.evaluation

public class Environment {
  private val values = HashMap<String, Value>()

  public operator fun get(
    name: String,
  ): Value? =
    values[name]

  public operator fun set(
    name: String,
    value: Value,
  ) {
    values[name] = value
  }
}

public fun replResultName(
  index: Int,
): String =
  $$"$$$index"
