package com.zachklipp.composecalc

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

/**
 * Creats a new [Calculator] instance, optionally populated with the given list of [rows].
 */
@Suppress("FunctionName")
fun Calculator(rows: Iterable<String> = emptyList()): Calculator = CalculatorImpl().apply {
  rows.forEachIndexed { index, row ->
    setRowInput(index, row)
  }
}

/**
 * TODO write documentation
 */
@Stable
interface Calculator {

  /**
   * TODO kdoc
   */
  val rows: List<Row>

  /**
   * Sets the input text for the row at the given index. The row does not need to be "create" first,
   * the list of rows will be expanded if necessary.
   */
  fun setRowInput(index: Int, input: String)

  /**
   * Inserts a new row at the given index. The row previously at that index, and all following it,
   * will be shifted down.
   */
  fun insertRowAt(index: Int, input: String = "")

  /**
   * TODO kdoc
   */
  fun removeRowAt(index: Int)

  @Stable
  interface Row {
    /**
     * An object that uniquely identifies this row within this [Calculator] instance. IDs can be
     * compared, but no other guarantees are made. They will remain stable over the lifetime of a
     * given [Calculator] instance, but are not guaranteed to be stable between processes.
     */
    val id: Any

    /**
     * The input for the row, used to calculate [result].
     * This can be modified by calling [setRowInput].
     * If this string is invalid, [errors] will be non-empty.
     */
    val input: String

    /**
     * A list of [ParseError]s that will be non-empty only if the [input] could not be parsed or
     * executed.
     */
    val errors: List<Error>

    /**
     * The result of executing the calculation described by [input], within the context of the
     * current [Calculator].
     */
    val result: String
  }

  @Immutable
  data class Error(
    val message: String,
    val position: IntRange
  )
}
