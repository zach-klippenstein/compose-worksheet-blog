package com.zachklipp.composeworksheet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zachklipp.composeworksheet.ui.WorksheetEditorState
import com.zachklipp.composeworksheet.ui.WorksheetScreen
import com.zachklipp.composeworksheet.ui.theme.ComposeWorksheetTheme

class MainActivity : ComponentActivity() {

  private var worksheetName by mutableStateOf("")

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ComposeWorksheetTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
          WorksheetScreen(
            name = worksheetName,
            onNameChange = { worksheetName = it },
            worksheetEditorState = remember {
              WorksheetEditorState(
                Worksheet(
                  """
                    subtotal=31.50
                    taxRate=0.15
                    partySize=3
                    taxAmount=subtotal*taxRate
                    total=subtotal+taxAmount
                    total/partySize
                  """.trimIndent().lines()
                )
              )
            })
        }
      }
    }
  }
}
