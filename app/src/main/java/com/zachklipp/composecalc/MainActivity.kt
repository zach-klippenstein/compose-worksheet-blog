package com.zachklipp.composecalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.zachklipp.composecalc.ui.CalculatorScreen
import com.zachklipp.composecalc.ui.theme.ComposeCalcTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      ComposeCalcTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colors.background) {
          CalculatorScreen(calculator = remember {
            Calculator(
              listOf(
                "1+2",
                "answer=42",
                "answer/2",
                "=error"
              )
            )
          })
        }
      }
    }
  }
}
