package com.t_mukai.mint.repl

import com.t_mukai.mint.evaluation.EvaluationError
import com.t_mukai.mint.evaluation.Value
import com.t_mukai.mint.parser.Diagnostic
import com.t_mukai.mint.parser.Expected
import com.t_mukai.mint.repl.ReplOutput.Result.Bound
import com.t_mukai.mint.unicode.CharClusters
import com.t_mukai.mint.unicode.Location

public fun main() {
  val session = ReplSession()
  while (true) {
    print("> ")
    val line = readlnOrNull() ?: break
    val (diagnostics, result) = session.run(line)
    when (result) {
      is Failed -> println(result.error.format(CharClusters(line)))
      is Bound -> println(result.format())
      is Discarded, is NotEvaluated -> Unit
    }
    diagnostics.forEach { println(it.format()) }
    println()
  }
}

private fun Bound.format(): String =
  "val $name: ${value.typeName} = ${value.format()}"

private val Value.typeName: String
  get() =
    when (this) {
      is Value.Int -> "Int"
      is Value.Float -> "Float"
      is Value.Unit -> "Unit"
    }

private fun Value.format(): String =
  when (this) {
    is Value.Int -> value.toString()
    is Value.Float -> value.toString()
    is Value.Unit -> "Unit"
  }

private fun EvaluationError.format(
  clusters: CharClusters,
): String {
  val start = clusters.locationOf(node.text.start)
  val end = clusters.locationOf(node.text.end)
  return "Error [${span(start, end)}] ${this::class.simpleName}"
}

private fun Diagnostic<*>.format(): String =
  when (this) {
    is Diagnostic.Reported<*> -> {
      "Warning [${span(start, end)}] $kind"
    }

    is Diagnostic.Unexpected -> {
      "Error [${span(start, end)}] Expected ${expected()}, found ${found()}"
    }
  }

private fun span(
  start: Location,
  end: Location,
): String =
  "${start.row}:${start.col}-${end.row}:${end.col}"

private fun Diagnostic.Unexpected.found(): String =
  found?.let { "'$it'" } ?: "end of input"

private fun Diagnostic.Unexpected.expected(): String {
  val e =
    expected
      .filterNot { it is Expected.EndOfInput }
      .ifEmpty { expected.toList() }
  return if (e.isEmpty()) {
    "nothing"
  } else if (e.size == 1) {
    e.first().format()
  } else {
    e.dropLast(1).joinToString(", ") { it.format() } +
      " or ${e.last().format()}"
  }
}

private fun Expected.format(): String =
  when (this) {
    is Expected.AnyChar -> "Any"
    is Expected.Char -> "'$c'"
    is Expected.Label -> name
    is Expected.Literal -> "'$text'"
    is Expected.Range -> "'${range.first}'..'${range.last}'"
    is Expected.EndOfInput -> "End of input"
  }
