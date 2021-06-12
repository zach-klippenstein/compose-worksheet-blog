package com.zachklipp.composecalc.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize.Max
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode.Polite
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zachklipp.composecalc.Value
import com.zachklipp.composecalc.Worksheet
import com.zachklipp.composecalc.Worksheet.Row
import kotlin.math.max
import kotlin.math.min

@Composable internal fun WorksheetRowEditor(
  row: Row,
  formatResult: (Value?) -> String,
  onInputChange: (String) -> Unit,
  onAddLine: () -> Unit,
  onRemoveLine: () -> Unit,
  onMoveFocus: (FocusDirection) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputLayout: TextLayoutResult? by remember { mutableStateOf(null) }
  var selection: TextRange? by remember { mutableStateOf(null) }
  val interactionSource = remember { MutableInteractionSource() }
  val isFocused = interactionSource.collectIsFocusedAsState()

  Column(modifier.padding(horizontal = 8.dp)) {
    Row(Modifier.fillMaxWidth()) {
      TextLineField(
        value = row.input,
        onChange = onInputChange,
        onAddLine = onAddLine,
        onRemoveLine = onRemoveLine,
        onMoveFocus = onMoveFocus,
        onSelectionChanged = { selection = it },
        onTextLayoutResult = { inputLayout = it },
        interactionSource = interactionSource,
        modifier = Modifier
          .weight(1f)
          .drawRowErrors(row, inputLayout)
          .semantics {
            if (row.errors.isNotEmpty()) {
              this.error(row.errors.joinToString { it.message })
            }
          }
      )
      SelectionContainer {
        Text(
          "=" + formatResult(row.result),
          fontWeight = FontWeight.Bold,
          modifier = Modifier.semantics {
            this.contentDescription = "result is " + formatResult(row.result)
            if (isFocused.value) {
              liveRegion = Polite
            }
          }
        )
      }
    }

    // Show current error message.
    Box(Modifier.animateContentSize()) {
      if (interactionSource.collectIsFocusedAsState().value) {
        SelectedRowErrors(row.errors, selection)
      }
    }
  }
}

@Composable private fun SelectedRowErrors(
  errors: List<Worksheet.Error>,
  selection: TextRange?
) {
  val cursorPosition = selection?.takeIf { it.collapsed }?.start ?: return
  // Grow the end of the range by one so the error is shown even if the cursor is at the end of
  // the error range.
  val selectedErrors = errors.filter { cursorPosition in it.position.first..it.position.last + 1 }
  if (selectedErrors.isEmpty()) return

  Column(
    verticalArrangement = spacedBy(4.dp),
    modifier = Modifier.padding(vertical = 4.dp),
  ) {
    selectedErrors.forEach { error ->
      key(error.message) {
        val entryModifier = Modifier.composed {
          val offset = remember {
            Animatable(
              initialValue = Offset(-10f, -5f),
              typeConverter = Offset.VectorConverter
            )
          }
          LaunchedEffect(offset) {
            offset.animateTo(
              targetValue = Offset.Zero,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioHighBouncy,
                stiffness = Spring.StiffnessMedium
              )
            )
          }

          graphicsLayer {
            translationX = offset.value.x.dp.toPx()
            translationY = offset.value.y.dp.toPx()
          }
        }

        Row(
          Modifier
            // Force the icon to be as big as the text.
            .height(Max)
            .then(entryModifier)
        ) {
          Icon(
            Icons.Rounded.Warning,
            // Purely decorative, the fact that this text is an error is explicitly indicated via
            // semantics.
            contentDescription = null,
            tint = Color.Red.copy(alpha = LocalContentAlpha.current),
            modifier = Modifier
              .fillMaxHeight(.8f)
              .align(CenterVertically)
          )
          Spacer(Modifier.width(with(LocalDensity.current) { 2.sp.toDp() }))
          Text(
            text = error.message,
            color = Color.Red,
            style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Medium),
          )
        }
      }
    }
  }
}

private fun Modifier.drawRowErrors(
  row: Row,
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
    errorRects.forEach { rect ->
      clipRect(rect.left, rect.top, rect.right, rect.bottom) {
        var rising = true
        var segmentRect = rect.copy(
          top = inputLayout.firstBaseline,
          bottom = inputLayout.firstBaseline + 2.dp.toPx(),
          right = rect.left + 2.dp.toPx(),
        )

        while (rect.contains(segmentRect.topLeft)) {
          drawLine(
            Color.Red,
            start = if (rising) segmentRect.bottomLeft else segmentRect.topLeft,
            end = if (rising) segmentRect.topRight else segmentRect.bottomRight,
            strokeWidth = 1.dp.toPx(),
            cap = StrokeCap.Butt
          )
          rising = !rising
          segmentRect = segmentRect.translate(translateX = segmentRect.width, translateY = 0f)
        }
      }
    }
  }
}
