package com.zachklipp.composecalc

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Creats a new [Worksheet] instance, optionally populated with the given list of [rows].
 */
@Suppress("FunctionName")
fun Worksheet(rows: Iterable<String> = emptyList()): Worksheet = WorksheetImpl().apply {
  rows.forEachIndexed { index, row ->
    insertRowAt(index)
    this.rows[index].input = row
  }
}

/**
 * TODO write documentation
 */
@Stable
interface Worksheet {

  /**
   * TODO kdoc
   */
  val rows: List<Row>

  /**
   * Inserts a new row at the given index. The row previously at that index, and all following it,
   * will be shifted down.
   */
  fun insertRowAt(index: Int)

  /**
   * TODO kdoc
   */
  fun removeRowAt(index: Int)

  @Stable
  interface Row {
    /**
     * An object that uniquely identifies this row within this [Worksheet] instance. IDs can be
     * compared, but no other guarantees are made. They will remain stable over the lifetime of a
     * given [Worksheet] instance, but are not guaranteed to be stable between processes.
     */
    val id: Any

    /**
     * The input for the row, used to calculate [result].
     * If this string is invalid, [errors] will be non-empty.
     */
    var input: String

    /**
     * A list of [Error]s that will be non-empty only if the [input] could not be parsed or
     * executed.
     */
    val errors: List<Error>

    /**
     * The result of executing the calculation described by [input], within the context of the
     * current [Worksheet].
     */
    val result: Value?
  }

  @Immutable
  data class Error(
    val message: String,
    val position: IntRange
  )
}
