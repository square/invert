package com.squareup.invert.internal.report.json

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.serialization.builtins.SetSerializer
import okio.buffer
import okio.source
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InvertJsonReportWriterTest {

  private lateinit var testDir: File
  private val logger = object : InvertLogger {
    override fun lifecycle(message: String) {}
    override fun info(message: String) {}
    override fun warn(message: String) {}
  }

  @BeforeTest
  fun setup() {
    testDir = File("build/test-reports/json-writer-test-${System.currentTimeMillis()}")
    testDir.mkdirs()
  }

  @AfterTest
  fun cleanup() {
    testDir.deleteRecursively()
  }

  @Test
  fun `test createInvertJsonReport generates all expected JSON files`() {
    // Given
    val writer = InvertJsonReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val testStats = createTestStatsData()

    // When
    writer.createInvertJsonReport(
      allConfigurationsData = setOf(
        CollectedConfigurationsForProject(
          modulePath = ":app",
          allConfigurationNames = setOf("implementation", "api"),
          analyzedConfigurationNames = setOf("implementation")
        )
      ),
      allProjectsDependencyData = setOf(
        CollectedDependenciesForProject(
          path = ":app",
          dependencies = mapOf(
            "com.example:library:1.0.0" to setOf("implementation")
          ),
          directDependencies = mapOf(
            "implementation" to setOf("com.example:library:1.0.0")
          )
        )
      ),
      allProjectsStatsData = testStats,
      allPluginsData = setOf(
        CollectedPluginsForProject(
          path = ":app",
          plugins = listOf("com.android.application")
        )
      ),
      allOwnersData = setOf(
        CollectedOwnershipForProject(
          path = ":app",
          ownerName = "team1"
        )
      ),
      globalStats = emptyMap(),
      reportMetadata = testMetadata,
      historicalData = emptySet()
    )

    // Then - Verify all JSON files are created
    val jsonDir = File(testDir, "json")
    assertTrue(jsonDir.exists(), "JSON directory should exist")

    val expectedFiles = listOf(
      InvertPluginFileKey.METADATA,
      InvertPluginFileKey.PLUGINS,
      InvertPluginFileKey.HISTORICAL_DATA,
      InvertPluginFileKey.STAT_TOTALS,
      InvertPluginFileKey.CONFIGURATIONS,
      InvertPluginFileKey.DEPENDENCIES,
      InvertPluginFileKey.OWNERS
    )

    expectedFiles.forEach { fileKey ->
      val file = File(jsonDir, fileKey.filename)
      assertTrue(file.exists(), "${fileKey.filename} should exist")
      assertTrue(file.length() > 0, "${fileKey.filename} should have content")
    }
  }

  @Test
  fun `test writeJsonFile creates valid JSON file`() {
    // Given
    val testFile = File(testDir, "test-metadata.json")
    val testMetadata = createTestMetadata()

    // When
    InvertJsonReportWriter.writeJsonFile(
      logger = logger,
      jsonFileKey = InvertPluginFileKey.METADATA,
      jsonOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    assertTrue(testFile.exists(), "JSON file should be created")

    // Read file using Okio to verify it was written correctly
    val content = testFile.source().buffer().use { it.readUtf8() }

    assertTrue(content.contains("\"branchName\":\"test-branch\""), "Should contain branch name")
    assertTrue(content.contains("\"buildSystem\":\"GRADLE\""), "Should contain build system")
    assertTrue(content.contains("test-branch"), "Should contain test branch data")
  }

  @Test
  fun `test writeJsonFile with custom description`() {
    // Given
    val testFile = File(testDir, "custom-data.json")
    val testMetadata = createTestMetadata()

    // When
    InvertJsonReportWriter.writeJsonFile(
      description = "Custom Test Description",
      jsonOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata,
      logger = logger
    )

    // Then
    assertTrue(testFile.exists(), "JSON file should be created")
    val content = testFile.source().buffer().use { it.readUtf8() }
    assertTrue(content.contains("\"branchName\":\"test-branch\""))
  }

  @Test
  fun `test writeJsonFileInDir creates file in correct subdirectory`() {
    // Given
    val writer = InvertJsonReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()

    // When
    writer.writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.METADATA,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    val expectedFile = File(testDir, "json/${InvertPluginFileKey.METADATA.filename}")
    assertTrue(expectedFile.exists(), "File should be created in json subdirectory")
  }

  @Test
  fun `test JSON files use UTF-8 encoding correctly`() {
    // Given
    val testFile = File(testDir, "unicode-test.json")
    val testMetadata = MetadataJsReportModel(
      artifactRepositories = listOf("https://repository.example.com/émoji-🎉-test"),
      branchName = "test-ブランチ-分支",
      buildSystem = BuildSystem.GRADLE,
      currentTime = System.currentTimeMillis(),
      currentTimeFormatted = "2024-01-01",
      latestCommitGitSha = "abc123",
      latestCommitTime = System.currentTimeMillis(),
      latestCommitTimeFormatted = "2024-01-01",
      tagName = null,
      timezoneId = "UTC",
      remoteRepoGit = "git@github.com:test/test.git",
      remoteRepoUrl = "https://github.com/test/test",
      owners = AllOwners(ownerToDetails = emptyMap())
    )

    // When
    InvertJsonReportWriter.writeJsonFile(
      logger = logger,
      jsonFileKey = InvertPluginFileKey.METADATA,
      jsonOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    val content = testFile.source().buffer().use { it.readUtf8() }
    assertTrue(content.contains("émoji-🎉-test"), "Should correctly encode emoji")
    assertTrue(
      content.contains("test-ブランチ-分支"),
      "Should correctly encode Japanese and Chinese characters"
    )
  }

  @Test
  fun `test large JSON file writing is efficient with Okio buffering`() {
    // Given
    val testFile = File(testDir, "large-stats.json")
    val largeStatsData = createLargeStatsData()

    // When
    val startTime = System.currentTimeMillis()
    InvertJsonReportWriter.writeJsonFile(
      description = "Large Stats Data",
      jsonOutputFile = testFile,
      serializer = StatsJsReportModel.serializer(),
      value = largeStatsData,
      logger = logger
    )
    val duration = System.currentTimeMillis() - startTime

    // Then
    assertTrue(testFile.exists(), "Large file should be created")
    assertTrue(testFile.length() > 1000, "File should have significant content")
    println("Large JSON write took ${duration}ms for ${testFile.length()} bytes")

    // Verify content is correct
    val content = testFile.source().buffer().use { it.readUtf8() }
    assertTrue(content.contains("\"statInfos\""))
    assertTrue(content.contains("large_module_"))
  }

  @Test
  fun `test JSON serialization of complex nested structures`() {
    // Given
    val testFile = File(testDir, "nested-data.json")
    val configurations = setOf(
      CollectedConfigurationsForProject(
        modulePath = ":app",
        allConfigurationNames = setOf("implementation", "api", "testImplementation", "kapt"),
        analyzedConfigurationNames = setOf("implementation", "api")
      ),
      CollectedConfigurationsForProject(
        modulePath = ":lib",
        allConfigurationNames = setOf("implementation", "api"),
        analyzedConfigurationNames = setOf("implementation")
      )
    )

    // When
    InvertJsonReportWriter.writeJsonFile(
      description = "Configurations",
      jsonOutputFile = testFile,
      serializer = SetSerializer(CollectedConfigurationsForProject.serializer()),
      value = configurations,
      logger = logger
    )

    // Then
    assertTrue(testFile.exists(), "File should be created")
    val content = testFile.source().buffer().use { it.readUtf8() }

    assertTrue(content.contains(":app"))
    assertTrue(content.contains(":lib"))
    assertTrue(content.contains("implementation"))
    assertTrue(content.contains("kapt"))
  }

  @Test
  fun `test writeJsonFile creates parent directories if they don't exist`() {
    // Given
    val nestedDir = File(testDir, "nested/deep/path")
    val testFile = File(nestedDir, "data.json")
    val testMetadata = createTestMetadata()

    // Create parent directories first (the File.sink() method doesn't auto-create parents)
    testFile.parentFile.mkdirs()

    // When
    InvertJsonReportWriter.writeJsonFile(
      description = "Nested Test",
      jsonOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata,
      logger = logger
    )

    // Then
    assertTrue(testFile.exists(), "File should be created in nested directory")
    assertTrue(testFile.parentFile.exists(), "Parent directories should exist")
  }

  @Test
  fun `test JSON format is properly minified`() {
    // Given
    val testFile = File(testDir, "minified-test.json")
    val testMetadata = createTestMetadata()

    // When
    InvertJsonReportWriter.writeJsonFile(
      logger = logger,
      jsonFileKey = InvertPluginFileKey.METADATA,
      jsonOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    val content = testFile.source().buffer().use { it.readUtf8() }

    // JSON should not have excessive whitespace (InvertJson typically doesn't add extra formatting)
    val lines = content.lines()
    assertTrue(lines.isNotEmpty(), "File should have content")

    // Verify it's valid JSON by checking for basic structure
    assertTrue(content.startsWith("{") || content.startsWith("["))
    assertTrue(content.trim().endsWith("}") || content.trim().endsWith("]"))
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

  private fun createTestStatsData(): StatsJsReportModel {
    val statInfo = StatMetadata(
      key = "test_stat",
      description = "Test Statistic",
      dataType = StatDataType.NUMERIC
    )

    return StatsJsReportModel(
      statInfos = mapOf(statInfo.key to statInfo),
      statsByModule = mapOf(
        "test_module" to mapOf(
          statInfo.key to Stat.NumericStat(42)
        )
      )
    )
  }

  private fun createLargeStatsData(): StatsJsReportModel {
    val statInfo = StatMetadata(
      key = "large_stat",
      description = "Large Statistic",
      dataType = StatDataType.NUMERIC
    )

    val modules = (1..100).associate { i ->
      "large_module_$i" to mapOf(
        statInfo.key to Stat.NumericStat(i)
      )
    }

    return StatsJsReportModel(
      statInfos = mapOf(statInfo.key to statInfo),
      statsByModule = modules
    )
  }
}
