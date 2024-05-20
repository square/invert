package com.squareup.invert

import com.squareup.invert.models.InvertSerialization
import kotlinx.serialization.KSerializer
import kotlin.test.assertEquals

object InvertTestUtils {
  fun <T> assertEqualsAndPrintDiff(
    expected: T,
    actual: T,
    serializer: KSerializer<T>
  ) {
    val message = buildString {
      appendLine("Expected:")
      appendLine(InvertSerialization.InvertJson.encodeToString(serializer, expected))
      appendLine("Actual:")
      appendLine(InvertSerialization.InvertJson.encodeToString(serializer, actual))
    }

    assertEquals(
      expected = expected,
      actual = actual,
      message = message
    )
  }
}
