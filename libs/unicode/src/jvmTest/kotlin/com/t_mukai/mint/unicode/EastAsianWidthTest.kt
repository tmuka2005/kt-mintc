package com.t_mukai.mint.unicode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

@Suppress("unused")
class EastAsianWidthTest :
  FunSpec({
    val properties = loadProperties("EastAsianWidth.txt")

    test("isEastAsianWide matches W and F") {
      val wide = properties.getValue("W")
      val fullwidth = properties.getValue("F")
      val members =
        BooleanArray(CODE_POINT_LIMIT) { wide[it] || fullwidth[it] }
      mismatches(members) { isEastAsianWide(it) }.shouldBeEmpty()
    }
  })
