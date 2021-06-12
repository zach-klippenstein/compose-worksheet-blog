package com.zachklipp.composecalc

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zachklipp.composecalc.EvaluationContext.Empty
import com.zachklipp.composecalc.EvaluationError.Type.NAME_ERROR
import com.zachklipp.composecalc.EvaluationError.Type.UNDEFINED_NAME
import com.zachklipp.composecalc.Worksheet.Error
import com.zachklipp.composecalc.Worksheet.Row
import com.zachklipp.composecalc.grammar.ParseError
import com.zachklipp.composecalc.grammar.ParseError.Type.EXPECTED_EXPRESSION
import com.zachklipp.composecalc.grammar.ParseError.Type.EXPECTED_NAME
import com.zachklipp.composecalc.grammar.ParseError.Type.EXPECTED_OPERATOR
import com.zachklipp.composecalc.grammar.parse

internal class WorksheetImpl : Worksheet {

  private val _rows = mutableStateListOf<RowImpl>()
  private var idCounter = 0

  override val rows: List<Row>
    get() = _rows

  init {
    ensureAtLeastOneRow()
  }

  // override fun setRowInput(index: Int, input: String) {
  //   // Ensure a row exists at the given index.
  //   while (index >= _rows.size) {
  //     _rows += newRow().apply {
  //       previousRow = _rows.lastOrNull()
  //     }
  //   }
  //   _rows[index].input = input
  // }

  override fun insertRowAt(index: Int) {
    if (index >= _rows.size) {
      // The insert is out-of-bounds of the list, so we have to create rows to fill the list up to
      // the desired index. We can't just call add(index, row) because every element of the list
      // also needs to participate in the linked list.
      while (index >= _rows.size) {
        _rows += newRow().apply {
          previousRow = _rows.lastOrNull()
        }
      }
      return
    }

    // Else, insert the new row in the middle of the list.
    val newRow = newRow()

    // Insert the row into the linked list first.
    val nextRow = _rows[index]
    newRow.previousRow = nextRow.previousRow
    nextRow.previousRow = newRow

    // Then add to the list.
    _rows.add(index, newRow)
  }

  override fun removeRowAt(index: Int) {
    if (index >= _rows.size) return

    _rows.removeAt(index).apply {
      // Unlink from the list, just in case something is still observing it.
      previousRow = null
    }
    _rows.getOrNull(index)?.previousRow = _rows.getOrNull(index - 1)
    ensureAtLeastOneRow()
  }

  private fun ensureAtLeastOneRow() {
    if (_rows.isEmpty()) {
      insertRowAt(0)
    }
  }

  private fun newRow() = RowImpl(idCounter++)
}

@Stable
private class RowImpl(
  override val id: Any
) : Row, EvaluationContext {

  var previousRow: RowImpl? by mutableStateOf(null)
  override var input by mutableStateOf("")

  val parseResult by derivedStateOf { parse(input) }
  val evalResult by derivedStateOf {
    parseResult.expression?.evaluate(previousRow ?: Empty)
  }

  override val result: Value? by derivedStateOf {
    evalResult?.value
  }

  override val errors: List<Error> by derivedStateOf {
    if (parseResult.errors.isEmpty() && evalResult?.errors?.isEmpty() != false) {
      return@derivedStateOf emptyList()
    }
    mutableListOf<Error>().also { errors ->
      parseResult.errors.mapTo(errors) { it.toError() }
      evalResult?.errors?.mapTo(errors) { it.toError() }
    }
  }

  override fun getValueForName(name: String): Value? {
    return evalResult?.let { result ->
      result.value.takeIf { result.assignedName == name }
    } ?: previousRow?.getValueForName(name)
  }

  override fun toString(): String {
    return ("Row(id=$id, input=$input, result=$result, errors=$errors)")
  }
}

private fun ParseError.toError() = Error(
  when (type) {
    EXPECTED_NAME -> "Expected a name"
    EXPECTED_EXPRESSION -> "Expected expression"
    EXPECTED_OPERATOR -> "Expected an operator"
  }, position
)

private fun EvaluationError.toError() = Error(
  when (type) {
    UNDEFINED_NAME -> "Name is not defined"
    NAME_ERROR -> "Name evaluated to an error"
  }, position
)
