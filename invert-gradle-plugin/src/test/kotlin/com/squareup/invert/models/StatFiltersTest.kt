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
  fun `transform zeroes basic stats whose details do not match`() {
    val transform = StatFilters.transform(StatFilter(details = ValueMatcher.regex("^unit")))

    assertEquals(Stat.NumericStat(5, "unit tests"), transform(Stat.NumericStat(5, "unit tests")))
    assertEquals(Stat.NumericStat(0, "ui tests"), transform(Stat.NumericStat(7, "ui tests")))
    assertEquals(Stat.NumericStat(0, null), transform(Stat.NumericStat(7, null)))
    assertEquals(Stat.BooleanStat(false, "ui"), transform(Stat.BooleanStat(true, "ui")))
    assertEquals(Stat.BooleanStat(true, "unit"), transform(Stat.BooleanStat(true, "unit")))
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
  fun `validate rejects conditions that do not apply to the stat type`() {
    assertEquals(
      2,
      StatFilters.validate(StatFilter(details = ValueMatcher.present()), codeReferencesMetadata).size,
      "details on code references, and no extras",
    )
    assertEquals(
      2,
      StatFilters.validate(
        StatFilter(extras = mapOf("kind" to ValueMatcher.present())),
        numericMetadata,
      ).size,
      "extras on a numeric stat, and no details",
    )
    assertTrue(
      StatFilters.validate(
        StatFilter(details = ValueMatcher.present()),
        numericMetadata.copy(dataType = StatDataType.STRING),
      ).single().contains("STRING")
    )
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
