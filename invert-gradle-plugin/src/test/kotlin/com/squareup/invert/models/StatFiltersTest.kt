package com.squareup.invert.models

import com.squareup.invert.models.Stat.CodeReferencesStat.CodeReference
import com.squareup.invert.models.utils.StatFilters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StatFiltersTest {

  private val codeReferencesMetadata = StatMetadata(
    key = "legacy_screens",
    description = "Legacy Screens",
    dataType = StatDataType.CODE_REFERENCES,
    extras = listOf(
      ExtraMetadata(key = "kind", type = ExtraDataType.STRING, description = "Kind"),
      ExtraMetadata(key = "gated", type = ExtraDataType.BOOLEAN, description = "Gated"),
    ),
  )

  private val numericMetadata = StatMetadata(
    key = "test_count",
    description = "Test Count",
    dataType = StatDataType.NUMERIC,
  )

  @Test
  fun `ONE_OF matches any listed value and never a missing value`() {
    val matches = StatFilters.predicate(ValueMatcher.oneOf("dialog", "card"))
    assertTrue(matches("dialog"))
    assertTrue(matches("card"))
    assertFalse(matches("layout"))
    assertFalse(matches(null))
  }

  @Test
  fun `REGEX finds a match anywhere unless anchored`() {
    assertTrue(StatFilters.predicate(ValueMatcher.regex("Dialog"))("LegacyDialogScreen"))
    assertFalse(StatFilters.predicate(ValueMatcher.regex("^Dialog$"))("LegacyDialogScreen"))
    assertTrue(StatFilters.predicate(ValueMatcher.regex("^x", "Screen$"))("LegacyDialogScreen"))
    assertFalse(StatFilters.predicate(ValueMatcher.regex("Dialog"))(null))
  }

  @Test
  fun `PRESENT matches any value, including empty`() {
    val matches = StatFilters.predicate(ValueMatcher.present())
    assertTrue(matches(""))
    assertTrue(matches("anything"))
    assertFalse(matches(null))
  }

  @Test
  fun `negate inverts the result, so a missing value matches`() {
    val matches = StatFilters.predicate(ValueMatcher.equalTo("dialog").negated())
    assertFalse(matches("dialog"))
    assertTrue(matches("layout"))
    assertTrue(matches(null))
    assertFalse(StatFilters.predicate(ValueMatcher.present().negated())("x"))
  }

  @Test
  fun `transform keeps only code references whose extras all match`() {
    val references = listOf(
      reference("a.kt", "kind" to "dialog", "gated" to "true"),
      reference("b.kt", "kind" to "dialog", "gated" to "false"),
      reference("c.kt", "kind" to "layout", "gated" to "true"),
      reference("d.kt"),
    )
    val filter = StatFilter(
      extras = mapOf(
        "kind" to ValueMatcher.equalTo("dialog"),
        "gated" to ValueMatcher.equalTo("true"),
      ),
    )

    val result = StatFilters.transform(filter)(Stat.CodeReferencesStat(references))

    assertEquals(listOf("a.kt"), (result as Stat.CodeReferencesStat).value.map { it.filePath })
  }

  @Test
  fun `validate accepts a filter on declared extras`() {
    val filter = StatFilter(extras = mapOf("kind" to ValueMatcher.equalTo("dialog")))
    assertEquals(emptyList(), StatFilters.validate(filter, codeReferencesMetadata))
  }

  @Test
  fun `validate rejects extras that the stat does not declare`() {
    val filter = StatFilter(extras = mapOf("knid" to ValueMatcher.equalTo("dialog")))
    val errors = StatFilters.validate(filter, codeReferencesMetadata)
    assertEquals(1, errors.size)
    assertTrue(errors.single().contains("'knid' is not declared"), errors.single())
    assertTrue(errors.single().contains("gated, kind"), errors.single())
  }

  @Test
  fun `validate rejects stats that are not code references`() {
    val filter = StatFilter(extras = mapOf("kind" to ValueMatcher.present()))
    listOf(StatDataType.NUMERIC, StatDataType.BOOLEAN, StatDataType.STRING).forEach { dataType ->
      val errors = StatFilters.validate(filter, numericMetadata.copy(dataType = dataType))
      assertEquals(1, errors.size, errors.toString())
      assertTrue(errors.single().contains("is a $dataType stat"), errors.single())
    }
  }

  @Test
  fun `validate rejects a filter without extras conditions`() {
    assertTrue(
      StatFilters.validate(StatFilter(), codeReferencesMetadata).single().contains("no 'extras' conditions")
    )
  }

  @Test
  fun `validate rejects a blank key`() {
    val filter = StatFilter(extras = mapOf("kind" to ValueMatcher.present()), key = " ")
    assertTrue(StatFilters.validate(filter, codeReferencesMetadata).single().contains("blank 'key'"))
  }

  @Test
  fun `validate rejects empty and invalid matchers`() {
    val errors = StatFilters.validate(
      StatFilter(
        extras = mapOf(
          "kind" to ValueMatcher(MatchOp.ONE_OF),
          "gated" to ValueMatcher.regex("(unclosed"),
        ),
      ),
      codeReferencesMetadata,
    )
    assertEquals(2, errors.size, errors.toString())
    assertTrue(errors.any { it.contains("ONE_OF needs at least one value") })
    assertTrue(errors.any { it.contains("Invalid regular expression '(unclosed'") })
  }

  private fun reference(filePath: String, vararg extras: Pair<String, String>) = CodeReference(
    filePath = filePath,
    startLine = 1,
    endLine = 1,
    extras = extras.toMap(),
  )
}
