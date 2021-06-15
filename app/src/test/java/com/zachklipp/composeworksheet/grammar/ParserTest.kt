package com.zachklipp.composeworksheet.grammar

import com.google.common.truth.Truth.assertThat
import com.zachklipp.composeworksheet.grammar.ExpressionAst.Assignment
import com.zachklipp.composeworksheet.grammar.ExpressionAst.Literal
import com.zachklipp.composeworksheet.grammar.ExpressionAst.NameReference
import com.zachklipp.composeworksheet.grammar.ExpressionAst.Operation
import com.zachklipp.composeworksheet.grammar.ParseError.Type.EXPECTED_EXPRESSION
import com.zachklipp.composeworksheet.grammar.ParseError.Type.EXPECTED_NAME
import com.zachklipp.composeworksheet.grammar.ParseError.Type.EXPECTED_OPERATOR
import com.zachklipp.composeworksheet.grammar.Token.Operator.ADD
import com.zachklipp.composeworksheet.grammar.Token.Operator.MULTIPLY
import com.zachklipp.composeworksheet.Value.Integer
import org.junit.Test

class ParserTest {

  @Test fun `empty input`() {
    val result = parse("")
    assertThat(result.errors).isEmpty()
    assertThat(result.expression).isNull()
  }

  @Test fun `single integer value`() {
    val result = parse("42")
    assertThat(result.errors).isEmpty()
    (result.expression as Literal).let { expr ->
      assertThat(expr.value.value).isEqualTo(Integer(42))
    }
  }

  @Test fun `single name`() {
    val result = parse("foo")
    assertThat(result.errors).isEmpty()
    (result.expression as NameReference).let { expr ->
      assertThat(expr.name.value).isEqualTo("foo")
    }
  }

  @Test fun `single operation`() {
    val result = parse("1+2")
    assertThat(result.errors).isEmpty()
    (result.expression as Operation).let { expr ->
      assertThat(expr.operator).isEqualTo(ADD)
      assertThat((expr.left as Literal).value.value).isEqualTo(Integer(1))
      assertThat((expr.right as Literal).value.value).isEqualTo(Integer(2))
    }
  }

  @Test fun `single assignment`() {
    val result = parse("answer=42")
    assertThat(result.errors).isEmpty()
    (result.expression as Assignment).let { expr ->
      assertThat(expr.name.name.value).isEqualTo("answer")
      assertThat((expr.value as Literal).value.value).isEqualTo(Integer(42))
    }
  }

  @Test fun `operators are grouped by precedence`() {
    val result = parse("1+2*3")
    assertThat(result.errors).isEmpty()
    (result.expression as Operation).let { expr ->
      assertThat(expr.operator).isEqualTo(ADD)
      assertThat((expr.left as Literal).value.value).isEqualTo(Integer(1))
      (expr.right as Operation).let { expr2 ->
        assertThat(expr2.operator).isEqualTo(MULTIPLY)
        assertThat((expr2.left as Literal).value.value).isEqualTo(Integer(2))
        assertThat((expr2.right as Literal).value.value).isEqualTo(Integer(3))
      }
    }
  }

  @Test fun `operators with equal precedence are left-associative`() {
    val result = parse("1+2+3")
    assertThat(result.errors).isEmpty()
    (result.expression as Operation).let { expr ->
      assertThat(expr.operator).isEqualTo(ADD)
      (expr.left as Operation).let { leftExpr ->
        assertThat(leftExpr.operator).isEqualTo(ADD)
        assertThat((leftExpr.left as Literal).value.value).isEqualTo(Integer(1))
        assertThat((leftExpr.right as Literal).value.value).isEqualTo(Integer(2))
      }
      assertThat((expr.right as Literal).value.value).isEqualTo(Integer(3))
    }
  }

  @Test fun `multiple assignments fails`() {
    val result = parse("foo=bar=42")
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_NAME, 0..6)
    )
    assertThat(result.expression).isNull()
  }

  @Test fun `assign without target fails`() {
    val result = parse("=1")
    assertThat(result.expression).isNull()
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_NAME, 0..0)
    )
  }

  @Test fun `assign with non-name target fails`() {
    val result = parse("23=1")
    assertThat(result.expression).isNull()
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_NAME, 0..1)
    )
  }

  @Test fun `adjacent operators fails`() {
    val result = parse("1++2")
    assertThat(result.expression).isEqualTo(Literal(Positioned(Integer(1), 0..0)))
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_EXPRESSION, 2..2)
    )
  }

  @Test fun `adjacent literals fails`() {
    val result = parse("1 2")
    assertThat(result.expression).isNull()
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_OPERATOR, 1..1)
    )
  }

  @Test fun `leading operator fails`() {
    val result = parse("+1+2")
    assertThat(result.expression).isNull()
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_EXPRESSION, 0..0)
    )
  }

  @Test fun `trailing operator fails`() {
    val result = parse("1+2+")
    assertThat(result.expression).isNull()
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_EXPRESSION, 4..Int.MAX_VALUE)
    )
  }

  @Test fun `operator without operands`() {
    val result = parse("+")
    assertThat(result.expression).isNull()
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_EXPRESSION, 0..0),
      ParseError(EXPECTED_EXPRESSION, 1..Int.MAX_VALUE),
    )
  }

  @Test fun `leading assign with multiple expression`() {
    val result = parse("=1 2")
    assertThat(result.errors).containsExactly(
      ParseError(EXPECTED_NAME, 0..0),
      ParseError(EXPECTED_OPERATOR, 2..2),
    )
  }
}
