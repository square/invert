package com.squareup.invert.internal.report

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.TechDebtInitiative
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InvertReportWriterTest {

  private lateinit var testDir: File
  private val logger = object : InvertLogger {
    override fun lifecycle(message: String) {}
    override fun info(message: String) {}
    override fun warn(message: String) {}
  }

  @BeforeTest
  fun setup() {
    testDir = File("build/test-reports/report-writer-test-${System.currentTimeMillis()}")
    testDir.mkdirs()
  }

  @AfterTest
  fun cleanup() {
    testDir.deleteRecursively()
  }

  @Test
  fun `test writeProjectData creates individual JSON files for CodeReference stats`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithCodeReferences()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val jsonDir = File(testDir, "json")
    assertTrue(jsonDir.exists(), "JSON directory should exist")

    val codeReferencesFile = File(jsonDir, "code_references_test_code_ref_stat.json")
    assertTrue(codeReferencesFile.exists(), "Code references JSON file should be created")
    assertTrue(codeReferencesFile.length() > 0, "Code references JSON file should have content")

    // Verify content
    val content = codeReferencesFile.readText()
    assertTrue(content.contains("test_code_ref_stat"), "Should contain stat key")
    assertTrue(content.contains("test.kt"), "Should contain file path")
    assertTrue(content.contains("test code"), "Should contain code snippet")
  }

  @Test
  fun `test writeProjectData creates individual SARIF files for CodeReference stats`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithCodeReferences()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val sarifDir = File(testDir, "sarif")
    assertTrue(sarifDir.exists(), "SARIF directory should exist")

    val sarifFile = File(sarifDir, "code_references_test_code_ref_stat.sarif")
    assertTrue(sarifFile.exists(), "Code references SARIF file should be created")
    assertTrue(sarifFile.length() > 0, "Code references SARIF file should have content")

    // Verify SARIF content
    val content = sarifFile.readText()
    assertTrue(content.contains("\"id\":\"test_code_ref_stat\""), "Should contain rule ID")
    assertTrue(content.contains("\"uri\":\"test.kt\""), "Should contain file path")
    assertTrue(content.contains("\"text\":\"test code\""), "Should contain code snippet")
  }

  @Test
  fun `test writeProjectData includes module and owner in code reference extras`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithCodeReferences()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val jsonDir = File(testDir, "json")
    val codeReferencesFile = File(jsonDir, "code_references_test_code_ref_stat.json")
    val content = codeReferencesFile.readText()

    // Verify module and owner are included in extras
    assertTrue(content.contains("\"module\""), "Should contain module extra")
    assertTrue(content.contains(":app"), "Should contain module path")
    assertTrue(content.contains("\"owner\""), "Should contain owner extra")
    assertTrue(content.contains("team1"), "Should contain owner name")
  }

  @Test
  fun `test writeProjectData writes tdi_config json when tech debt initiatives are provided`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithCodeReferences()
    val techDebtInitiatives = listOf(
      TechDebtInitiative(
        id = "tdi-001",
        title = "Migrate to Kotlin",
        descriptionMarkdown = "Migrate all Java files to Kotlin",
        unit = "files",
        completedStatKey = "kotlin_files",
        remainingStatKey = "java_files"
      )
    )

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = techDebtInitiatives
    )

    // Then
    val jsonDir = File(testDir, "json")
    val tdiConfigFile = File(jsonDir, "tdi_config.json")
    assertTrue(tdiConfigFile.exists(), "TDI config file should be created")
    assertTrue(tdiConfigFile.length() > 0, "TDI config file should have content")

    // Verify content
    val content = tdiConfigFile.readText()
    assertTrue(content.contains("tdi-001"), "Should contain TDI ID")
    assertTrue(content.contains("Migrate to Kotlin"), "Should contain TDI title")
    assertTrue(content.contains("kotlin_files"), "Should contain completed stat key")
    assertTrue(content.contains("java_files"), "Should contain remaining stat key")
  }

  @Test
  fun `test writeProjectData does not write tdi_config json when no tech debt initiatives`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithCodeReferences()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val jsonDir = File(testDir, "json")
    val tdiConfigFile = File(jsonDir, "tdi_config.json")
    assertFalse(tdiConfigFile.exists(), "TDI config file should not be created when no initiatives provided")
  }

  @Test
  fun `test writeProjectData writes multiple TDIs correctly`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithCodeReferences()
    val techDebtInitiatives = listOf(
      TechDebtInitiative(
        id = "tdi-001",
        title = "Migrate to Kotlin",
        descriptionMarkdown = "Migrate all Java files to Kotlin",
        unit = "files",
        completedStatKey = "kotlin_files",
        remainingStatKey = "java_files"
      ),
      TechDebtInitiative(
        id = "tdi-002",
        title = "Remove deprecated APIs",
        descriptionMarkdown = "Remove all usages of deprecated APIs",
        unit = "usages",
        completedStatKey = null,
        remainingStatKey = "deprecated_api_usages"
      )
    )

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = techDebtInitiatives
    )

    // Then
    val jsonDir = File(testDir, "json")
    val tdiConfigFile = File(jsonDir, "tdi_config.json")
    assertTrue(tdiConfigFile.exists(), "TDI config file should be created")

    val content = tdiConfigFile.readText()
    assertTrue(content.contains("tdi-001"), "Should contain first TDI")
    assertTrue(content.contains("tdi-002"), "Should contain second TDI")
    assertTrue(content.contains("Migrate to Kotlin"), "Should contain first TDI title")
    assertTrue(content.contains("Remove deprecated APIs"), "Should contain second TDI title")
  }

  @Test
  fun `test writeProjectData handles multiple code reference stats`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithMultipleCodeReferences()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val jsonDir = File(testDir, "json")
    val sarifDir = File(testDir, "sarif")

    // Verify multiple stat files are created
    val codeRefFile1 = File(jsonDir, "code_references_deprecated_api.json")
    val codeRefFile2 = File(jsonDir, "code_references_todo_comments.json")
    assertTrue(codeRefFile1.exists(), "First code references JSON file should exist")
    assertTrue(codeRefFile2.exists(), "Second code references JSON file should exist")

    val sarifFile1 = File(sarifDir, "code_references_deprecated_api.sarif")
    val sarifFile2 = File(sarifDir, "code_references_todo_comments.sarif")
    assertTrue(sarifFile1.exists(), "First code references SARIF file should exist")
    assertTrue(sarifFile2.exists(), "Second code references SARIF file should exist")
  }

  @Test
  fun `test writeProjectData only creates files for CodeReference stats`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithMixedStats()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val jsonDir = File(testDir, "json")

    // Should create file for code reference stat
    val codeRefFile = File(jsonDir, "code_references_test_code_ref_stat.json")
    assertTrue(codeRefFile.exists(), "Code references JSON file should exist")

    // Should NOT create file for numeric stat
    val numericStatFile = File(jsonDir, "code_references_test_numeric_stat.json")
    assertFalse(numericStatFile.exists(), "Should not create code references file for numeric stats")
  }

  @Test
  fun `test writeProjectData handles code references across multiple modules`() {
    // Given
    val writer = InvertReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testCollectedData = createCollectedDataWithMultipleModules()

    // When
    writer.writeProjectData(
      reportMetadata = testMetadata,
      collectedData = testCollectedData,
      historicalData = emptySet(),
      techDebtInitiatives = emptyList()
    )

    // Then
    val jsonDir = File(testDir, "json")
    val codeReferencesFile = File(jsonDir, "code_references_test_code_ref_stat.json")
    val content = codeReferencesFile.readText()

    // Verify both modules are represented
    assertTrue(content.contains(":app"), "Should contain app module")
    assertTrue(content.contains(":lib"), "Should contain lib module")
    assertTrue(content.contains("team1"), "Should contain team1 owner")
    assertTrue(content.contains("team2"), "Should contain team2 owner")
  }

  // Helper functions

  private fun createTestMetadata(): MetadataJsReportModel {
    return MetadataJsReportModel(
      artifactRepositories = listOf("https://repo.maven.apache.org/maven2"),
      branchName = "test-branch",
      buildSystem = BuildSystem.GRADLE,
      currentTime = System.currentTimeMillis(),
      currentTimeFormatted = "2024-01-01T12:00:00Z",
      latestCommitGitSha = "abc123def456",
      latestCommitTime = System.currentTimeMillis() - 1000000,
      latestCommitTimeFormatted = "2024-01-01T11:00:00Z",
      tagName = "v1.0.0",
      timezoneId = "UTC",
      remoteRepoGit = "git@github.com:test/test.git",
      remoteRepoUrl = "https://github.com/test/test",
      owners = AllOwners(ownerToDetails = emptyMap())
    )
  }

  private fun createCollectedDataWithCodeReferences(): InvertCombinedCollectedData {
    val codeRefStatMetadata = StatMetadata(
      key = "test_code_ref_stat",
      description = "Test Code Reference Stat",
      dataType = StatDataType.CODE_REFERENCES
    )

    val codeReferences = listOf(
      Stat.CodeReferencesStat.CodeReference(
        filePath = "test.kt",
        startLine = 1,
        endLine = 10,
        code = "test code"
      )
    )

    return InvertCombinedCollectedData(
      collectedConfigurations = setOf(
        CollectedConfigurationsForProject(
          modulePath = ":app",
          allConfigurationNames = setOf("implementation"),
          analyzedConfigurationNames = setOf("implementation")
        )
      ),
      collectedDependencies = setOf(
        CollectedDependenciesForProject(
          path = ":app",
          dependencies = emptyMap(),
          directDependencies = emptyMap()
        )
      ),
      collectedOwners = setOf(
        CollectedOwnershipForProject(
          path = ":app",
          ownerName = "team1"
        )
      ),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = ":app",
          stats = mapOf(
            codeRefStatMetadata.key to Stat.CodeReferencesStat(codeReferences)
          ),
          statInfos = mapOf(
            codeRefStatMetadata.key to codeRefStatMetadata
          )
        )
      ),
      collectedPlugins = setOf(
        CollectedPluginsForProject(
          path = ":app",
          plugins = listOf("com.android.application")
        )
      )
    )
  }

  private fun createCollectedDataWithMultipleCodeReferences(): InvertCombinedCollectedData {
    val deprecatedApiStatMetadata = StatMetadata(
      key = "deprecated_api",
      description = "Deprecated API Usages",
      dataType = StatDataType.CODE_REFERENCES
    )

    val todoCommentsStatMetadata = StatMetadata(
      key = "todo_comments",
      description = "TODO Comments",
      dataType = StatDataType.CODE_REFERENCES
    )

    val deprecatedApiReferences = listOf(
      Stat.CodeReferencesStat.CodeReference(
        filePath = "deprecated.kt",
        startLine = 1,
        endLine = 5,
        code = "oldApi()"
      )
    )

    val todoReferences = listOf(
      Stat.CodeReferencesStat.CodeReference(
        filePath = "todo.kt",
        startLine = 10,
        endLine = 10,
        code = "// TODO: Fix this"
      )
    )

    return InvertCombinedCollectedData(
      collectedConfigurations = setOf(
        CollectedConfigurationsForProject(
          modulePath = ":app",
          allConfigurationNames = setOf("implementation"),
          analyzedConfigurationNames = setOf("implementation")
        )
      ),
      collectedDependencies = setOf(
        CollectedDependenciesForProject(
          path = ":app",
          dependencies = emptyMap(),
          directDependencies = emptyMap()
        )
      ),
      collectedOwners = setOf(
        CollectedOwnershipForProject(
          path = ":app",
          ownerName = "team1"
        )
      ),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = ":app",
          stats = mapOf(
            deprecatedApiStatMetadata.key to Stat.CodeReferencesStat(deprecatedApiReferences),
            todoCommentsStatMetadata.key to Stat.CodeReferencesStat(todoReferences)
          ),
          statInfos = mapOf(
            deprecatedApiStatMetadata.key to deprecatedApiStatMetadata,
            todoCommentsStatMetadata.key to todoCommentsStatMetadata
          )
        )
      ),
      collectedPlugins = setOf(
        CollectedPluginsForProject(
          path = ":app",
          plugins = listOf("com.android.application")
        )
      )
    )
  }

  private fun createCollectedDataWithMixedStats(): InvertCombinedCollectedData {
    val codeRefStatMetadata = StatMetadata(
      key = "test_code_ref_stat",
      description = "Test Code Reference Stat",
      dataType = StatDataType.CODE_REFERENCES
    )

    val numericStatMetadata = StatMetadata(
      key = "test_numeric_stat",
      description = "Test Numeric Stat",
      dataType = StatDataType.NUMERIC
    )

    val codeReferences = listOf(
      Stat.CodeReferencesStat.CodeReference(
        filePath = "test.kt",
        startLine = 1,
        endLine = 10,
        code = "test code"
      )
    )

    return InvertCombinedCollectedData(
      collectedConfigurations = setOf(
        CollectedConfigurationsForProject(
          modulePath = ":app",
          allConfigurationNames = setOf("implementation"),
          analyzedConfigurationNames = setOf("implementation")
        )
      ),
      collectedDependencies = setOf(
        CollectedDependenciesForProject(
          path = ":app",
          dependencies = emptyMap(),
          directDependencies = emptyMap()
        )
      ),
      collectedOwners = setOf(
        CollectedOwnershipForProject(
          path = ":app",
          ownerName = "team1"
        )
      ),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = ":app",
          stats = mapOf(
            codeRefStatMetadata.key to Stat.CodeReferencesStat(codeReferences),
            numericStatMetadata.key to Stat.NumericStat(42)
          ),
          statInfos = mapOf(
            codeRefStatMetadata.key to codeRefStatMetadata,
            numericStatMetadata.key to numericStatMetadata
          )
        )
      ),
      collectedPlugins = setOf(
        CollectedPluginsForProject(
          path = ":app",
          plugins = listOf("com.android.application")
        )
      )
    )
  }

  private fun createCollectedDataWithMultipleModules(): InvertCombinedCollectedData {
    val codeRefStatMetadata = StatMetadata(
      key = "test_code_ref_stat",
      description = "Test Code Reference Stat",
      dataType = StatDataType.CODE_REFERENCES
    )

    val appCodeReferences = listOf(
      Stat.CodeReferencesStat.CodeReference(
        filePath = "app/MainActivity.kt",
        startLine = 1,
        endLine = 10,
        code = "app code"
      )
    )

    val libCodeReferences = listOf(
      Stat.CodeReferencesStat.CodeReference(
        filePath = "lib/Library.kt",
        startLine = 5,
        endLine = 15,
        code = "lib code"
      )
    )

    return InvertCombinedCollectedData(
      collectedConfigurations = setOf(
        CollectedConfigurationsForProject(
          modulePath = ":app",
          allConfigurationNames = setOf("implementation"),
          analyzedConfigurationNames = setOf("implementation")
        ),
        CollectedConfigurationsForProject(
          modulePath = ":lib",
          allConfigurationNames = setOf("implementation"),
          analyzedConfigurationNames = setOf("implementation")
        )
      ),
      collectedDependencies = setOf(
        CollectedDependenciesForProject(
          path = ":app",
          dependencies = emptyMap(),
          directDependencies = emptyMap()
        ),
        CollectedDependenciesForProject(
          path = ":lib",
          dependencies = emptyMap(),
          directDependencies = emptyMap()
        )
      ),
      collectedOwners = setOf(
        CollectedOwnershipForProject(
          path = ":app",
          ownerName = "team1"
        ),
        CollectedOwnershipForProject(
          path = ":lib",
          ownerName = "team2"
        )
      ),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = ":app",
          stats = mapOf(
            codeRefStatMetadata.key to Stat.CodeReferencesStat(appCodeReferences)
          ),
          statInfos = mapOf(
            codeRefStatMetadata.key to codeRefStatMetadata
          )
        ),
        CollectedStatsForProject(
          path = ":lib",
          stats = mapOf(
            codeRefStatMetadata.key to Stat.CodeReferencesStat(libCodeReferences)
          ),
          statInfos = mapOf(
            codeRefStatMetadata.key to codeRefStatMetadata
          )
        )
      ),
      collectedPlugins = setOf(
        CollectedPluginsForProject(
          path = ":app",
          plugins = listOf("com.android.application")
        ),
        CollectedPluginsForProject(
          path = ":lib",
          plugins = listOf("com.android.library")
        )
      )
    )
  }
}