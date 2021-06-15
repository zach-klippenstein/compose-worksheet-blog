package com.zachklipp.composeworksheet.ui

import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zachklipp.composeworksheet.Worksheet

/**
 * A component that displays all the rows in a [Worksheet] allows editing each [Row]'s input, and
 * displays any errors for each row.
 */
@Composable fun WorksheetEditor(worksheet: WorksheetEditorState) {
  val focusManager = LocalFocusManager.current
  var rowIndexToFocus: Int? by remember { mutableStateOf(0) }

  LazyColumn {
    item {
      Row(
        horizontalArrangement = spacedBy(8.dp),
        modifier = Modifier
          .toggleable(
            value = worksheet.showFractions,
            onValueChange = { worksheet.showFractions = it })
          .padding(8.dp)
      ) {
        Checkbox(
          checked = worksheet.showFractions,
          onCheckedChange = { worksheet.showFractions = it },
          modifier = Modifier.clearAndSetSemantics { }
        )
        Text("Show fractions")
      }
    }

    itemsIndexed(worksheet.worksheet.rows, key = { _, row -> row.id }) { index, row ->
      WorksheetRowEditor(
        row,
        formatResult = worksheet::formatValue,
        onInputChange = { row.input = it /*.setRowInput(index, it)*/ },
        onAddLine = {
          worksheet.worksheet.insertRowAt(index + 1)
          rowIndexToFocus = index + 1
        },
        onRemoveLine = {
          worksheet.worksheet.removeRowAt(index)
          rowIndexToFocus = index - 1
        },
        onMoveFocus = focusManager::moveFocus,
        modifier = worksheet.modifierForId(row.id)
      )

      SideEffect {
        if (rowIndexToFocus == index) {
          worksheet.requestFocusId(row.id)
          rowIndexToFocus = null
        }
      }

      Divider()
    }
  }
}

@Preview(showBackground = true)
@Composable fun CalculatorEditorPreview() {
  WorksheetEditor(
    worksheet = remember {
      WorksheetEditorState(
        Worksheet(
          listOf(
            "1+2",
            "answer=42",
            "answer/2",
            "=error"
          )
        )
      )
    }
  )
}