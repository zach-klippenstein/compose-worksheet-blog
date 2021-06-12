package com.zachklipp.composecalc

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CalculatorTest {

  private var calculator = Calculator()

  @Test fun `empty calculator`() {
    assertThat(calculator.rows).isEmpty()
  }

  @Test fun `set first row of empty calculator to empty string`() {
    calculator.setRowInput(0, "")
    assertThat(calculator.rows).hasSize(1)
  }

  @Test fun `setting non-initial row of empty calculator grows rows`() {
    calculator.setRowInput(42, "")
    assertThat(calculator.rows).hasSize(43)
  }

  @Test fun `numeric literal evaluates to self`() {
    calculator.setRowInput(0, "42")
    calculator.rows.single().let { row ->
      assertThat(row.input).isEqualTo("42")
      assertThat(row.result).isEqualTo("42")
      assertThat(row.errors.isEmpty())
    }
  }

  @Test fun `row instance does not change after mutation`() {
    calculator.setRowInput(0, "")
    val initialRow = calculator.rows.single()

    calculator.setRowInput(0, "42")

    assertThat(calculator.rows.single()).isSameInstanceAs(initialRow)
  }

  @Test fun `row ID does not change after mutation`() {
    calculator.setRowInput(0, "")
    val initialId = calculator.rows.single().id

    calculator.setRowInput(0, "42")

    assertThat(calculator.rows.single().id).isEqualTo(initialId)
  }

  @Test fun `row insertion shifts subsequent rows`() {
    calculator = Calculator(listOf("42", "43", "44"))
    // Copy the list because it's a mutable list, and we want to actually snapshot it at this
    // point.
    val initialRows = calculator.rows.toList()
    val initialRowIds = calculator.rows.map { it.id }

    calculator.insertRowAt(1)

    assertThat(calculator.rows.map { it.input }).containsExactly("42", "", "43", "44")
    assertThat(calculator.rows[0]).isSameInstanceAs(initialRows[0])
    assertThat(calculator.rows[2]).isSameInstanceAs(initialRows[1])
    assertThat(calculator.rows[3]).isSameInstanceAs(initialRows[2])
    assertThat(calculator.rows[0].id).isEqualTo(initialRowIds[0])
    assertThat(calculator.rows[2].id).isEqualTo(initialRowIds[1])
    assertThat(calculator.rows[3].id).isEqualTo(initialRowIds[2])
  }

  @Test fun `row IDs are unique`() {
    repeat(10_000) {
      calculator.insertRowAt(0)
    }

    val ids = calculator.rows.distinctBy { it.id }
    assertThat(ids).hasSize(calculator.rows.size)
  }

  @Test fun `variable assignment result is variable value`() {
    calculator.setRowInput(0, "foo=42")
    assertThat(calculator.rows.single().result).isEqualTo("42")
  }

  @Test fun `variable can be used in subsequent row`() {
    calculator = Calculator(listOf("foo=42", "foo"))
    assertThat(calculator.rows[1].result).isEqualTo("42")
  }

  @Test fun `variable cannot be used before defined`() {
    calculator = Calculator(listOf("foo", "foo=42"))
    assertThat(calculator.rows[0].result).isEqualTo("!ERROR!")
    assertThat(calculator.rows[0].errors).isNotEmpty()
  }

  @Test fun `errors do not prevent calculation of independent subsequent rows`() {
    calculator = Calculator(listOf("foo", "foo=42"))
    assertThat(calculator.rows[1].result).isEqualTo("42")
    assertThat(calculator.rows[1].errors).isEmpty()
  }

  @Test fun `integer addition works`() {
    assertCalculation("1+2", "3")
  }

  @Test fun `integer subtraction works`() {
    assertCalculation("3-2", "1")
    assertCalculation("2-3", "-1")
  }

  @Test fun `integer multiplication works`() {
    assertCalculation("3*2", "6")
  }

  @Test fun `integer division works`() {
    assertCalculation("6/2", "3")
  }

  @Test fun `fractional addition works`() {
    assertCalculation(".1+.2", "0.3")
  }

  @Test fun `fractional subtraction works`() {
    assertCalculation(".3-.2", "0.1")
    assertCalculation(".2-.3", "-0.1")
  }

  @Test fun `fractional multiplication works`() {
    assertCalculation("6*.2", "3")
    assertCalculation("3*.2", "0.6")
    assertCalculation(".3*.2", "0.06")
  }

  @Test fun `fractional division works`() {
    assertCalculation(".6/.2", "3")
    assertCalculation("3/.2", "15")
    assertCalculation(".3/2", "0.15")
  }

  private fun assertCalculation(input: String, expectedResult: String) {
    // Ensure the calculator is empty.
    calculator = Calculator()
    calculator.setRowInput(0, input)
    assertThat(calculator.rows[0].errors).isEmpty()
    assertThat(calculator.rows[0].result).isEqualTo(expectedResult)
  }
}
