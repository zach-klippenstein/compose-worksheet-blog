package com.zachklipp.composeworksheet.grammar

import com.zachklipp.composeworksheet.EvaluationContext
import com.zachklipp.composeworksheet.EvaluationError
import com.zachklipp.composeworksheet.EvaluationError.Type.NAME_ERROR
import com.zachklipp.composeworksheet.EvaluationError.Type.UNDEFINED_NAME
import com.zachklipp.composeworksheet.EvaluationResult
import com.zachklipp.composeworksheet.Expression
import com.zachklipp.composeworksheet.Value
import com.zachklipp.composeworksheet.grammar.Token.Operator
import com.zachklipp.composeworksheet.grammar.Token.Operator.ADD
import com.zachklipp.composeworksheet.grammar.Token.Operator.ASSIGN
import com.zachklipp.composeworksheet.grammar.Token.Operator.DIVIDE
import com.zachklipp.composeworksheet.grammar.Token.Operator.MULTIPLY
import com.zachklipp.composeworksheet.grammar.Token.Operator.SUBTRACT
import com.zachklipp.composeworksheet.Value.Error
import com.zachklipp.composeworksheet.createFormatSpecifier
import java.util.Formattable
import java.util.Formatter

internal sealed class ExpressionAst : Expression, HasPosition, Formattable {

  data class NameReference(val name: Positioned<String>) : ExpressionAst()

  data class Literal(val value: Positioned<Value>) : ExpressionAst()

  data class Assignment(
    val name: NameReference,
    val value: ExpressionAst
  ) : ExpressionAst()

  data class Operation(
    val left: ExpressionAst,
    val operator: Operator,
    val right: ExpressionAst
  ) : ExpressionAst()

  override fun evaluate(context: EvaluationContext): EvaluationResult = when (this) {
    is Literal -> EvaluationResult(this.value.value)
    is NameReference -> {
      when (val nameValue = context.getValueForName(this.name.value)) {
        null -> EvaluationResult(
          value = Error,
          errors = setOf(EvaluationError(UNDEFINED_NAME, this.position))
        )
        Error -> EvaluationResult(
          value = Error,
          errors = setOf(EvaluationError(NAME_ERROR, this.position))
        )
        else -> EvaluationResult(nameValue)
      }
    }
    is Assignment -> {
      val rhsResult = value.evaluate(context)
      EvaluationResult(
        assignedName = name.name.value,
        value = rhsResult.value,
        errors = rhsResult.errors
      )
    }
    is Operation -> {
      val leftValue = left.evaluate(context)
      val rightValue = right.evaluate(context)
      val value = when (operator) {
        ADD -> leftValue.value + rightValue.value
        SUBTRACT -> leftValue.value - rightValue.value
        MULTIPLY -> leftValue.value * rightValue.value
        DIVIDE -> leftValue.value / rightValue.value
        ASSIGN -> {
          error(
            "Invalid expression: assignment operator was parsed as Operation: %s"
              .format(this)
          )
        }
      }
      EvaluationResult(value, errors = leftValue.errors + rightValue.errors)
    }
  }

  override val position: IntRange
    get() = when (this) {
      is Literal -> this.value.position
      is NameReference -> this.name.position
      is Assignment -> name.position + value.position
      is Operation -> left.position + right.position
    }

  override fun formatTo(formatter: Formatter, flags: Int, width: Int, precision: Int) {
    val formatString = createFormatSpecifier(flags, width, precision)
    when (this) {
      // Don't apply width/precision to names.
      is Literal -> formatter.format("%s", value.value)
      is NameReference -> formatter.format(formatString, name.value)
      is Assignment -> formatter.format("%s=$formatString", name.name, value)
      is Operation -> formatter.format("$formatString%s$formatString", left, operator.char, right)
    }
  }
}
