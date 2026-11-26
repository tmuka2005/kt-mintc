package com.t_mukai.mint.parser

import com.t_mukai.mint.unicode.Location
import io.kotest.assertions.arrow.core.shouldBeSome
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

@Suppress("unused")
class RecoveryTest :
  FunSpec({
    val newline = char('\n')
    val lineComment =
      just("//").then(
        custom { _ ->
          while (peek() != null && peek()?.toString() != "\n") {
            advance()
          }
          ParseResult.unit
        },
      )
    val digit = char('0'..'9')
    val number =
      many(digit)
        .atLeast(1)
        .slice()
        .map { it.toString().toInt() }
        .padded()
        .labeled("number")

    fun <T> Parser<T, Nothing>.run(
      input: String,
    ) =
      parse(input, comment = lineComment, terminator = newline)

    fun <T> Output<T, *>.errorSummary() =
      diagnostics
        .filterIsInstance<Diagnostic.Unexpected>()
        .map { (start, expected) -> start to expected }

    test("delimiters without termination treat newlines as spaces") {
      val sum = many(number).map { it.sum() }
      sum
        .delimitedBy('(', ')')
        .run("(1\n2\n)")
        .value
        .shouldBeSome(3)
      sum
        .delimitedBy('{', '}', terminated = true)
        .run("{1\n2}")
        .errorSummary() shouldBe
        [
          Location(1, 3) to
            setOf(Expected.Label("number"), Expected.Char('}')),
        ]
    }
  })
