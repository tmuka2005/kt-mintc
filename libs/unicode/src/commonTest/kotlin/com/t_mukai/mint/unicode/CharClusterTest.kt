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

    test("displayWidth follows the base character") {
      mapOf(
        "a" to 1,
        "\u3042" to 2,
        "e\u0301" to 1,
        "\u03B1" to 1,
        "\u1100\u1161\u11A8" to 2,
        "\uD83D\uDC68\u200D\uD83D\uDC69" to 2,
        "\u200B" to 0,
      ).forEach { (key, value) ->
        clusters(key).single().displayWidth() shouldBe value
      }
    }

    test("displayWidth is wide with the emoji presentation selector") {
      clusters("\u2764").single().displayWidth() shouldBe 1
      clusters("\u2764\uFE0F").single().displayWidth() shouldBe 2
    }

    test("isNewline accepts each line terminator") {
      ["\n", "\u000B", "\u000C", "\r", "\r\n"]
        .plus(listOf("\u0085", "\u2028", "\u2029"))
        .forEach { clusters(it).single().isNewline() shouldBe true }
      clusters("a").single().isNewline() shouldBe false
    }

    test("locationOf counts display columns from the line start") {
      val clusters = CharClusters("\u3042a\nxyz")
      clusters.locationOf(0) shouldBe Location(1, 1)
      clusters.locationOf(1) shouldBe Location(1, 3)
      clusters.locationOf(5) shouldBe Location(2, 3)
    }

    test("locationOf treats CRLF as one line break") {
      CharClusters("a\r\nb").locationOf(3) shouldBe Location(2, 1)
    }

    test("locationOf expands tabs and replaced controls") {
      CharClusters("\ta").locationOf(1) shouldBe Location(1, 5)
      CharClusters("\u202Ea").locationOf(1) shouldBe Location(1, 2)
    }

    test("locationOf inside a cluster points to the cluster start") {
      CharClusters("xe\u0301").locationOf(2) shouldBe Location(1, 2)
    }
  })
