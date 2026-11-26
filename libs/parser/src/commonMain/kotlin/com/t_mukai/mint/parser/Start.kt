package com.t_mukai.mint.parser

internal class Start(
  private val ranges: List<CharRange>,
  private val nullable: Boolean,
  private val opaque: Boolean,
  val padded: Boolean,
  val expected: Set<Expected>,
) {
  val mayMatchAtEnd: Boolean
    get() = opaque || nullable

  val mayStartWithNonAscii: Boolean
    get() =
      mayMatchAtEnd || padded || ranges.any { it.last.code >= ASCII_SIZE }

  fun mayStartWith(
    c: Char,
  ): Boolean =
    mayMatchAtEnd || ranges.any { c in it }

  fun then(
    next: () -> Start,
  ): Start =
    followedBy(next)

  fun or(
    other: Start,
  ): Start =
    Start(
      ranges + other.ranges,
      nullable || other.nullable,
      opaque || other.opaque,
      padded || other.padded,
      expected + other.expected,
    )

  fun optional(): Start =
    Start(ranges, true, opaque, padded, expected)

  fun padded(): Start =
    Start(ranges, nullable, opaque, true, expected)

  fun labeled(
    label: Expected.Label,
  ): Start =
    Start(ranges, nullable, opaque, padded, setOf(label))

  companion object {
    val opaque: Start = constant(nullable = true, opaque = true)

    val nullable: Start = constant(nullable = true, opaque = false)

    val never: Start = constant(nullable = false, opaque = false)

    val anyChar: Start =
      of(Char.MIN_VALUE..Char.MAX_VALUE, Expected.AnyChar)

    fun empty(
      expected: Expected,
    ): Start =
      Start(emptyList(), true, opaque = false, padded = false, setOf(expected))

    fun of(
      range: CharRange,
      expected: Expected,
    ): Start =
      Start([range], false, opaque = false, padded = false, setOf(expected))

    private fun constant(
      nullable: Boolean,
      opaque: Boolean,
    ): Start =
      Start(emptyList(), nullable, opaque, padded = false, emptySet())
  }

  private fun followedBy(
    next: () -> Start,
  ): Start {
    if (!nullable) {
      return this
    }
    val rest = next()
    return Start(
      ranges + rest.ranges,
      rest.nullable,
      opaque || rest.opaque,
      padded || rest.padded,
      expected + rest.expected,
    )
  }
}

internal fun Parser<*, *>.start(): Start =
  startOf(this)

@Suppress("CyclomaticComplexMethod", "LongMethod")
private fun startOf(
  parser: Parser<*, *>,
): Start =
  when (parser) {
    is CharParser -> {
      Start.of(parser.c..parser.c, Expected.Char(parser.c))
    }

    is RangeParser -> {
      Start.of(parser.range, Expected.Range(parser.range))
    }

    is CodePointParser -> {
      Start.of(Char.MIN_VALUE..Char.MAX_VALUE, parser.label)
    }

    is LiteralParser -> {
      startOfLiteral(parser.text)
    }

    AnyParser -> {
      Start.anyChar
    }

    EndParser -> {
      Start.empty(Expected.EndOfInput)
    }

    EmptyParser -> {
      Start.nullable
    }

    NewlineParser -> {
      Start.of(Char.MIN_VALUE..Char.MAX_VALUE, NewlineParser.label)
    }

    is Then<*, *, *> -> {
      parser.first
        .start()
        .then { parser.second.start() }
    }

    is ThenIgnore<*, *> -> {
      parser.first
        .start()
        .then { parser.second.start() }
    }

    is IgnoreThen<*, *> -> {
      parser.first
        .start()
        .then { parser.second.start() }
    }

    is Mapped<*, *, *> -> {
      parser.inner.start()
    }

    is Sliced<*> -> {
      parser.inner.start()
    }

    is WithSlice<*, *> -> {
      parser.inner.start()
    }

    is Many<*, *> -> {
      startOfMany(parser)
    }

    is Choice<*, *> -> {
      parser.candidates
        .map { it.start() }
        .fold(Start.never, Start::or)
    }

    is Labeled<*, *> -> {
      parser.inner
        .start()
        .labeled(parser.label)
    }

    is Padded<*, *> -> {
      parser.inner
        .start()
        .padded()
    }

    is Delimited<*, *> -> {
      Start.of(parser.open..parser.open, Expected.Char(parser.open))
    }

    is Committed<*> -> {
      parser.head.start()
    }

    is CommittedSeq<*, *> -> {
      parser.head.start()
    }

    is Recursive<*, *> -> {
      parser.start()
    }

    is Reporting<*, *> -> {
      parser.inner.start()
    }

    is MappedWithReports<*, *, *> -> {
      parser.inner.start()
    }

    is AndIs<*, *> -> {
      parser.inner.start()
    }

    is Not<*> -> {
      Start.nullable
    }

    is Pratt<*, *> -> {
      startOfPratt(parser)
    }

    is Custom<*, *> -> {
      Start.opaque
    }
  }

private fun startOfLiteral(
  text: String,
): Start =
  if (text.isEmpty()) {
    Start.empty(Expected.Literal(text))
  } else {
    Start.of(text[0]..text[0], Expected.Literal(text))
  }

private fun startOfMany(
  many: Many<*, *>,
): Start {
  val itemStart = many.item.start()
  return if (many.min == 0) {
    itemStart.optional()
  } else {
    itemStart
  }
}

private fun startOfPratt(
  pratt: Pratt<*, *>,
): Start {
  val operators = pratt.prefixes.map { it.operator.operator }
  return operators.fold(pratt.atom.start()) { start, operator ->
    start.or(operator.start())
  }
}
