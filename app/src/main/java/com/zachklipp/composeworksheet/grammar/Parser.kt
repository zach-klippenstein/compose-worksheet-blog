package com.zachklipp.composeworksheet.grammar

import com.zachklipp.composeworksheet.Expression
import com.zachklipp.composeworksheet.grammar.ExpressionAst.Assignment
import com.zachklipp.composeworksheet.grammar.ExpressionAst.Literal
import com.zachklipp.composeworksheet.grammar.ExpressionAst.NameReference
import com.zachklipp.composeworksheet.grammar.ExpressionAst.Operation
import com.zachklipp.composeworksheet.grammar.ParseError.Type.EXPECTED_EXPRESSION
import com.zachklipp.composeworksheet.grammar.ParseError.Type.EXPECTED_NAME
import com.zachklipp.composeworksheet.grammar.ParseError.Type.EXPECTED_OPERATOR
import com.zachklipp.composeworksheet.grammar.Token.LiteralToken
import com.zachklipp.composeworksheet.grammar.Token.NameToken
import com.zachklipp.composeworksheet.grammar.Token.Operator
import com.zachklipp.composeworksheet.grammar.Token.Operator.ASSIGN

/**
 * TODO write documentation
 */
fun parse(input: String): ParseResult {
  val tokens = tokenize(input)
  return parse(tokens)
}

@Suppress("ReplaceRangeToWithUntil")
private fun parse(input: List<Positioned<*>>): ParseResult {
  var index = 0
  val errors = mutableSetOf<ParseError>()
  // This stack will either be empty, or start with an expression. If non-empty, it will contain
  // alternating Expression, Operator, Expression, etc.
  val lookBehind = mutableListOf<HasPosition>()

  fun peekOp() = (lookBehind[lookBehind.size - 2] as? Positioned<*>)?.value as? Operator

  fun reduceHeadUntilPrecedence(targetPrecedence: Int) {
    // For well-formed input, the stack size will be
    while (lookBehind.size > 1 && (peekOp() ?: return).precedence >= targetPrecedence) {
      // Pop the top three items.
      // If the stack's in a bad state, an error will have already been reported, so just return
      // early.
      val rhs = lookBehind.removeLastOrNull() as? ExpressionAst ?: return
      val op = ((lookBehind.removeLastOrNull() as? Positioned<*>)?.value as? Operator) ?: return
      val lhs = lookBehind.removeLastOrNull() as? ExpressionAst ?: return

      when (op) {
        ASSIGN -> {
          if (lhs is NameReference) {
            lookBehind += Assignment(lhs, rhs)
          } else {
            errors += ParseError(EXPECTED_NAME, lhs.position)
          }
        }
        else -> lookBehind += Operation(lhs, op, rhs)
      }
    }
  }

  while (index < input.size) {
    val positionalToken = input[index]
    when (val token = positionalToken.value) {
      is NameToken -> lookBehind += NameReference(Positioned(token.name, positionalToken.position))
      is LiteralToken -> lookBehind += Literal(Positioned(token.value, positionalToken.position))
      is Operator -> {
        val prevExpr = lookBehind.lastOrNull()
        val prevOp = lookBehind.getOrNull(lookBehind.size - 2)

        lookBehind += when {
          prevExpr == null -> {
            errors += ParseError(
              type = if (token == ASSIGN) EXPECTED_NAME else EXPECTED_EXPRESSION,
              position = positionalToken.position
            )
            positionalToken
          }
          prevExpr !is ExpressionAst -> {
            errors += ParseError(EXPECTED_EXPRESSION, positionalToken.position)
            positionalToken
          }
          // We're the first operator, so always push.
          prevOp == null -> positionalToken
          prevOp !is Positioned<*> || prevOp.value !is Operator -> {
            errors += ParseError(EXPECTED_OPERATOR, positionalToken.position)
            positionalToken
          }
          else -> {
            reduceHeadUntilPrecedence(token.precedence)
            // The precedence of the previous operator is now guaranteed to be lower than ours,
            // so we can just push ourself onto the stack.
            positionalToken
          }
        }
      }
    }
    index++
  }

  // The lookbehind stack will now contain operators in strictly ascending order, so we can reduce
  // it in a single pass.
  reduceHeadUntilPrecedence(0)
  // If the input was well-formed, the stack will now contain a single expression.
  // If the stack is still not empty, the input was invalid, and errors will have been reported
  // already.

  if (lookBehind.isNotEmpty()) {
    // Any bare operators left in the stack are missing expressions.
    val first = lookBehind.first()
    if (first !is ExpressionAst && (first as? Positioned<*>)?.value != ASSIGN) {
      errors += ParseError(EXPECTED_EXPRESSION, 0..first.position.first)
    }
    val last = lookBehind.last()
    if (last !is ExpressionAst && (last as? Positioned<*>)?.value != ASSIGN) {
      errors += ParseError(EXPECTED_EXPRESSION, last.position.last + 1..Int.MAX_VALUE)
    }

    // Any remaining adjacent expressions are missing operators.
    lookBehind.runningReduce { prev, next ->
      if (!prev.isOperator && !next.isOperator) {
        errors += ParseError(EXPECTED_OPERATOR, prev.position.last + 1..next.position.first - 1)
      }
      next
    }
  }

  val singleNode = lookBehind.singleOrNull()
  return ParseResult(singleNode as? ExpressionAst, errors)
}

private val HasPosition.isOperator get() = (this as? Positioned<*>)?.value is Operator

data class ParseResult(
  val expression: Expression?,
  val errors: Set<ParseError>
)

data class ParseError(
  val type: Type,
  val position: IntRange
) {
  enum class Type {
    EXPECTED_NAME,
    EXPECTED_EXPRESSION,
    EXPECTED_OPERATOR,
  }
}

