package com.t_mukai.mint.parser

import kotlin.jvm.JvmInline

public enum class Associativity { Left, Right, None }

@JvmInline
public value class FoldScope internal constructor(public val slice: Slice)

public fun interface InfixFold<T, in O> {
  public fun FoldScope.fold(
    lhs: T,
    op: O,
    rhs: T,
  ): T
}

public fun interface PrefixFold<T, in O> {
  public fun FoldScope.fold(
    op: O,
    operand: T,
  ): T
}

public fun interface PostfixFold<T, in O> {
  public fun FoldScope.fold(
    operand: T,
    op: O,
  ): T
}

public sealed interface Operator<T, out E>

public sealed class Infix<T, out E> : Operator<T, E> {
  internal abstract val operator: Parser<*, E>

  internal abstract val associativity: Associativity

  internal abstract fun fold(
    scope: FoldScope,
    lhs: T,
    op: Any?,
    rhs: T,
  ): T
}

public sealed class Prefix<T, out E> : Operator<T, E> {
  internal abstract val operator: Parser<*, E>

  internal abstract fun fold(
    scope: FoldScope,
    op: Any?,
    operand: T,
  ): T
}

public sealed class Postfix<T, out E> : Operator<T, E> {
  internal abstract val operator: Parser<*, E>

  internal abstract fun fold(
    scope: FoldScope,
    operand: T,
    op: Any?,
  ): T
}

public fun <T, O, E> infixLeft(
  operator: Parser<O, E>,
  fold: InfixFold<T, O>,
): Infix<T, E> =
  InfixOf(operator, Associativity.Left, fold)

public fun <T, O, E> infixRight(
  operator: Parser<O, E>,
  fold: InfixFold<T, O>,
): Infix<T, E> =
  InfixOf(operator, Associativity.Right, fold)

public fun <T, O, E> infixNone(
  operator: Parser<O, E>,
  fold: InfixFold<T, O>,
): Infix<T, E> =
  InfixOf(operator, Associativity.None, fold)

public fun <T, O, E> prefix(
  operator: Parser<O, E>,
  fold: PrefixFold<T, O>,
): Prefix<T, E> =
  PrefixOf(operator, fold)

public fun <T, O, E> postfix(
  operator: Parser<O, E>,
  fold: PostfixFold<T, O>,
): Postfix<T, E> =
  PostfixOf(operator, fold)

public fun <T, E> pratt(
  atom: Parser<T, E>,
  levels: List<List<Operator<T, E>>>,
): Parser<T, E> =
  Pratt(atom, levels)

// region internal

