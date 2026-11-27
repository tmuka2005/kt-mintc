package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Slice

public sealed value class Expression {
  public abstract val text: Slice

  public sealed value class Primitive : Expression() {
    public sealed value class Number : Primitive() {
      public abstract val warnings: List<NumberWarning>

      public value class IntDec internal constructor(
        override val text: Slice,
        override val warnings: List<NumberWarning>,
      ) : Number()

      public value class FloatDec internal constructor(
        override val text: Slice,
        override val warnings: List<NumberWarning>,
      ) : Number()

      public value class IntHex internal constructor(
        override val text: Slice,
        override val warnings: List<NumberWarning>,
      ) : Number()
    }
  }
}
