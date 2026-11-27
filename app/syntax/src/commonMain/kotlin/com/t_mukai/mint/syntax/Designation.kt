package com.t_mukai.mint.syntax

import com.t_mukai.mint.parser.Slice

public sealed value class Designation {
  public abstract val text: Slice

  public value class Name(public val identifier: Expression.Identifier) :
    Designation() {
    override val text: Slice
      get() = identifier.text
  }

  public value class Invalid(override val text: Slice) : Designation()
}
