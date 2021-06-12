package com.zachklipp.composecalc

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zachklipp.composecalc.Calculator.Error
import com.zachklipp.composecalc.Calculator.Row
import com.zachklipp.composecalc.EvaluationContext.Empty
import com.zachklipp.composecalc.grammar.ParseError
import com.zachklipp.composecalc.grammar.parse

internal class CalculatorImpl : Calculator {

  private val _rows = mutableStateListOf<RowImpl>()
  private var idCounter = 0

  override val rows: List<Row>
    get() = _rows

  override fun setRowInput(index: Int, input: String) {
    // Ensure a row exists at the given index.
    while (index >= _rows.size) {
      _rows += newRow().apply {
        previousRow = _rows.lastOrNull()
      }
    }
    _rows[index].input = input
  }

  override fun insertRowAt(index: Int, input: String) {
    // If the index does not exist yet, this is the same as a set.
    if (index >= _rows.size) {
      setRowInput(index, input)
      return
    }

    val newRow = newRow().also { it.input = input }

    // Insert the row into the linked list first.
    newRow.previousRow = _rows.getOrNull(index - 1)
    _rows[index].previousRow = newRow

    // Then add to the list.
    _rows.add(index, newRow)
  }

  override fun removeRowAt(index: Int) {
    _rows.removeAt(index).apply {
      // Unlink from the list, just in case something is still observing it.
      previousRow = null
    }
    _rows.getOrNull(index)?.previousRow = _rows.getOrNull(index - 1)
  }

  private fun newRow() = RowImpl(idCounter++) { "%s".format(it) }
}

@Stable
private class RowImpl(
  override val id: Any,
  private val resultFormatter: (Value) -> String
) : Row, EvaluationContext {

  var previousRow: RowImpl? by mutableStateOf(null)
  override var input by mutableStateOf("")

  val parseResult by derivedStateOf { parse(input) }
  val evalResult by derivedStateOf {
    parseResult.expression?.evaluate(previousRow ?: Empty)
  }

  override val result: String by derivedStateOf {
    evalResult?.value?.let(resultFormatter).orEmpty()
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

// TODO better error messages
private fun ParseError.toError() = Error(type.toString(), position)
private fun EvaluationError.toError() = Error(type.toString(), position)
