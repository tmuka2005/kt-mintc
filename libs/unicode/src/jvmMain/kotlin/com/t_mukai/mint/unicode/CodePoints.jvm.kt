package com.t_mukai.mint.unicode

import kotlin.text.unicode.CodePoint

public actual fun CodePoint.digitValue(
  radix: Int,
): Int =
  Character.digit(code, radix)
