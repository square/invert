package com.squareup.invert.internal.report

import com.squareup.invert.internal.AggregatedCodeReferences
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ExtraMetadata
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.Stat
import com.squareup.invert.models.Stat.CodeReferencesStat.CodeReference
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatFilter
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.ValueMatcher
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.TechDebtInitiative
import com.squareup.invert.models.js.TechDebtInitiativeConfig
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TechDebtInitiativeStatsTest {

  private lateinit var testDir: File
  private val warnings = mutableListOf<String>()
  private val logger = object : InvertLogger {
    override fun lifecycle(message: String) {}
    override fun info(message: String) {}
    override fun warn(message: String) {
      warnings.add(message)
    }
  }

  private val screensMetadata = StatMetadata(
    key = "legacy_screens",
    description = "Legacy Screens",
    dataType = StatDataType.CODE_REFERENCES,
    category = "Screens",
    extras = listOf(ExtraMetadata(key = "kind", type = ExtraDataType.STRING, description = "Kind")),
  )

  private val testCountMetadata = StatMetadata(
    key = "test_count",
    description = "Test Count",
    dataType = StatDataType.NUMERIC,
  )

  private val dialogsTdi = TechDebtInitiative(
    id = "legacy_dialogs",
    title = "Legacy Dialogs",
    descriptionMarkdown = "Remove legacy dialogs",
    unit = "Screens",
    completedStatKey = null,
    remainingStatKey = screensMetadata.key,
    remainingFilter = StatFilter(extras = mapOf("kind" to ValueMatcher.equalTo("dialog"))),
  )

  private val derivedKey = "tdi__legacy_dialogs__remaining"

  @BeforeTest
  fun setup() {
    testDir = File("build/test-reports/tdi-stats-test-${System.nanoTime()}")
    testDir.mkdirs()
  }

  @AfterTest
  fun cleanup() {
    testDir.deleteRecursively()
  }

  @Test
  fun `derived stat holds only matching code references in each module`() {
    val result = TechDebtInitiativeStats.addDerivedStats(collectedData(), listOf(dialogsTdi), logger)

    assertEquals(setOf(derivedKey), result.derivedStatKeys)
    val byModule = result.collectedData.collectedStats.associateBy { it.path }
    assertEquals(
      listOf("app/A.kt"),
      (byModule.getValue(":app").stats.getValue(derivedKey) as Stat.CodeReferencesStat).value.map { it.filePath },
    )
    assertEquals(
      emptyList(),
      (byModule.getValue(":lib").stats.getValue(derivedKey) as Stat.CodeReferencesStat).value,
      "A module with the source stat but no matches keeps an empty derived stat",
    )
    assertFalse(derivedKey in byModule.getValue(":none").stats, "No source stat, no derived stat")

    val derivedMetadata = byModule.getValue(":app").statInfos.getValue(derivedKey)
    assertEquals(StatDataType.CODE_REFERENCES, derivedMetadata.dataType)
    assertEquals("Legacy Dialogs (Remaining)", derivedMetadata.title)
    assertEquals(TechDebtInitiativeStats.CATEGORY, derivedMetadata.category)
    assertEquals(screensMetadata.extras, derivedMetadata.extras)
    assertTrue(derivedMetadata.description.contains("`legacy_screens`"))
  }

  @Test
  fun `initiatives without filters add nothing`() {
    val data = collectedData()
    val result = TechDebtInitiativeStats.addDerivedStats(
      data,
      listOf(dialogsTdi.copy(remainingFilter = null)),
      logger,
    )
    assertEquals(emptySet(), result.derivedStatKeys)
    assertEquals(data, result.collectedData)
  }

  @Test
  fun `derived stat uses the filter key when set`() {
    val versionedKey = "legacy_screens.kind_dialog.v2"
    val tdi = dialogsTdi.copy(remainingFilter = dialogsTdi.remainingFilter!!.copy(key = versionedKey))
    val result = TechDebtInitiativeStats.addDerivedStats(collectedData(), listOf(tdi), logger)

    assertEquals(setOf(versionedKey), result.derivedStatKeys)
    assertEquals(versionedKey, tdi.effectiveRemainingStatKey)
    val app = result.collectedData.collectedStats.single { it.path == ":app" }
    assertEquals(
      listOf("app/A.kt"),
      (app.stats.getValue(versionedKey) as Stat.CodeReferencesStat).value.map { it.filePath },
    )
    assertFalse(derivedKey in app.stats, "The default key is not used")
  }

  @Test
  fun `filter on a numeric stat fails`() {
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(
        collectedData(),
        listOf(dialogsTdi.copy(remainingStatKey = testCountMetadata.key)),
        logger,
      )
    }
    assertTrue(error.message.orEmpty().contains("'test_count' is a NUMERIC stat"), error.message)
  }

  @Test
  fun `invalid filters fail with every problem listed`() {
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(
        collectedData(),
        listOf(
          dialogsTdi.copy(remainingFilter = StatFilter(extras = mapOf("knid" to ValueMatcher.equalTo("dialog")))),
          dialogsTdi.copy(id = "no_completed", completedFilter = dialogsTdi.remainingFilter),
        ),
        logger,
      )
    }
    val message = error.message.orEmpty()
    assertTrue(message.contains("TDI 'legacy_dialogs' remainingFilter: Extra 'knid' is not declared"), message)
    assertTrue(message.contains("TDI 'no_completed' has a completedFilter but no completedStatKey"), message)
  }

  @Test
  fun `duplicate initiative ids fail`() {
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(collectedData(), listOf(dialogsTdi, dialogsTdi), logger)
    }
    assertTrue(error.message.orEmpty().contains("produced more than once"), error.message)
  }

  @Test
  fun `duplicate filter keys fail`() {
    val filter = dialogsTdi.remainingFilter!!.copy(key = "dialogs.v1")
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(
        collectedData(),
        listOf(
          dialogsTdi.copy(remainingFilter = filter),
          dialogsTdi.copy(id = "other_dialogs", remainingFilter = filter),
        ),
        logger,
      )
    }
    val message = error.message.orEmpty()
    assertTrue(message.contains("'dialogs.v1' is produced more than once"), message)
    assertTrue(message.contains("legacy_dialogs, other_dialogs"), message)
  }

  @Test
  fun `filter key used by a collected stat fails`() {
    val tdi = dialogsTdi.copy(remainingFilter = dialogsTdi.remainingFilter!!.copy(key = screensMetadata.key))
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(collectedData(), listOf(tdi), logger)
    }
    assertTrue(error.message.orEmpty().contains("already used by a collected stat"), error.message)
  }

  @Test
  fun `derived key used by a collected stat fails`() {
    val data = collectedData(extraStats = mapOf(screensMetadata.copy(key = derivedKey) to Stat.CodeReferencesStat(emptyList())))
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(data, listOf(dialogsTdi), logger)
    }
    assertTrue(error.message.orEmpty().contains("already used by a collected stat"), error.message)
  }

  @Test
  fun `missing source stat warns and is skipped`() {
    val result = TechDebtInitiativeStats.addDerivedStats(
      collectedData(),
      listOf(dialogsTdi.copy(remainingStatKey = "not_collected")),
      logger,
    )
    assertEquals(emptySet(), result.derivedStatKeys)
    assertTrue(warnings.single().contains("'not_collected' was not collected"), warnings.toString())
  }

  @Test
  fun `invalid filter on a missing source stat still fails`() {
    val error = assertFailsWith<IllegalArgumentException> {
      TechDebtInitiativeStats.addDerivedStats(
        collectedData(),
        listOf(
          dialogsTdi.copy(
            remainingStatKey = "not_collected",
            remainingFilter = StatFilter(extras = mapOf("kind" to ValueMatcher.regex("(unclosed")), key = ""),
          ),
        ),
        logger,
      )
    }
    val message = error.message.orEmpty()
    assertTrue(message.contains("blank 'key'"), message)
    assertTrue(message.contains("Invalid regular expression '(unclosed'"), message)
  }

  @Test
  fun `writeProjectData publishes totals, exports and effective keys for derived stats`() {
    InvertReportWriter(logger, testDir).writeProjectData(
      reportMetadata = metadata(),
      collectedData = collectedData(),
      historicalData = emptySet(),
      techDebtInitiatives = listOf(dialogsTdi),
    )

    val totals = InvertJson.decodeFromString(
      CollectedStatTotalsJsReportModel.serializer(),
      File(testDir, "json/stat_totals.json").readText(),
    ).statTotals
    assertEquals(3, totals.getValue("legacy_screens").total)
    assertEquals(1, totals.getValue(derivedKey).total)
    assertEquals(mapOf("team1" to 1), totals.getValue(derivedKey).totalByOwner)

    val exported = InvertJson.decodeFromString(
      AggregatedCodeReferences.serializer(),
      File(testDir, "json/code_references_$derivedKey.json").readText(),
    )
    assertEquals(listOf("app/A.kt"), exported.values.map { it.filePath })
    assertTrue(File(testDir, "sarif/code_references_$derivedKey.sarif").isFile)

    val aggregateSarif = testDir.walk().single { it.name == "stats.sarif" }
    assertTrue(aggregateSarif.readText().contains("legacy_screens"))
    assertFalse(aggregateSarif.readText().contains(derivedKey), "Derived stats repeat source findings")

    val tdiConfig = InvertJson.decodeFromString(
      TechDebtInitiativeConfig.serializer(),
      File(testDir, "json/tdi_config.json").readText(),
    )
    assertEquals(derivedKey, tdiConfig.tdis.single().effectiveRemainingStatKey)
    assertTrue(File(testDir, "json/tdi_config.json").readText().contains("\"effectiveRemainingStatKey\""))
  }

  @Test
  fun `writeProjectData exports a derived stat with no matches`() {
    val tdi = dialogsTdi.copy(remainingFilter = StatFilter(extras = mapOf("kind" to ValueMatcher.equalTo("none"))))
    InvertReportWriter(logger, testDir).writeProjectData(
      reportMetadata = metadata(),
      collectedData = collectedData(),
      historicalData = emptySet(),
      techDebtInitiatives = listOf(tdi),
    )

    val totals = InvertJson.decodeFromString(
      CollectedStatTotalsJsReportModel.serializer(),
      File(testDir, "json/stat_totals.json").readText(),
    ).statTotals
    assertEquals(0, totals.getValue(derivedKey).total, "A finished initiative reads 0, not missing")
    assertTrue(File(testDir, "json/code_references_$derivedKey.json").isFile)
    assertTrue(File(testDir, "sarif/code_references_$derivedKey.sarif").isFile)
  }

  private fun collectedData(
    extraStats: Map<StatMetadata, Stat> = emptyMap(),
  ): InvertCombinedCollectedData {
    fun project(path: String, stats: Map<StatMetadata, Stat>) = CollectedStatsForProject(
      path = path,
      statInfos = stats.keys.associateBy { it.key },
      stats = stats.mapKeys { it.key.key },
    )
    return InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = setOf(
        CollectedOwnershipForProject(path = ":app", ownerName = "team1"),
        CollectedOwnershipForProject(path = ":lib", ownerName = "team2"),
      ),
      collectedStats = setOf(
        project(
          ":app",
          mapOf(
            screensMetadata to Stat.CodeReferencesStat(
              listOf(reference("app/A.kt", "dialog"), reference("app/B.kt", "layout"))
            ),
            testCountMetadata to Stat.NumericStat(3, "unit"),
          ) + extraStats,
        ),
        project(
          ":lib",
          mapOf(
            screensMetadata to Stat.CodeReferencesStat(listOf(reference("lib/C.kt", null))),
            testCountMetadata to Stat.NumericStat(5, "ui"),
          ),
        ),
        project(":none", emptyMap()),
      ),
      collectedPlugins = emptySet(),
    )
  }

  private fun reference(filePath: String, kind: String?) = CodeReference(
    filePath = filePath,
    startLine = 1,
    endLine = 1,
    extras = listOfNotNull(kind?.let { "kind" to it }).toMap(),
  )

  private fun metadata() = MetadataJsReportModel(
    artifactRepositories = emptyList(),
    branchName = "main",
    buildSystem = BuildSystem.GRADLE,
    currentTime = 0,
    currentTimeFormatted = "",
    latestCommitGitSha = "abc123",
    latestCommitTime = 0,
    latestCommitTimeFormatted = "",
    tagName = null,
    timezoneId = "UTC",
    remoteRepoGit = "git@github.com:test/test.git",
    remoteRepoUrl = "https://github.com/test/test",
    owners = AllOwners(ownerToDetails = emptyMap()),
  )
}
