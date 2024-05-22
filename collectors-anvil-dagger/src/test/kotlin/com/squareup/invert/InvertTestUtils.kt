package com.squareup.invert

import com.squareup.invert.models.InvertSerialization
import com.squareup.invert.models.InvertSerialization.InvertJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.test.assertEquals

object InvertTestUtils {
    val json: Json = Json {
        prettyPrint=true
    }

fun <T> assertEqualsAndPrintDiff(
    expected: T,
    actual: T,
    serializer: KSerializer<T>
) {
    val message = buildString {
        appendLine("Expected:")
        appendLine(json.encodeToString(serializer, expected))
        appendLine("Actual:")
        appendLine(json.encodeToString(serializer, actual))
    }

    assertEquals(
        expected = expected,
        actual = actual,
        message = message
    )
}
}
