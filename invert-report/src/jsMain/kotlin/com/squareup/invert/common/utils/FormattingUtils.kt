package com.squareup.invert.common.utils

object FormattingUtils {
    fun Int.formatDecimalSeparator(): String {
        return toString()
            .reversed()
            .chunked(3)
            .joinToString(",")
            .reversed()
    }
}