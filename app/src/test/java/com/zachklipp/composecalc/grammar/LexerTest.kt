package com.zachklipp.composecalc

import com.google.common.truth.Truth.assertThat
import com.zachklipp.composecalc.Token.LiteralToken
import com.zachklipp.composecalc.Token.NameToken
import com.zachklipp.composecalc.Token.Operator.ADD
import com.zachklipp.composecalc.Token.Operator.ASSIGN
import com.zachklipp.composecalc.Token.Operator.DIVIDE
import com.zachklipp.composecalc.Token.Operator.MULTIPLY
import com.zachklipp.composecalc.Token.Operator.SUBTRACT
import com.zachklipp.composecalc.Value.Integer
import com.zachklipp.composecalc.Value.Real
import org.junit.Test

class LexerTest {

  @Test fun `empty input`() {
    val result = tokenize("")
    assertThat(result).isEmpty()
  }

  @Test fun `integer literal`() {
    val result = tokenize("42")
    val (token, position) = result.single()
    assertThat(token).isEqualTo(LiteralToken(Integer(42)))
    assertThat(position).isEqualTo(0..2)
  }

  @Test fun `decimal literal without leading zero`() {
    val result = tokenize(".1")
    val (token, position) = result.single()
    assertThat(token).isEqualTo(LiteralToken(Real(.1)))
    assertThat(position).isEqualTo(0..2)
  }

  @Test fun `decimal literal with leading zero`() {
    val result = tokenize("0.1")
    val (token, position) = result.single()
    assertThat(token).isEqualTo(LiteralToken(Real(.1)))
    assertThat(position).isEqualTo(0..3)
  }

  @Test fun `leading and trailing whitespace is ignored`() {
    val result = tokenize("     \t\t42  \t ")
    val (token, position) = result.single()
    assertThat(token).isEqualTo(LiteralToken(Integer(42)))
    assertThat(position).isEqualTo(7..8)
  }

  @Test fun `intermediate whitespace is ignored`() {
    val result = tokenize("answer = 42")
    assertThat(result).containsExactly(
      Positioned(NameToken("answer"), 0..5),
      Positioned(ASSIGN, 7..7),
      Positioned(LiteralToken(Integer(42)), 9..11),
    )
  }

  @Test fun `operators work`() {
    val result = tokenize("+*-/=")
    assertThat(result).containsExactly(
      Positioned(ADD, 0..0),
      Positioned(MULTIPLY, 1..1),
      Positioned(SUBTRACT, 2..2),
      Positioned(DIVIDE, 3..3),
      Positioned(ASSIGN, 4..4),
    )
  }

  @Test fun `expression works`() {
    val result = tokenize("answer=4+2")
    assertThat(result).containsExactly(
      Positioned(NameToken("answer"), 0..5),
      Positioned(ASSIGN, 6..6),
      Positioned(LiteralToken(Integer(4)), 7..7),
      Positioned(ADD, 8..8),
      Positioned(LiteralToken(Integer(2)), 9..10),
    )
  }
}
