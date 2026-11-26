package com.t_mukai.mint.unicode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlin.text.unicode.toCodePoint

@Suppress("unused")
class CodePointsTest :
  FunSpec({
    test("digitValue reads ASCII digits") {
      '7'.toCodePoint().digitValue(10) shouldBe 7
    }

    test("digitValue reads fullwidth and other decimal digits") {
      '７'.toCodePoint().digitValue(10) shouldBe 7
      '٣'.toCodePoint().digitValue(10) shouldBe 3
    }

    test("digitValue reads hex letters including fullwidth ones") {
      'f'.toCodePoint().digitValue(16) shouldBe 15
      'Ｂ'.toCodePoint().digitValue(16) shouldBe 11
    }

    test("digitValue is negative outside the radix") {
      'a'.toCodePoint().digitValue(10) shouldBe -1
      '.'.toCodePoint().digitValue(10) shouldBe -1
    }
  })
