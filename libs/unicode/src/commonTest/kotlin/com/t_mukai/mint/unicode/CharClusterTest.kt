package com.t_mukai.mint.unicode

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.text.unicode.toCodePoint

@Suppress("unused")
class CharClusterTest :
  FunSpec({
    fun clusters(
      text: String,
    ): List<CharCluster> {
      val clusters = CharClusters(text)
      return generateSequence(0) { clusters.endOf(it) }
        .takeWhile { it < text.length }
        .map { clusters.clusterAt(it) }
        .toList()
    }

    test("splits text into grapheme clusters") {
      clusters("a\"\u0301\r\n\uD83D\uDC4D\uD83C\uDFFD")
        .map { it.toString() } shouldBe
        ["a", "\"\u0301", "\r\n", "\uD83D\uDC4D\uD83C\uDFFD"]
    }

    test("codePoints combines surrogate pairs") {
      clusters("\uD83D\uDC4D\uD83C\uDFFD")
        .single()
        .codePoints()
        .toList() shouldBe [0x1F44D, 0x1F3FD]
    }

    test("equal contents are equal clusters") {
      val split = clusters("e\u0301-e\u0301")
      split[0] shouldBe split[2]
      split[0].hashCode() shouldBe split[2].hashCode()
      split[0] shouldNotBe clusters("e").single()
    }

    test("matchBase is Plain for the code point alone") {
      clusters("\"").single().matchBase('"'.toCodePoint()) shouldBe
        BaseMatch.Plain
    }

    test("matchBase is Decorated when marks follow the code point") {
      clusters("\"\u0301").single().matchBase('"'.toCodePoint()) shouldBe
        BaseMatch.Decorated
    }

    test("matchBase skips prepended characters") {
      clusters("\u0600\"").single().matchBase('"'.toCodePoint()) shouldBe
        BaseMatch.Decorated
    }

    test("matchBase is Mismatch for another code point") {
      clusters("'\u0301").single().matchBase('"'.toCodePoint()) shouldBe
        BaseMatch.Mismatch
    }

    test("baseIndexAt points past prepended characters") {
      val clusters = CharClusters("\u0600\"a")
      clusters.baseIndexAt(0) shouldBe 1
      clusters.baseIndexAt(2) shouldBe 2
    }

    test("baseIndexAt is negative without a base") {
      CharClusters("\u0600").baseIndexAt(0) shouldBe -1
    }
  })
