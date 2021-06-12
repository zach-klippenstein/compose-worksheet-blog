package com.zachklipp.composecalc

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.zachklipp.composecalc.Value.Error
import com.zachklipp.composecalc.Value.Fraction
import com.zachklipp.composecalc.Value.Integer
import com.zachklipp.composecalc.Value.Real
import org.junit.Test

class ValueTest {

  private class Case(
    val description: String,
    val left: Value,
    val op: (Value, Value) -> Value,
    val right: Value,
    val expected: Value,
  )

  // Fraction math should all use primes for these cases so there's no reduction.
  private val cases = listOf(
    // Ints, Ints
    Case("i+i=i", Integer(1), Value::plus, Integer(2), Integer(3)),
    Case("i-i=i", Integer(1), Value::minus, Integer(2), Integer(-1)),
    Case("i*i=i", Integer(2), Value::times, Integer(3), Integer(6)),
    Case("i/i=i", Integer(6), Value::div, Integer(3), Integer(2)),
    Case("i/i=f", Integer(3), Value::div, Integer(2), Fraction(3, 2)),
    // Ints, Fractions
    Case("i+f=f", Integer(3), Value::plus, Fraction(1, 2), Fraction(7, 2)),
    Case("i-f=f", Integer(3), Value::minus, Fraction(1, 2), Fraction(5, 2)),
    Case("i*f=f", Integer(3), Value::times, Fraction(2, 5), Fraction(6, 5)),
    Case("i/f=f", Integer(3), Value::div, Fraction(5, 2), Fraction(6, 5)),
    // Ints, Reals
    Case("i+r=r", Integer(1), Value::plus, Real(.2), Real(1.2)),
    Case("i-r=r", Integer(1), Value::minus, Real(.2), Real(0.8)),
    Case("i*r=r", Integer(2), Value::times, Real(.2), Real(2.0 * 0.2)),
    Case("i/r=r", Integer(6), Value::div, Real(.2), Real(6.0 / 0.2)),
    // Fractions, Fractions,
    Case("f+f=f", Fraction(1, 3), Value::plus, Fraction(1, 2), Fraction(5, 6)),
    Case("f-f=f", Fraction(1, 3), Value::minus, Fraction(1, 2), Fraction(-1, 6)),
    Case("f*f=f", Fraction(1, 3), Value::times, Fraction(1, 2), Fraction(1, 6)),
    Case("f/f=f", Fraction(1, 3), Value::div, Fraction(1, 2), Fraction(2, 3)),
    // Fractions, Reals
    Case("f+r=r", Fraction(1, 2), Value::plus, Real(0.2), Real(0.7)),
    Case("f-r=r", Fraction(1, 2), Value::minus, Real(0.2), Real(0.3)),
    Case("f*r=r", Fraction(1, 2), Value::times, Real(0.2), Real(0.1)),
    Case("f/r=r", Fraction(1, 2), Value::div, Real(0.2), Real(2.5)),
    // Reals, Reals
    Case("r+r=r", Real(0.5), Value::plus, Real(0.2), Real(0.7)),
    Case("r-r=r", Real(0.5), Value::minus, Real(0.2), Real(0.3)),
    Case("r*r=r", Real(0.5), Value::times, Real(0.2), Real(0.1)),
  )

  @Test fun operations() {
    cases.forEach { case ->
      assertWithMessage(case.description)
        .that(case.op(case.left, case.right))
        .isEqualTo(case.expected)
    }
  }

  @Test fun `divide by zero`() {
    assertThat(Integer(1) / Integer(0)).isEqualTo(Error)
    assertThat(Integer(1) / Fraction(0, 1)).isEqualTo(Error)
    assertThat(Integer(1) / Real(0.0)).isEqualTo(Error)
    assertThat(Fraction(1, 1) / Integer(0)).isEqualTo(Error)
    assertThat(Fraction(1, 1) / Fraction(0, 1)).isEqualTo(Error)
    assertThat(Fraction(1, 1) / Real(0.0)).isEqualTo(Error)
    assertThat(Real(1.0) / Integer(0)).isEqualTo(Error)
    assertThat(Real(1.0) / Fraction(0, 1)).isEqualTo(Error)
    assertThat(Real(1.0) / Real(0.0)).isEqualTo(Error)
  }

  @Test fun `fractions get reduced`() {
    val plusIdentity = Fraction(0, 1)
    val multIdentity = Fraction(1, 1)
    assertThat(Fraction(10, 8) + plusIdentity).isEqualTo(Fraction(5, 4))
    assertThat(Fraction(10, 8) - plusIdentity).isEqualTo(Fraction(5, 4))
    assertThat(Fraction(10, 8) * multIdentity).isEqualTo(Fraction(5, 4))
    assertThat(Fraction(10, 8) / multIdentity).isEqualTo(Fraction(5, 4))
    assertThat(Fraction(1, 0) / multIdentity).isEqualTo(Error)
  }
}
