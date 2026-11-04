package com.squareup.invert.models

import com.google.gson.Gson
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.js.TechDebtInitiative
import com.squareup.invert.models.js.TechDebtInitiativeConfig
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class TechDebtInitiativeTest {

  private val unfiltered = TechDebtInitiative(
    id = "kotlin",
    title = "Migrate to Kotlin",
    descriptionMarkdown = "",
    unit = "Files",
    completedStatKey = "kotlin_files",
    remainingStatKey = "java_files",
  )

  private val filtered = unfiltered.copy(
    id = "kotlin_payments",
    remainingFilter = StatFilter(extras = mapOf("team" to ValueMatcher.oneOf("payments", "checkout"))),
    completedFilter = StatFilter(extras = mapOf("team" to ValueMatcher.equalTo("payments").negated())),
  )

  @Test
  fun `effective keys are the source keys without filters`() {
    assertEquals("java_files", unfiltered.effectiveRemainingStatKey)
    assertEquals("kotlin_files", unfiltered.effectiveCompletedStatKey)
  }

  @Test
  fun `effective keys are derived keys with filters`() {
    assertEquals("tdi__kotlin_payments__remaining", filtered.effectiveRemainingStatKey)
    assertEquals("tdi__kotlin_payments__completed", filtered.effectiveCompletedStatKey)
    assertEquals(
      "java_files",
      filtered.copy(remainingFilter = null).effectiveRemainingStatKey,
      "copy() recomputes the effective keys",
    )
  }

  @Test
  fun `effective completed key is null without a completed stat`() {
    assertNull(filtered.copy(completedStatKey = null).effectiveCompletedStatKey)
  }

  @Test
  fun `tdi_config json includes effective keys and filter defaults`() {
    val json = InvertJson.encodeToJsonElement(
      TechDebtInitiativeConfig.serializer(),
      TechDebtInitiativeConfig(listOf(unfiltered, filtered)),
    )
    val (first, second) = json.jsonObject.getValue("tdis").jsonArray.map { it.jsonObject }

    assertEquals("java_files", first.getValue("effectiveRemainingStatKey").jsonPrimitive.content)
    assertEquals("kotlin_files", first.getValue("effectiveCompletedStatKey").jsonPrimitive.content)
    assertFalse("remainingFilter" in first, "No filter is written when there is none")
    assertEquals(
      "tdi__kotlin_payments__remaining",
      second.getValue("effectiveRemainingStatKey").jsonPrimitive.content,
    )
    val completedMatcher = second.getValue("completedFilter").jsonObject
      .getValue("extras").jsonObject.getValue("team").jsonObject
    assertEquals("true", completedMatcher.getValue("negate").jsonPrimitive.content)
    val remainingFilter = second.getValue("remainingFilter").jsonObject
    assertEquals(
      "false",
      remainingFilter.getValue("extras").jsonObject.getValue("team").jsonObject
        .getValue("negate").jsonPrimitive.content,
      "Defaults are written, so readers that ignore Kotlin defaults see them",
    )
  }

  @Test
  fun `tdi_config json round trips`() {
    val config = TechDebtInitiativeConfig(listOf(unfiltered, filtered))
    val decoded = InvertJson.decodeFromString(
      TechDebtInitiativeConfig.serializer(),
      InvertJson.encodeToString(TechDebtInitiativeConfig.serializer(), config),
    )
    assertEquals(config, decoded)
    assertEquals(filtered.effectiveRemainingStatKey, decoded.tdis[1].effectiveRemainingStatKey)
  }

  @Test
  fun `tdi_config json written before filters existed still decodes`() {
    val decoded = InvertJson.decodeFromString(
      TechDebtInitiativeConfig.serializer(),
      """{"tdis":[{"id":"kotlin","title":"Migrate to Kotlin","descriptionMarkdown":"","unit":"Files",""" +
        """"completedStatKey":"kotlin_files","remainingStatKey":"java_files"}]}""",
    )
    assertEquals(unfiltered, decoded.tdis.single())
    assertEquals("java_files", decoded.tdis.single().effectiveRemainingStatKey)
  }

  @Test
  fun `tdi_config json can be read with Gson`() {
    // Some consumers read tdi_config.json with Gson, which cannot create interfaces and ignores
    // Kotlin default values.
    val json = InvertJson.encodeToString(
      TechDebtInitiativeConfig.serializer(),
      TechDebtInitiativeConfig(listOf(unfiltered, filtered)),
    )

    val decoded = Gson().fromJson(json, TechDebtInitiativeConfig::class.java)

    assertEquals(listOf(unfiltered, filtered), decoded.tdis)
    assertEquals("tdi__kotlin_payments__remaining", decoded.tdis[1].effectiveRemainingStatKey)
    assertEquals("kotlin_files", decoded.tdis[0].effectiveCompletedStatKey)
  }
}
