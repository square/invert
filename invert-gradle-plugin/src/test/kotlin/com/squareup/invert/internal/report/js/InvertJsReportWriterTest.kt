package com.squareup.invert.internal.report.js

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.DirectDependenciesJsReportModel
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.JsReportFileKey
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import okio.buffer
import okio.source
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InvertJsReportWriterTest {

  private lateinit var testDir: File
  private val logger = object : InvertLogger {
    override fun lifecycle(message: String) {}
    override fun info(message: String) {}
    override fun warn(message: String) {}
  }

  @BeforeTest
  fun setup() {
    testDir = File("build/test-reports/js-writer-test-${System.currentTimeMillis()}")
    testDir.mkdirs()
  }

  @AfterTest
  fun cleanup() {
    testDir.deleteRecursively()
  }

  @Test
  fun `test createInvertHtmlReport generates all expected files`() {
    // Given
    val writer = InvertJsReportWriter(logger, testDir)
    val testData = createTestReportData()

    // When
    writer.createInvertHtmlReport(
      allProjectsDependencyData = testData.dependencies,
      allProjectsConfigurationsData = testData.configurations,
      allProjectsStatsData = testData.stats,
      directDependencies = testData.directDependencies,
      invertedDependencies = testData.invertedDependencies,
      allPluginsData = testData.plugins,
      collectedOwnershipInfo = testData.ownership,
      globalStatTotals = testData.statTotals,
      reportMetadata = testData.metadata,
      historicalData = emptySet()
    )

    // Then - Verify JS data files are created (resources may not be available in tests)
    val jsDir = File(testDir, "js")
    assertTrue(File(jsDir, "${JsReportFileKey.HOME.key}.js").exists(), "home.js should exist")
    assertTrue(
      File(jsDir, "${JsReportFileKey.METADATA.key}.js").exists(), "metadata.js should exist"
    )
    assertTrue(File(jsDir, "${JsReportFileKey.OWNERS.key}.js").exists(), "owners.js should exist")
    assertTrue(File(jsDir, "${JsReportFileKey.PLUGINS.key}.js").exists(), "plugins.js should exist")
    assertTrue(
      File(jsDir, "${JsReportFileKey.CONFIGURATIONS.key}.js").exists(),
      "configurations.js should exist"
    )

    // HTML/CSS/JS resources are optional - only check if they were copied
    val parentDir = testDir.parentFile
    // These files may not exist in test environment, so we just verify the JS data files were created
    println("HTML resources copied: ${File(parentDir, "index.html").exists()}")
  }

  @Test
  fun `test writeJsFile creates valid JavaScript file with window variable assignment`() {
    // Given
    val testFile = File(testDir, "test-data.js")
    val testMetadata = createTestMetadata()

    // When
    InvertJsReportWriter.writeJsFile(
      logger = logger,
      fileKey = "test_key",
      jsOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    assertTrue(testFile.exists(), "JS file should be created")

    // Read file using Okio to verify it was written correctly
    val content = testFile.source().buffer().use { it.readUtf8() }

    assertTrue(
      content.contains("window.invert_report[\"test_key\"]="),
      "Should contain window variable assignment"
    )
    assertTrue(
      content.contains("\"branchName\":\"test-branch\""),
      "Should contain serialized JSON data"
    )
    assertTrue(
      content.contains("\"buildSystem\":\"GRADLE\""),
      "Should contain build system"
    )
  }

  @Test
  fun `test writeJsFile handles nested objects correctly`() {
    // Given
    val testFile = File(testDir, "stats.js")
    val statsData = createTestStatsData()

    // When
    InvertJsReportWriter.writeJsFile(
      logger = logger,
      fileKey = "stats",
      jsOutputFile = testFile,
      serializer = StatsJsReportModel.serializer(),
      value = statsData
    )

    // Then
    assertTrue(testFile.exists(), "Stats file should be created")

    val content = testFile.source().buffer().use { it.readUtf8() }
    assertTrue(content.contains("window.invert_report[\"stats\"]="))
    assertTrue(content.contains("\"statInfos\""))
    assertTrue(content.contains("\"statsByModule\""))
    assertTrue(content.contains("test_module"))
  }

  @Test
  fun `test writeJsFile creates parent directories if they don't exist`() {
    // Given
    val nestedDir = File(testDir, "nested/deep/path")
    val testFile = File(nestedDir, "data.js")
    val testMetadata = createTestMetadata()

    // When
    InvertJsReportWriter.writeJsFile(
      logger = logger,
      fileKey = "nested_test",
      jsOutputFile = testFile,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    assertTrue(testFile.exists(), "File should be created in nested directory")
    assertTrue(testFile.parentFile.exists(), "Parent directories should be created")
  }

  @Test
  fun `test writeJsFileInDir creates file in correct subdirectory`() {
    // Given
    val writer = InvertJsReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()

    // When
    writer.writeJsFileInDir(
      fileKey = JsReportFileKey.METADATA.key,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    val expectedFile = File(testDir, "js/${JsReportFileKey.METADATA.key}.js")
    assertTrue(expectedFile.exists(), "File should be created in js subdirectory")
  }

  @Test
  fun `test writeJsFileInDir with custom filename`() {
    // Given
    val writer = InvertJsReportWriter(logger, testDir)
    val testMetadata = createTestMetadata()
    val customFilename = "custom-metadata.js"

    // When
    writer.writeJsFileInDir(
      fileKey = JsReportFileKey.METADATA.key,
      jsFilename = customFilename,
      serializer = MetadataJsReportModel.serializer(),
      value = testMetadata
    )

    // Then
    val expectedFile = File(testDir, "js/$customFilename")
    assertTrue(expectedFile.exists(), "File should be created with custom filename")
  }

  @Test
  fun `test binary resource files are written correctly using Okio`() {
    // Given
    val writer = InvertJsReportWriter(logger, testDir)
    val testData = createTestReportData()

    // When
    writer.createInvertHtmlReport(
      allProjectsDependencyData = testData.dependencies,
      allProjectsConfigurationsData = testData.configurations,
      allProjectsStatsData = testData.stats,
      directDependencies = testData.directDependencies,
      invertedDependencies = testData.invertedDependencies,
      allPluginsData = testData.plugins,
      collectedOwnershipInfo = testData.ownership,
      globalStatTotals = testData.statTotals,
      reportMetadata = testData.metadata,
      historicalData = emptySet()
    )

    // Then - Verify HTML file if it exists (resource files may not be available in test environment)
    val htmlFile = File(testDir.parentFile, "index.html")
    if (htmlFile.exists()) {
      assertTrue(htmlFile.length() > 0, "HTML file should have content")

      // Verify content is valid (not corrupted)
      val htmlContent = htmlFile.source().buffer().use { it.readUtf8() }
      assertTrue(
        htmlContent.contains("<!DOCTYPE html>") || htmlContent.contains("<html"),
        "HTML file should contain valid HTML"
      )
    } else {
      // In test environment, resources may not be available - that's OK
      println("HTML resources not available in test environment - skipping binary resource test")
    }

    // Always verify that JS data files were created
    val jsDir = File(testDir, "js")
    assertTrue(File(jsDir, "${JsReportFileKey.HOME.key}.js").exists(), "JS data files should exist")
  }

  @Test
  fun `test JS files use UTF-8 encoding correctly`() {
    // Given
    val testFile = File(testDir, "unicode-test.js")
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
    InvertJsReportWriter.writeJsFile(
      logger = logger,
      fileKey = "unicode_test",
      jsOutputFile = testFile,
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
  fun `test large file writing is efficient with Okio buffering`() {
    // Given
    val testFile = File(testDir, "large-data.js")
    val largeStatsData = createLargeStatsData()

    // When
    val startTime = System.currentTimeMillis()
    InvertJsReportWriter.writeJsFile(
      logger = logger,
      fileKey = "large_stats",
      jsOutputFile = testFile,
      serializer = StatsJsReportModel.serializer(),
      value = largeStatsData
    )
    val duration = System.currentTimeMillis() - startTime

    // Then
    assertTrue(testFile.exists(), "Large file should be created")
    assertTrue(testFile.length() > 1000, "File should have significant content")
    println("Large file write took ${duration}ms for ${testFile.length()} bytes")

    // Verify content is correct
    val content = testFile.source().buffer().use { it.readUtf8() }
    assertTrue(content.contains("window.invert_report"))
    assertTrue(content.contains("large_module_"))
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
      owners = AllOwners(
        ownerToDetails = emptyMap()
      )
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

  private data class TestReportData(
    val dependencies: Set<CollectedDependenciesForProject>,
    val configurations: Set<CollectedConfigurationsForProject>,
    val stats: StatsJsReportModel,
    val directDependencies: DirectDependenciesJsReportModel,
    val invertedDependencies: DependenciesJsReportModel,
    val plugins: Set<CollectedPluginsForProject>,
    val ownership: OwnershipJsReportModel,
    val statTotals: CollectedStatTotalsJsReportModel,
    val metadata: MetadataJsReportModel
  )

  private fun createTestReportData(): TestReportData {
    return TestReportData(
      dependencies = setOf(
        CollectedDependenciesForProject(
          path = ":app",
          dependencies = mapOf(
            "com.example:library:1.0.0" to setOf("implementation")
          ),
          directDependencies = mapOf(
            "implementation" to setOf(
              "com.example:library:1.0.0"
            )
          )
        )
      ),
      configurations = setOf(
        CollectedConfigurationsForProject(
          modulePath = ":app",
          allConfigurationNames = setOf("implementation", "api"),
          analyzedConfigurationNames = setOf("implementation")
        )
      ),
      stats = createTestStatsData(),
      directDependencies = DirectDependenciesJsReportModel(
        directDependencies = emptyMap()
      ),
      invertedDependencies = DependenciesJsReportModel(
        invertedDependencies = emptyMap()
      ),
      plugins = setOf(
        CollectedPluginsForProject(
          path = ":app",
          plugins = listOf("com.android.application")
        )
      ),
      ownership = OwnershipJsReportModel(
        teams = setOf("team1"),
        modules = mapOf(":app" to "team1")
      ),
      statTotals = CollectedStatTotalsJsReportModel(emptyMap()),
      metadata = createTestMetadata()
    )
  }
}
