package com.zachklipp.composecalc.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.LocalContentColor
import androidx.compose.material.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

@Composable internal fun TextLineField(
  value: String,
  onChange: (String) -> Unit,
  onAddLine: () -> Unit,
  onRemoveLine: () -> Unit,
  onMoveFocus: (FocusDirection) -> Unit,
  onSelectionChanged: (TextRange) -> Unit,
  onTextLayoutResult: (TextLayoutResult) -> Unit,
  interactionSource: MutableInteractionSource,
  modifier: Modifier = Modifier,
) {
  var textValue by remember {
    mutableStateOf(
      TextFieldValue(value, selection = TextRange(value.length))
    )
  }
  textValue = textValue.copy(value)

  val keyModifier = Modifier
    .onPreviewKeyEvent {
      when {
        it.type != KeyEventType.KeyDown -> return@onPreviewKeyEvent false
        it.key == Key.Enter -> onAddLine()
        it.key == Key.Backspace && value.isEmpty() -> onRemoveLine()
        it.key == Key.DirectionUp -> onMoveFocus(FocusDirection.Up)
        it.key == Key.DirectionDown -> onMoveFocus(FocusDirection.Down)
        else -> return@onPreviewKeyEvent false
      }
      return@onPreviewKeyEvent true
    }

  BasicTextField(
    value = textValue,
    // Can't set singleLine to true because that removes the Enter key from the soft keyboard, and
    // we want to keep that to allow inserting new lines.
    singleLine = false,
    maxLines = 1,
    onValueChange = {
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
      keyboardType = KeyboardType.Ascii,
    ),
    interactionSource = interactionSource,
    modifier = modifier.then(keyModifier),
    textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current),
  )
}
