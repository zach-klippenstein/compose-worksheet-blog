package com.zachklipp.composecalc.grammar

import kotlin.math.max
import kotlin.math.min

/**
 * TODO kdoc
 */
// TODO betternamme
internal interface HasPosition {
  val position: IntRange
}

/**
 * Associates a value with the indices at which it was found in the input string.
 */
internal data class Positioned<out T : Any>(
  val value: T,
  override val position: IntRange
) : HasPosition {
  fun <R : Any> map(f: (T) -> R): Positioned<R> = Positioned(f(value), position)
}

internal operator fun IntRange.plus(other: IntRange): IntRange =
  IntRange(
    start = min(this.first, other.first),
    endInclusive = max(this.last, other.last)
  )
