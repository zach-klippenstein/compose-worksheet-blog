package com.zachklipp.composecalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.focused
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.zachklipp.composecalc.Worksheet

@Composable fun WorksheetScreen(
  name: String,
  onNameChange: (String) -> Unit,
  worksheetEditorState: WorksheetEditorState
) {
  Scaffold(
    topBar = {
      var titleFocused by remember { mutableStateOf(false) }
      TopAppBar(
        modifier = Modifier
          .semantics {
            focused = titleFocused
          }
          .onFocusChanged {
            titleFocused = it.hasFocus || it.isFocused
          }
          .focusTarget(),
        title = {
          if (titleFocused) {
            // Always request focus as soon as it's visible, since that means it was just tapped.
            val requester = remember { FocusRequester() }
            SideEffect {
              requester.requestFocus()
            }
            TextField(
              value = name,
              onValueChange = onNameChange,
              modifier = Modifier
                .focusRequester(requester)
                .fillMaxSize(),
            )
          } else {
            Text(
              name.ifBlank { "Unnamed" },
              Modifier
                .fillMaxSize()
                .clickable(onClickLabel = "Edit name") { titleFocused = true }
                .wrapContentSize(align = Alignment.CenterStart)
            )
          }
        })
    }
  ) {
    WorksheetEditor(worksheetEditorState)
  }
}

@Preview
@Composable private fun WorksheetScreenPreview() {
  WorksheetScreen(
    name = "Worksheet",
    onNameChange = {},
    worksheetEditorState = remember {
      WorksheetEditorState(
        Worksheet(listOf("1+2"))
      )
    }
  )
}
