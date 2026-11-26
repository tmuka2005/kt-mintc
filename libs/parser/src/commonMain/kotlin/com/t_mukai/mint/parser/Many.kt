package com.t_mukai.mint.parser

public fun <T, E> many(
  item: Parser<T, E>,
): Many<T, E> =
  Many(item, 0, Int.MAX_VALUE)

public class Many<out T, out E> internal constructor(
  internal val item: Parser<T, E>,
  internal val min: Int,
  private val max: Int,
) : Parser<List<T>, E> {
  public fun atLeast(
    count: Int,
  ): Parser<List<T>, E> =
    Many(item, count, Int.MAX_VALUE)

  public fun exactly(
    count: Int,
  ): Parser<List<T>, E> =
    Many(item, count, count)

  override fun parse(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<List<T>> =
    parseItems(ctx, mode)

  // region internal

  private fun parseItems(
    ctx: Context<in E>,
    mode: Mode,
  ): ParseResult<List<T>> {
    var values: ArrayList<T>? = null
    var count = 0
    var progressed = true
    while (progressed && count < max) {
      val before = ctx.mark()
      val result = item.parse(ctx, mode)
      if (result.isFailure) {
        ctx.rewind(before)
        break
      }
      count++
      if (mode == Mode.Emit) {
        values = (values ?: ArrayList()).apply { add(result.value) }
      }
      progressed = ctx.pos != before.pos
    }
    return when {
      count < min -> ParseResult.failure
      mode == Mode.Check -> ParseResult.omitted
      else -> ParseResult.success(values ?: emptyList())
    }
  }

  // endregion
}