internal class Pratt<T, E>(
  val atom: Parser<T, E>,
  levels: List<List<Operator<T, E>>>,
) : Parser<T, E> {
  val prefixes: MutableList<Ranked<Prefix<T, E>>> = ArrayList()

  private val infixes: MutableList<Ranked<Infix<T, E>>> = ArrayList()

  private val postfixes: MutableList<Ranked<Postfix<T, E>>> = ArrayList()

  init {
    levels.forEachIndexed { index, level ->
      val precedence = levels.size - index
      for (operator in level) {
        when (operator) {
          is Prefix -> prefixes += Ranked(operator, precedence)
          is Infix -> infixes += Ranked(operator, precedence)
          is Postfix -> postfixes += Ranked(operator, precedence)
        }
      }
    }
  }

  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> =
    parseAbove(ctx, mode, 1)

  private fun parseAbove(
    ctx: Context<in E>,
    mode: Mode,
    minPrecedence: Int,
  ): ParseResult<T> {
    val start = ctx.pos
    var lhs = parseOperand(ctx, mode)
    var blocked = 0
    var folding = lhs.isSuccess
    while (folding) {
      val before = ctx.mark()
      var op: Any? = null
      val postfix =
        firstOf(
          ctx,
          mode,
          postfixes,
          { it.operator },
          { it >= minPrecedence },
        ) { op = it }
      val infix =
        if (postfix == null) {
          firstOf(
            ctx,
            mode,
            infixes,
            { it.operator },
            { it >= minPrecedence && it != blocked },
          ) { op = it }
        } else {
          null
        }
      val folded =
        when {
          postfix != null -> {
            foldPostfix(
              ctx, mode, before, start, postfix, lhs, op,
            )
          }

          infix != null -> {
            foldInfix(ctx, mode, before, start, infix, lhs, op)
          }

          else -> {
            ParseResult.failure
          }
        }
      if (folded.isSuccess &&
        infix?.operator?.associativity == Associativity.None
      ) {
        blocked = infix.precedence
      }
      folding = folded.isSuccess
      if (folding) {
        lhs = folded
      }
    }
    return if (mode == Mode.Check && lhs.isSuccess) {
      ParseResult.omitted
    } else {
      lhs
    }
  }

  private fun foldPostfix(
    ctx: Context<in E>,
    mode: Mode,
    before: Mark,
    start: Int,
    ranked: Ranked<Postfix<T, E>>,
    lhs: ParseResult<T>,
    op: Any?,
  ): ParseResult<T> =
    when {
      ctx.pos == before.pos -> {
        ctx.rewind(before)
        ParseResult.failure
      }

      mode == Mode.Emit -> {
        val scope = ctx.scopeFrom(start)
        ParseResult.success(ranked.operator.fold(scope, lhs.value, op))
      }

      else -> {
        lhs
      }
    }

  private fun foldInfix(
    ctx: Context<in E>,
    mode: Mode,
    before: Mark,
    start: Int,
    ranked: Ranked<Infix<T, E>>,
    lhs: ParseResult<T>,
    op: Any?,
  ): ParseResult<T> {
    val rhs = parseAbove(ctx, mode, ranked.rightPrecedence)
    return when {
      rhs.isFailure -> {
        ctx.rewind(before)
        ParseResult.failure
      }

      mode == Mode.Emit -> {
        val scope = ctx.scopeFrom(start)
        ParseResult.success(
          ranked.operator.fold(scope, lhs.value, op, rhs.value),
        )
      }

      else -> {
        lhs
      }
    }
  }

  private fun parseOperand(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<T> {
    val start = ctx.pos
    val before = ctx.mark()
    // Index loop on purpose: a for-each over the list allocates an
    // ArrayList iterator on every call, and escape analysis does not
    // remove it on this hot path (see #38).
    for (index in prefixes.indices) {
      val result = parsePrefixed(ctx, mode, prefixes[index], start)
      if (result.isSuccess) {
        return result
      }
      ctx.rewind(before)
    }
    return atom.parse(ctx, mode)
  }

  private fun parsePrefixed(
    ctx: Context<in E>,
    mode: Mode,
    ranked: Ranked<Prefix<T, E>>,
    start: Int,
  ): ParseResult<T> {
    val op =
      ranked.operator.operator
        .parse(ctx, mode)
    val operand =
      if (op.isSuccess) {
        parseAbove(ctx, mode, ranked.precedence)
      } else {
        ParseResult.failure
      }
    return when {
      operand.isFailure -> {
        ParseResult.failure
      }

      mode == Mode.Check -> {
        ParseResult.omitted
      }

      else -> {
        val scope = ctx.scopeFrom(start)
        ParseResult.success(
          ranked.operator.fold(scope, op.value, operand.value),
        )
      }
    }
  }

  private inline fun <O> firstOf(
    ctx: Context<in E>,
    mode: Mode,
    candidates: List<Ranked<O>>,
    parserOf: (O) -> Parser<*, E>,
    accepts: (Int) -> Boolean,
    matched: (Any?) -> Unit,
  ): Ranked<O>? {
    val before = ctx.mark()
    // Index loop on purpose: a for-each over the list allocates an
    // ArrayList iterator on every call, and escape analysis does not
    // remove it on this hot path (see #38).
    for (index in candidates.indices) {
      val ranked = candidates[index]
      if (!accepts(ranked.precedence)) {
        continue
      }
      val result = parserOf(ranked.operator).parse(ctx, mode)
      if (result.isSuccess) {
        matched(result.value)
        return ranked
      }
      ctx.rewind(before)
    }
    return null
  }

  private fun Context<in E>.scopeFrom(
    start: Int,
  ): FoldScope =
    FoldScope(Slice(input, start, pos))
}

internal class Ranked<out O>(
  val operator: O,
  val precedence: Int,
)

internal class InfixOf<T, O, E>(
  override val operator: Parser<O, E>,
  override val associativity: Associativity,
  private val folder: InfixFold<T, O>,
) : Infix<T, E>() {
  @Suppress("UNCHECKED_CAST")
  override fun fold(
    scope: FoldScope,
    lhs: T,
    op: Any?,
    rhs: T,
  ): T =
    with(folder) { scope.fold(lhs, op as O, rhs) }
}

internal class PrefixOf<T, O, E>(
  override val operator: Parser<O, E>,
  private val folder: PrefixFold<T, O>,
) : Prefix<T, E>() {
  @Suppress("UNCHECKED_CAST")
  override fun fold(
    scope: FoldScope,
    op: Any?,
    operand: T,
  ): T =
    with(folder) { scope.fold(op as O, operand) }
}

internal class PostfixOf<T, O, E>(
  override val operator: Parser<O, E>,
  private val folder: PostfixFold<T, O>,
) : Postfix<T, E>() {
  @Suppress("UNCHECKED_CAST")
  override fun fold(
    scope: FoldScope,
    operand: T,
    op: Any?,
  ): T =
    with(folder) { scope.fold(operand, op as O) }
}

private val Ranked<Infix<*, *>>.rightPrecedence: Int
  get() =
    if (operator.associativity == Associativity.Right) {
      precedence
    } else {
      precedence + 1
    }

// endregion
