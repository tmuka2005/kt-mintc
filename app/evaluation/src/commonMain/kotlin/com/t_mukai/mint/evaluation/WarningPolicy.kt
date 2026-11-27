package com.t_mukai.mint.evaluation

import com.t_mukai.mint.syntax.IdentifierWarning
import com.t_mukai.mint.syntax.NumberWarning

public fun interface NumberWarningPolicy {
  public fun isError(
    warning: NumberWarning,
  ): Boolean
}

public fun interface IdentifierWarningPolicy {
  public fun isError(
    warning: IdentifierWarning,
  ): Boolean
}
