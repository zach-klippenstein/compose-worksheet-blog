package com.zachklipp.composecalc

import androidx.compose.runtime.Stable

/**
 * TODO write documentation
 */
@Stable
interface Calculator {

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

  @Stable
  interface Row {
    val id: Any
    val input: String
    val errors: List<RowError>
    val result: String
  }

  @Stable
  interface RowError {
    /** The range of indices within the input that is in error. */
    val inputRange: IntRange
    val message: String
  }
}
