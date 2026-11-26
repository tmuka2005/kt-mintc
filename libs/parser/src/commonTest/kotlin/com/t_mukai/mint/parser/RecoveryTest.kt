package com.t_mukai.mint.parser

import arrow.core.None
import arrow.core.Some
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
    val letter = choice(char('a'..'z'), char('_'))
    val digit = char('0'..'9')
    val ident =
      letter
        .then(many(choice(letter, digit)))
        .slice()
        .map { it.toString() }
        .padded()
        .labeled("identifier")
    val number =
      many(digit)
        .atLeast(1)
        .slice()
        .map { it.toString().toInt() }
        .padded()
        .labeled("number")
    val letStmt =
      keyword("let")
        .padded()
        .commit()
        .then(ident)
        .thenIgnore(char('=').padded())
        .then(number)
        .map { name, value -> name to value }
    val program = many(letStmt.thenIgnore(many(newline)))

    fun <T> Parser<T, Nothing>.run(
      input: String,
    ) =
      parse(input, comment = lineComment, terminator = newline)

    fun <T> Output<T, *>.errorSummary() =
      diagnostics
        .filterIsInstance<Diagnostic.Unexpected>()
        .map { (start, expected) -> start to expected }

    test("padded stops at the terminator outside delimiters") {
      val output = program.run("let x = 1 // one\nlet y = 2")
      output.value.shouldBeSome(
        [
          Some("x") to Some(1),
          Some("y") to Some(2),
        ],
      )
    }

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

    test("commit skips unexpected input") {
      val output = letStmt.run("let x = @@ 1")
      output.value.shouldBeSome(Some("x") to Some(1))
      output.errorSummary() shouldBe
        [Location(1, 9) to setOf(Expected.Label("number"))]
    }

    test("commit treats a missing element as None") {
      val output = letStmt.run("let x 1")
      output.value.shouldBeSome(Some("x") to Some(1))
      output.errorSummary() shouldBe
        [Location(1, 7) to setOf(Expected.Char('='))]
    }

    test("commit gives up at the terminator") {
      val output = program.run("let x =\nlet y = 2")
      output.value.shouldBeSome(
        [Some("x") to None, Some("y") to Some(2)],
      )
      output.errorSummary() shouldBe
        [Location(1, 8) to setOf(Expected.Label("number"))]
    }

    test("commit gives up at a closing delimiter") {
      val output = letStmt.delimitedBy('(', ')').run("(let x = )")
      output.value.shouldBeSome(Some("x") to None)
      output.errorSummary() shouldBe
        [Location(1, 10) to setOf(Expected.Label("number"))]
    }
  })
