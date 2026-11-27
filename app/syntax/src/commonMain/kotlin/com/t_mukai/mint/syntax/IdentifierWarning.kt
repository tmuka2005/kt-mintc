package com.t_mukai.mint.syntax

public sealed value class IdentifierWarning {
  public value object NonAsciiChar : IdentifierWarning()

  public value object DecoratedChar : IdentifierWarning()

  public value object InvisibleLetter : IdentifierWarning()
}
