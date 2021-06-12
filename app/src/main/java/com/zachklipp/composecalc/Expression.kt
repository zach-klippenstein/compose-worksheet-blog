package com.zachklipp.composecalc

/**
 * TODO kdoc
 */
interface Expression {
  fun evaluate(context: EvaluationContext): EvaluationResult
}

interface EvaluationContext {
  /**
   * Looks up [name] in the context and, if found, returns its [Value]. If not found, returns null.
   */
  fun getValueForName(name: String): Value?

  object Empty : EvaluationContext {
    override fun getValueForName(name: String): Value? = null
  }
}

/**
 * The result of evaluating an [Expression].
 *
 * @param value The [Value] of the expression, possibly [Error].
 * @param assignedName If the expression included an assignment, the name that was assigned.
 * May be non-null even if there were errors.
 * @param errors A set of [EvaluationError]s that will be non-empty if [value] is [Error].
 */
data class EvaluationResult(
  val value: Value,
  val assignedName: String? = null,
  val errors: Set<EvaluationError> = emptySet()
)

data class EvaluationError(
  val type: Type,
  val position: IntRange
) {
  enum class Type {
    UNDEFINED_NAME,
    NAME_ERROR,
  }
}