package com.t_mukai.mint.syntax

public sealed value class NumberWarning {
  public value object NonAsciiChar : NumberWarning()

  public value object UpperHexPrefix : NumberWarning()

  public value object LowerHexDigit : NumberWarning()

  public value object MissingHexDigits : NumberWarning()

  public value object DecoratedChar : NumberWarning()
}
