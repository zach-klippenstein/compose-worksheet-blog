package com.zachklipp.composeworksheet

/**
 * TODO write documentation
 */
internal fun createFormatSpecifier(
  flags: Int,
  width: Int,
  precision: Int,
  conversion: Char = 's'
): String = buildString {
  append('%')
  if (flags != 0) append(flags)
  if (width != -1) append(width)
  if (precision != -1) {
    append('.')
    append(precision)
  }
  append(conversion)
}