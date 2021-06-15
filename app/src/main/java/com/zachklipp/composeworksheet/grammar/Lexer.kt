package com.zachklipp.composeworksheet.grammar

import com.zachklipp.composeworksheet.Value
import com.zachklipp.composeworksheet.grammar.Token.LiteralToken
import com.zachklipp.composeworksheet.grammar.Token.NameToken
import com.zachklipp.composeworksheet.grammar.Token.Operator.ADD
import com.zachklipp.composeworksheet.grammar.Token.Operator.ASSIGN
import com.zachklipp.composeworksheet.grammar.Token.Operator.DIVIDE
import com.zachklipp.composeworksheet.grammar.Token.Operator.MULTIPLY
import com.zachklipp.composeworksheet.grammar.Token.Operator.SUBTRACT
import com.zachklipp.composeworksheet.Value.Integer
import com.zachklipp.composeworksheet.Value.Real

/**
 * Splits an input string into a series of [Token]s and their positions in the string.
 */
@Suppress("ReplaceRangeToWithUntil")
internal fun tokenize(input: String): List<Positioned<Token>> {
  var currStart = 0
  var currEnd = 0
  val result = mutableListOf<Positioned<Token>>()
  var currAccepter: TokenAccepter? = null

  /**
   * Adds the given [Token] to the result with the current range and advances the start and end
   * indices appropriately.
   */
  fun consumeToken(token: Token) {
    result += Positioned(token, position = currStart..currEnd)
    currEnd++
    currStart = currEnd
    currAccepter = null
  }

  fun startToken(accepter: TokenAccepter) {
    check(currAccepter == null)
    currAccepter = accepter
    currEnd++
  }

  fun accept(c: Char?): Pair<TokenAccepter?, Token?> =
    // Decrement end since we don't know if the current char is accepted yet.
    currAccepter?.accept(c, input.substring(currStart..(currEnd - 1)))
      ?: error("Expected accepter to not be null in the middle of a token.")

  while (currEnd < input.length) {
    val c = input[currEnd]

    if (currStart == currEnd) {
      // Starting a new token.
      when (c) {
        ADD.char -> consumeToken(ADD)
        SUBTRACT.char -> consumeToken(SUBTRACT)
        MULTIPLY.char -> consumeToken(MULTIPLY)
        DIVIDE.char -> consumeToken(DIVIDE)
        ASSIGN.char -> consumeToken(ASSIGN)
        '.', in '0'..'9' -> startToken(acceptDecimal)
        in 'a'..'z', in 'A'..'Z' -> startToken(acceptName)
        else -> {
          // Ignore whitespace and invalid characters.
          currStart++
          currEnd = currStart
        }
      }
    } else {
      // Continuing previous token.
      val (accepter, token) = accept(c)
      if (token != null) {
        // Token finished, consume it.
        currEnd--
        consumeToken(token)
      } else {
        // Continue processing the same token.
        currEnd++

        if (accepter != null) {
          // Use a different accepter for the rest of the token.
          currAccepter = accepter
        }
      }
    }
  }

  if (currStart != currEnd) {
    // The input finished in the middle of a token, so that automatically becomes the end.
    val (_, token) = accept(null)
    token?.let { consumeToken(token) }
  }

  return result
}

private val acceptInteger = TokenAccepter { c, text ->
  when (c) {
    in '0'..'9' -> Pair(null, null)
    else -> Pair(null, LiteralToken(parseLiteralValue(text)))
  }
}

/** Like `acceptInteger`, but also accepts a single '.' character. */
private val acceptDecimal = TokenAccepter { c, text ->
  when (c) {
    // After a single decimal point, no more decimal points are accepted.
    '.' -> Pair(acceptInteger, null)
    else -> acceptInteger.accept(c, text)
  }
}

private val acceptName = TokenAccepter { c, text ->
  when (c) {
    in 'a'..'z',
    in 'A'..'Z',
    in '0'..'9' -> Pair(null, null)
    else -> Pair(null, NameToken(text))
  }
}

private fun parseLiteralValue(text: String): Value =
  if ('.' in text) Real(text.toDouble()) else Integer(text.toInt())

sealed interface Token {

  enum class Operator(val char: Char, val precedence: Int) : Token {
    ASSIGN('=', 0),
    ADD('+', 1),
    SUBTRACT('-', 1),
    MULTIPLY('*', 2),
    DIVIDE('/', 2),
  }

  /** A numeric literal. */
  data class LiteralToken(val value: Value) : Token

  /** A variable name, either for assignment or reference. */
  data class NameToken(val name: String) : Token
}

private fun interface TokenAccepter {
  /**
   * Given the next character in the input, determine how to continue processing the current token:
   * - Return a non-null [Token] – the character is not part of the current token, so consume the
   *   current token and start a new one.
   * - Return a non-null [TokenAccepter] – the character is valid, but the rest of the token should
   *   be processed with a different strategy.
   * - Return all nulls – the character is valid and continue processing with the same strategy.
   *
   * @param c The character to process. If null, the end of the input was reached and a [Token]
   * should be returned.
   * @param currentText The substring of the input that is currently part of the current token.
   */
  fun accept(c: Char?, currentText: String): Pair<TokenAccepter?, Token?>
}
