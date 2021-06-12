package com.zachklipp.composecalc.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zachklipp.composecalc.Calculator
import com.zachklipp.composecalc.Calculator.Row
import kotlin.math.max
import kotlin.math.min

/**
 * TODO write documentation
 */
@Composable fun CalculatorScreen(calculator: Calculator = remember { Calculator() }) {
  LazyColumn {
    itemsIndexed(calculator.rows, key = { _, row -> row.id }) { index, row ->
      CalculatorRow(
        row,
        onInputChange = { calculator.setRowInput(index, it) },
        onSplit = { new, current ->
          println("OMG split $new | $current")
          calculator.insertRowAt(index, new)
          calculator.setRowInput(index + 1, current)
        },
        onJoinToPrevious = { textToJoin ->
          if (index == 0) return@CalculatorRow
          val previousRow = calculator.rows[index - 1]
          calculator.setRowInput(index - 1, previousRow.input + textToJoin)
          calculator.removeRowAt(index)
        }
      )
      Divider()
    }
  }
}

@Composable private fun CalculatorRow(
  row: Calculator.Row,
  onInputChange: (String) -> Unit,
  onSplit: (String, String) -> Unit,
  onJoinToPrevious: (String) -> Unit
) {
  var inputLayout: TextLayoutResult? by remember { mutableStateOf(null) }
  var selection: TextRange? by remember { mutableStateOf(null) }
  val interactionSource = remember { MutableInteractionSource() }

  Column(Modifier.padding(horizontal = 8.dp)) {
    Row(Modifier.fillMaxWidth()) {
      TextLineField(
        value = row.input,
        onChange = onInputChange,
        onSplit = onSplit,
        onDelete = onJoinToPrevious,
        onSelectionChanged = { selection = it },
        onTextLayoutResult = { inputLayout = it },
        interactionSource = interactionSource,
        modifier = Modifier
          .weight(1f)
          .drawRowErrors(row, inputLayout)
      )
      SelectionContainer {
        Text(row.result)
      }
    }

    // Show current error message.
    Box(Modifier.animateContentSize()) {
      if (interactionSource.collectIsFocusedAsState().value) {
        SelectedRowErrors(row, selection)
      }
    }
  }
}

@Composable private fun TextLineField(
  value: String,
  onChange: (String) -> Unit,
  onSplit: (String, String) -> Unit,
  onDelete: (String) -> Unit,
  onSelectionChanged: (TextRange) -> Unit,
  onTextLayoutResult: (TextLayoutResult) -> Unit,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier
) {
  var textValue by remember { mutableStateOf(TextFieldValue(value)) }
  textValue = textValue.copy(value)

  val keyModifier = Modifier
    .onPreviewKeyEvent {
      // Backspace has to be handled in preview since the field will otherwise consume it.
      if (
        it.type == KeyEventType.KeyDown &&
        it.key == Key.Backspace &&
        textValue.selection == TextRange.Zero
      ) {
        // Backspace at start of line, ask to delete this line.
        onDelete(textValue.text)
        return@onPreviewKeyEvent true
      }
      return@onPreviewKeyEvent false
    }
    .onKeyEvent {
      if (it.key == Key.DirectionDown) {
        
      }
      if (it.type != KeyEventType.KeyUp) return@onKeyEvent false
      when (it.key) {
        // Key.Enter -> {
        //   println("OMG enter on range ${textValue.selection}")
        //   true
        // }
        // Key.Delete -> {
        //   println("OMG: delete")
        //   false
        // }
        else -> false
      }
    }

  BasicTextField(
    value = textValue,
    onValueChange = {
      val lineBreakIndex = it.text.indexOf('\n')
      if (lineBreakIndex != -1) {
        // Enter was pressed, so we need to split the line.
        val beforeText = it.text.substring(0, lineBreakIndex)
        val afterText = it.text.substring(lineBreakIndex + 1)
        onSplit(beforeText, afterText)
        onSelectionChanged(TextRange.Zero)
        return@BasicTextField
      }

      if (it.selection != textValue.selection) {
        onSelectionChanged(it.selection)
      }
      textValue = it
      if (value != it.text) {
        onChange(it.text)
      }
    },
    onTextLayout = onTextLayoutResult,
    keyboardOptions = KeyboardOptions(
      autoCorrect = false,
      keyboardType = KeyboardType.Number
    ),
    interactionSource = interactionSource,
    modifier = modifier.then(keyModifier),
  )
}

@Composable private fun SelectedRowErrors(
  row: Row,
  selection: TextRange?
) {
  val errors = row.errors.takeUnless { it.isEmpty() } ?: return
  val cursorPosition = selection?.takeIf { it.collapsed }?.start ?: return
  val selectedErrors = errors.filter { cursorPosition in it.position }

  Column {
    selectedErrors.forEach { error ->
      Text(
        text = error.message,
        color = Color.Red,
        style = MaterialTheme.typography.caption,
      )
    }
  }
}

private fun Modifier.drawRowErrors(
  row: Calculator.Row,
  inputLayout: TextLayoutResult?,
): Modifier = drawWithCache {
  if (row.errors.isEmpty() || inputLayout == null) {
    return@drawWithCache onDrawBehind {}
  }

  // TODO handle case where being drawn before errors has updated
  val errorRects = row.errors.mapNotNull { error ->
    val textIndices = inputLayout.layoutInput.text.indices
    if (error.position.first !in textIndices) return@mapNotNull null
    val startRect = inputLayout.getBoundingBox(error.position.first)
    val endRect =
      inputLayout.getBoundingBox(error.position.last.coerceIn(textIndices))
    Rect(
      left = min(startRect.left, endRect.left),
      top = min(startRect.top, endRect.top),
      right = max(startRect.right, endRect.right),
      bottom = max(startRect.bottom, endRect.bottom)
    )
  }

  onDrawBehind {
    errorRects.forEach {
      drawRoundRect(
        Color.Red,
        topLeft = it.topLeft,
        size = it.size,
        alpha = .5f,
        cornerRadius = CornerRadius(3.dp.toPx())
      )
    }
  }
}

@Preview(showBackground = true)
@Composable fun CalculatorScreenPreview() {
  CalculatorScreen(
    calculator = remember {
      Calculator(
        listOf(
          "1+2",
          "answer=42",
          "answer/2",
          "=error"
        )
      )
    }
  )
}