package com.t_mukai.mint.evaluation

public sealed value class Value {
  public value class Int(public val value: Long) : Value()

  public value class Float(public val value: Double) : Value()

  public value object Unit : Value()
}
