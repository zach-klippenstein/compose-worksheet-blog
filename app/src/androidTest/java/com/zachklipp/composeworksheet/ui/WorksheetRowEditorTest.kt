package com.zachklipp.composeworksheet.ui

import android.view.KeyEvent.ACTION_DOWN
import android.view.KeyEvent.ACTION_UP
import android.view.KeyEvent.KEYCODE_DEL
import android.view.KeyEvent.KEYCODE_DPAD_DOWN
import android.view.KeyEvent.KEYCODE_DPAD_UP
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.NativeKeyEvent
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsMatcher.Companion.expectValue
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zachklipp.composeworksheet.Value
import com.zachklipp.composeworksheet.Worksheet.Error
import com.zachklipp.composeworksheet.Worksheet.Row
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("TestFunctionName")
@RunWith(AndroidJUnit4::class)
class WorksheetRowEditorTest {

  @get:Rule val rule = createComposeRule()
  private val isEditable = SemanticsMatcher.keyIsDefined(SemanticsProperties.EditableText)

  @Test fun displays_row_data_and_semantics() {
    val row = TestRow().apply {
      input = "hello world"
      result = Value.Integer(42)
      errors = listOf(Error("oopsie", 0..0))
    }
    rule.setContent {
      WorksheetRowEditor(row)
    }

    rule.onNode(isEditable)
      .assertIsDisplayed()
      .assertTextEquals("hello world")
      .assert(expectValue(SemanticsProperties.Error, "oopsie"))
    rule.onNodeWithText("=42").assertIsDisplayed()
      .assertContentDescriptionEquals("result is 42")
    rule.onNodeWithText("oopsie").assertIsDisplayed()
  }

  @Test fun edits_invoke_event_handler() {
    val row = TestRow()
    rule.setContent {
      WorksheetRowEditor(row)
    }

    rule.onNode(isEditable)
      .performTextInput("hello")

    rule.onNode(isEditable)
      .assertIsDisplayed()
  }

  @Test fun arrow_keys_request_previous_and_nextfocus() {
    val row = TestRow()
    val focusMoves = mutableListOf<FocusDirection>()
    rule.setContent {
      WorksheetRowEditor(row, onMoveFocus = { focusMoves += it })
    }

    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DPAD_DOWN)))
    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_UP, KEYCODE_DPAD_DOWN)))
    assertEquals(listOf(FocusDirection.Down), focusMoves)

    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DPAD_UP)))
    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_UP, KEYCODE_DPAD_UP)))
    assertEquals(listOf(FocusDirection.Up), focusMoves)
  }

  @Test fun backspace_in_empty_row_removes() {
    val row = TestRow()
    var removeCount = 0
    rule.setContent {
      WorksheetRowEditor(row, onRemoveLine = { removeCount++ })
    }

    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DEL)))
    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_UP, KEYCODE_DEL)))
    assertEquals(1, removeCount)
  }

  @OptIn(ExperimentalTestApi::class)
  @Test fun backspace_in_nonempty_row_doesnt_remove() {
    val row = TestRow().apply {
      input = "hello"
    }
    var removeCount = 0
    rule.setContent {
      WorksheetRowEditor(row, onRemoveLine = { removeCount++ })
    }

    // Ensure the cursor is at the beginning of the line.
    rule.onNode(isEditable)
      .performTextInputSelection(TextRange.Zero)

    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_DOWN, KEYCODE_DEL)))
    rule.onNode(isEditable)
      .performKeyPress(KeyEvent(NativeKeyEvent(ACTION_UP, KEYCODE_DEL)))
    assertEquals(0, removeCount)
  }

  @Stable
  private class TestRow(override val id: Any = Unit) : Row {
    override var input: String by mutableStateOf("")
    override var errors: List<Error> by mutableStateOf(emptyList())
    override var result: Value? by mutableStateOf(null)
  }

  @Composable private fun WorksheetRowEditor(
    row: Row,
    formatResult: (Value?) -> String = { it.toString() },
    onInputChange: (String) -> Unit = {},
    onAddLine: () -> Unit = {},
    onRemoveLine: () -> Unit = {},
    onMoveFocus: (FocusDirection) -> Unit = {},
  ) {
    WorksheetRowEditor(
      row,
      formatResult,
      onInputChange,
      onAddLine,
      onRemoveLine,
      onMoveFocus,
      modifier = Modifier
    )
  }
}