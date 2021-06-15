package com.zachklipp.composeworksheet.grammar

import kotlin.math.max
import kotlin.math.min

/**
 * Represents an object that is associated with a position in a [parsed][parse] string.
 */
internal interface HasPosition {
  val position: IntRange
}

/**
 * Associates a value with the indices at which it was found in the input string.
 */
internal data class Positioned<out T : Any>(
  val value: T,
  override val position: IntRange
) : HasPosition

internal operator fun IntRange.plus(other: IntRange): IntRange =
  IntRange(
    start = min(this.first, other.first),
    endInclusive = max(this.last, other.last)
  )
