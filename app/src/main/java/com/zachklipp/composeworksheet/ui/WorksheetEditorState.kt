package com.zachklipp.composeworksheet.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.zachklipp.composeworksheet.Value
import com.zachklipp.composeworksheet.Value.Error
import com.zachklipp.composeworksheet.Value.Fraction
import com.zachklipp.composeworksheet.Worksheet
import java.util.concurrent.atomic.AtomicInteger

@Stable
class WorksheetEditorState(val worksheet: Worksheet) {

  private val rowModifiersById: MutableMap<Any, RowModifier> = mutableStateMapOf()

  var showFractions by mutableStateOf(true)

  fun formatValue(value: Value?): String = when (value) {
    null, is Error -> ""
    is Fraction -> if (showFractions) "%s".format(value) else formatValue(value.toReal())
    else -> "%s".format(value)
  }

  @SuppressLint("ModifierFactoryExtensionFunction")
  fun modifierForId(rowId: Any): Modifier = Modifier.composed {
    val rowModifier = remember(rowId) {
      rowModifiersById.getOrPut(rowId) { RowModifier() }
        // The refcount must be incremented here, in the composition, instead of later in the
        // effect, so that if two compositions are running in parallel, one removing this modifier
        // instance and one adding it, the refcount will always be guaranteed to be incremented by
        // the time the onDispose callback runs (onDispose callbacks will be ran serially on the
        // main thread).
        .also { it.refCount.incrementAndGet() }
    }

    DisposableEffect(rowModifier) {
      onDispose {
        val refCount = rowModifier.refCount.decrementAndGet()
        if (refCount == 0) {
          rowModifiersById -= rowId
        }
      }
    }

    return@composed focusRequester(rowModifier.focusRequester)
  }

  fun requestFocusId(rowId: Any) {
    rowModifiersById[rowId]?.focusRequester?.requestFocus()
  }

  private inner class RowModifier {
    val focusRequester = FocusRequester()
    val refCount = AtomicInteger(0)
  }
}
