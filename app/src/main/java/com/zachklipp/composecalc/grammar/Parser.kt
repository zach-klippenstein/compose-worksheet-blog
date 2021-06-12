package com.zachklipp.composecalc.grammar

import com.zachklipp.composecalc.Expression
import com.zachklipp.composecalc.grammar.ExpressionAst.Assignment
import com.zachklipp.composecalc.grammar.ExpressionAst.Literal
import com.zachklipp.composecalc.grammar.ExpressionAst.NameReference
import com.zachklipp.composecalc.grammar.ExpressionAst.Operation
import com.zachklipp.composecalc.grammar.ParseError.Type.EXPECTED_EXPRESSION
import com.zachklipp.composecalc.grammar.ParseError.Type.EXPECTED_NAME
import com.zachklipp.composecalc.grammar.ParseError.Type.EXPECTED_OPERATOR
import com.zachklipp.composecalc.grammar.Token.LiteralToken
import com.zachklipp.composecalc.grammar.Token.NameToken
import com.zachklipp.composecalc.grammar.Token.Operator
import com.zachklipp.composecalc.grammar.Token.Operator.ASSIGN

/**
 * TODO write documentation
 */
fun parse(input: String): ParseResult {
  val tokens = tokenize(input)
  return parse(tokens)
}

private fun parse(input: List<Positioned<*>>): ParseResult {
  var index = 0
  val errors = mutableSetOf<ParseError>()
  // This stack will either be empty, or start with an expression. If non-empty, it will contain
  // alternating Expression, Operator, Expression, etc.
  val lookBehind = mutableListOf<Any>()

  fun peekOp() = lookBehind[lookBehind.size - 2] as? Operator

  fun reduceHeadUntilPrecedence(targetPrecedence: Int) {
    // For well-formed input, the stack size will be
    while (lookBehind.size > 1 && (peekOp() ?: return).precedence >= targetPrecedence) {
      // Pop the top three items.
      // If the stack's in a bad state, an error will have already been reported, so just return
      // early.
      val rhs = lookBehind.removeLastOrNull() as? ExpressionAst ?: return
      val op = lookBehind.removeLastOrNull() as? Operator ?: return
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
            token
          }
          prevExpr !is ExpressionAst -> {
            errors += ParseError(EXPECTED_EXPRESSION, positionalToken.position)
            token
          }
          // We're the first operator, so always push.
          prevOp == null -> token
          prevOp !is Operator -> {
            errors += ParseError(EXPECTED_OPERATOR, positionalToken.position)
            token
          }
          else -> {
            reduceHeadUntilPrecedence(token.precedence)
            // The precedence of the previous operator is now guaranteed to be lower than ours,
            // so we can just push ourself onto the stack.
            token
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

  val expr = lookBehind.singleOrNull() as ExpressionAst?
  return ParseResult(expr, errors)
}

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

