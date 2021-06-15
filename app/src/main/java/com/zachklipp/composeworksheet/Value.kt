package com.zachklipp.composeworksheet

import com.zachklipp.composeworksheet.Value.Error
import com.zachklipp.composeworksheet.Value.Fraction
import com.zachklipp.composeworksheet.Value.Integer
import com.zachklipp.composeworksheet.Value.Real
import java.util.Formattable
import java.util.Formatter

/**
 * A value that can be represented by the expression language.
 *
 * - [Integer] division will either produce another [Integer] or a [Fraction].
 * - Any operation with a [Real] will product another [Real]. [Fraction]s will be converted to
 *   [Real]s first.
 * - [Fraction]s multiplied or divided with other [Fraction]s will produce reduced [Fraction]s.
 * - Any [Fraction] that reduces to a denominator of 1 will produce an [Integer].
 * - Any operation with an [Error] will produce an [Error].
 */
sealed class Value : Formattable {
  data class Integer(val value: Int) : Value()
  data class Fraction(val numerator: Int, val denominator: Int) : Value()
  data class Real(val value: Double) : Value()

  /** The "value" of an invalid expression. */
  object Error : Value()

  operator fun plus(rhs: Value): Value = when {
    this == Error || rhs == Error -> Error
    this is Integer && rhs is Integer -> Integer(value + rhs.value)
    this is Real || rhs is Real -> Real(this.toReal().value + rhs.toReal().value)
    this is Fraction || rhs is Fraction -> {
      val (ln, ld) = this.toFraction()
      val (rn, rd) = rhs.toFraction()
      Fraction(
        numerator = ln * rd + rn * ld,
        denominator = ld * rd
      ).reduce()
    }
    else -> calcError("Invalid values: $this + $rhs")
  }

  operator fun minus(rhs: Value): Value = this + (NEGATIVE_ONE * rhs)

  operator fun times(rhs: Value): Value = when {
    this == Error || rhs == Error -> Error
    this is Integer && rhs is Integer -> Integer(value * rhs.value)
    this is Real || rhs is Real -> Real(this.toReal().value * rhs.toReal().value)
    this is Fraction || rhs is Fraction -> {
      val (ln, ld) = this.toFraction()
      val (rn, rd) = rhs.toFraction()
      Fraction(ln * rn, ld * rd).reduce()
    }
    else -> calcError("Invalid values: $this * $rhs")
  }

  operator fun div(rhs: Value): Value = when {
    this == Error || rhs == Error -> Error
    rhs is Integer && rhs.value == 0 -> Error
    rhs is Real && rhs.value == 0.0 -> Error
    rhs is Fraction && rhs.numerator == 0 -> Error
    this is Integer && rhs is Integer -> Fraction(value, rhs.value).reduce()
    this is Real || rhs is Real -> Real(this.toReal().value / rhs.toReal().value)
    this is Fraction || rhs is Fraction -> {
      (this.toFraction() * rhs.toFraction().inverse())
        .toFraction()
        .reduce()
    }
    else -> calcError("Invalid values: $this / $rhs")
  }

  override fun formatTo(formatter: Formatter, flags: Int, width: Int, precision: Int) {
    when (this) {
      Error -> formatter.format("!ERROR!")
      is Fraction -> {
        val specifier = createFormatSpecifier(flags, width, precision, 'd')
        formatter.format("$specifier/$specifier", numerator, denominator)
      }
      is Integer -> formatter.format(createFormatSpecifier(flags, width, precision, 'd'), value)
      is Real -> formatter.format(createFormatSpecifier(flags, width, precision, 'f'), value)
    }
  }

  fun toReal(): Real = when (this) {
    Error -> error("Tried to convert an Error value to a Real.")
    is Real -> this
    is Integer -> Real(value.toDouble())
    is Fraction -> Real(numerator.toDouble() / denominator.toDouble())
  }

  private fun toFraction() = when (this) {
    Error -> error("Tried to convert an Error value to a Fraction.")
    is Real -> error("Tried to convert a Real value to a Fraction.")
    is Fraction -> this
    is Integer -> Fraction(numerator = value, denominator = 1)
  }

  private fun calcError(message: String): Nothing = throw IllegalArgumentException(message)
}

private val NEGATIVE_ONE = Integer(-1)

private fun Fraction.reduce(): Value =
  when (val gcd = euclidsGcd(numerator, denominator)) {
    1 -> this
    denominator -> Integer(numerator / gcd)
    else -> Fraction(numerator / gcd, denominator / gcd)
  }

private fun Fraction.inverse() = Fraction(denominator, numerator)

private tailrec fun euclidsGcd(n1: Int, n2: Int): Int =
  if (n2 == 0) n1 else euclidsGcd(n2, n1 % n2)
