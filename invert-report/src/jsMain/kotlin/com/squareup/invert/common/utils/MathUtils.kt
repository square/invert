package com.squareup.invert.common.utils;

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Utils to help format numbers and calculate percentages.
 */
object MathUtils {

  /**
   * Formats a number with commas.
   */
  fun Int.formatted(): String {
    return formatNumber(this)
  }

  fun formatNumber(number: Int): String {
    return number.asDynamic().toLocaleString("en-US")
  }

  fun percentage(amount: Int, total: Int): Double {
    return if (amount == 0 && total == 0) {
      100.0
    } else if (amount == 0) {
      0.0
    } else {
      roundToDecimal(amount / total.toDouble() * 100)
    }
  }

  fun roundToDecimal(value: Double, decimalPlaces: Int = 1): Double {
    val factor = 10.0.pow(decimalPlaces)
    val rounded = (value * factor).roundToInt() / factor
    return if (rounded == 0.0) {
      roundToDecimal(value, decimalPlaces + 1)
    } else {
      rounded
    }
  }
}