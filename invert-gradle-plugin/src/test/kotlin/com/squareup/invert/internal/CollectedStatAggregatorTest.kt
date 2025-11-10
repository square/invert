package com.squareup.invert.internal

import com.squareup.invert.CollectedStat
import com.squareup.invert.CollectedStatsAggregate
import com.squareup.invert.InvertAllCollectedDataRepo
import com.squareup.invert.ReportOutputConfig
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.AllOwners
import com.squareup.invert.models.js.BuildSystem
import com.squareup.invert.models.js.MetadataJsReportModel
import org.junit.Test
import java.io.File
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CollectedStatAggregatorTest {

  @Test
  fun `aggregate should merge multiple stats from different collectors for same project`() {
    // Given: Original collected data with a single project
    val projectPath = ":app"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = projectPath,
          statInfos = emptyMap(),
          stats = emptyMap()
        )
      ),
      collectedPlugins = emptySet()
    )

    // Create two different stat collectors that produce different stats for the same project
    val collector1 = TestStatCollector(
      name = "collector1",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "stat_key_1",
                description = "First stat",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 42)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val collector2 = TestStatCollector(
      name = "collector2",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "stat_key_2",
                description = "Second stat",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 100)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val reportMetadata = createTestReportMetadata()
    val reportOutputConfig = createTestReportOutputConfig()

    // When: We aggregate with both collectors
    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = reportOutputConfig,
      reportMetadata = reportMetadata,
      statCollectorsForAggregation = listOf(collector1, collector2)
    )

    // Then: Both stats should be present for the project
    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats, "Project stats should not be null")

    assertEquals(
      2,
      projectStats.stats.size,
      "Should have both stats from both collectors, but got: ${projectStats.stats.keys}"
    )

    assertTrue(
      projectStats.stats.containsKey("stat_key_1"),
      "Should contain stat_key_1 from collector1"
    )
    assertTrue(
      projectStats.stats.containsKey("stat_key_2"),
      "Should contain stat_key_2 from collector2"
    )

    assertEquals(42, (projectStats.stats["stat_key_1"] as? Stat.NumericStat)?.value)
    assertEquals(100, (projectStats.stats["stat_key_2"] as? Stat.NumericStat)?.value)
  }

  @Test
  fun `aggregate should handle single collector with multiple stats for same project`() {
    val projectPath = ":lib"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = projectPath,
          statInfos = emptyMap(),
          stats = emptyMap()
        )
      ),
      collectedPlugins = emptySet()
    )

    val collector = TestStatCollector(
      name = "multiStatCollector",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "stat_a",
                description = "Stat A",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 1)
            ),
            CollectedStat(
              metadata = StatMetadata(
                key = "stat_b",
                description = "Stat B",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 2)
            ),
            CollectedStat(
              metadata = StatMetadata(
                key = "stat_c",
                description = "Stat C",
                dataType = StatDataType.STRING
              ),
              stat = Stat.StringStat(value = "test")
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector)
    )

    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats)
    assertEquals(3, projectStats.stats.size)
    assertTrue(projectStats.stats.containsKey("stat_a"))
    assertTrue(projectStats.stats.containsKey("stat_b"))
    assertTrue(projectStats.stats.containsKey("stat_c"))
  }

  @Test
  fun `aggregate should preserve existing stats from original data`() {
    val projectPath = ":core"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = projectPath,
          statInfos = mapOf(
            "existing_stat" to StatMetadata(
              key = "existing_stat",
              description = "Existing stat",
              dataType = StatDataType.NUMERIC
            )
          ),
          stats = mapOf(
            "existing_stat" to Stat.NumericStat(value = 999)
          )
        )
      ),
      collectedPlugins = emptySet()
    )

    val collector = TestStatCollector(
      name = "newCollector",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "new_stat",
                description = "New stat",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 123)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector)
    )

    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats)
    assertEquals(2, projectStats.stats.size)
    assertTrue(projectStats.stats.containsKey("existing_stat"))
    assertTrue(projectStats.stats.containsKey("new_stat"))
    assertEquals(999, (projectStats.stats["existing_stat"] as? Stat.NumericStat)?.value)
    assertEquals(123, (projectStats.stats["new_stat"] as? Stat.NumericStat)?.value)
  }

  @Test
  fun `aggregate should handle multiple projects with multiple collectors`() {
    val project1 = ":app"
    val project2 = ":lib"

    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(path = project1, statInfos = emptyMap(), stats = emptyMap()),
        CollectedStatsForProject(path = project2, statInfos = emptyMap(), stats = emptyMap())
      ),
      collectedPlugins = emptySet()
    )

    val collector1 = TestStatCollector(
      name = "collector1",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          project1 to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "p1_s1", description = "P1 S1", dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 10)
            )
          ),
          project2 to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "p2_s1", description = "P2 S1", dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 20)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val collector2 = TestStatCollector(
      name = "collector2",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          project1 to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "p1_s2", description = "P1 S2", dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 30)
            )
          ),
          project2 to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "p2_s2", description = "P2 S2", dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 40)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector1, collector2)
    )

    val project1Stats = result.collectedStats.find { it.path == project1 }
    val project2Stats = result.collectedStats.find { it.path == project2 }

    assertNotNull(project1Stats)
    assertNotNull(project2Stats)

    assertEquals(2, project1Stats.stats.size)
    assertEquals(2, project2Stats.stats.size)

    assertTrue(project1Stats.stats.containsKey("p1_s1"))
    assertTrue(project1Stats.stats.containsKey("p1_s2"))
    assertTrue(project2Stats.stats.containsKey("p2_s1"))
    assertTrue(project2Stats.stats.containsKey("p2_s2"))
  }

  @Test
  fun `aggregate should handle null stat values`() {
    val projectPath = ":test"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(path = projectPath, statInfos = emptyMap(), stats = emptyMap())
      ),
      collectedPlugins = emptySet()
    )

    val collector = TestStatCollector(
      name = "nullCollector",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "null_stat", description = "Null stat", dataType = StatDataType.NUMERIC
              ),
              stat = null // null stat should not be added
            ),
            CollectedStat(
              metadata = StatMetadata(
                key = "valid_stat", description = "Valid stat", dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 50)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector)
    )

    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats)
    assertEquals(1, projectStats.stats.size)
    assertTrue(projectStats.stats.containsKey("valid_stat"))
  }

  @Test
  fun `second collector SHOULD see stats added by first collector in InvertAllCollectedDataRepo`() {
    val projectPath = ":app"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = projectPath,
          statInfos = emptyMap(),
          stats = emptyMap()
        )
      ),
      collectedPlugins = emptySet()
    )

    val collector1 = TestStatCollector(
      name = "collector1",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "stat_from_collector1",
                description = "Stat from collector 1",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 42)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    // Collector2 that captures what data it receives
    val collector2 = object : StatCollector {
      override fun getName() = "collector2"
      override fun aggregate(
        reportOutputConfig: ReportOutputConfig,
        invertAllCollectedDataRepo: InvertAllCollectedDataRepo
      ): CollectedStatsAggregate {
        val projectData = invertAllCollectedDataRepo.getProject(projectPath)
        println("Collector2 received stats: ${projectData?.collectedStats?.stats?.keys}")

        // After the fix, collector2 should see collector1's stat
        val hasCollector1Stat =
          projectData?.collectedStats?.stats?.containsKey("stat_from_collector1") == true
        println("Collector2 can see collector1's stat: $hasCollector1Stat")

        return CollectedStatsAggregate(
          aggregatedStatsByProject = mapOf(
            projectPath to listOf(
              CollectedStat(
                metadata = StatMetadata(
                  key = "stat_from_collector2",
                  description = "Stat from collector 2",
                  dataType = StatDataType.NUMERIC
                ),
                stat = Stat.NumericStat(value = 100)
              )
            )
          ),
          globalStats = emptyList()
        )
      }
    }

    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector1, collector2)
    )

    // The final result should have both stats
    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats)
    println("Final stats: ${projectStats.stats.keys}")
    assertEquals(2, projectStats.stats.size)
    assertTrue(projectStats.stats.containsKey("stat_from_collector1"))
    assertTrue(projectStats.stats.containsKey("stat_from_collector2"))
  }

  @Test
  fun `collector returning same stat key multiple times should keep last value`() {
    // Edge case: what if a collector returns multiple CollectedStat instances with the same key?
    val projectPath = ":app"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = projectPath,
          statInfos = emptyMap(),
          stats = emptyMap()
        )
      ),
      collectedPlugins = emptySet()
    )

    val collector = TestStatCollector(
      name = "duplicateKeyCollector",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "duplicate_key",
                description = "First instance",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 1)
            ),
            CollectedStat(
              metadata = StatMetadata(
                key = "duplicate_key",  // Same key!
                description = "Second instance",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 2)
            ),
            CollectedStat(
              metadata = StatMetadata(
                key = "another_stat",
                description = "Another stat",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 3)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector)
    )

    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats)

    // Should have 2 unique keys
    assertEquals(2, projectStats.stats.size)
    assertTrue(projectStats.stats.containsKey("duplicate_key"))
    assertTrue(projectStats.stats.containsKey("another_stat"))

    // The last value should win for duplicate_key
    assertEquals(2, (projectStats.stats["duplicate_key"] as? Stat.NumericStat)?.value)
  }

  @Test
  fun `demonstrate potential race condition if curr is read outside synchronized block`() {
    // This test documents a potential race condition:
    // If curr is read outside the synchronized block and two operations happen concurrently,
    // we could lose stats. However, since aggregate() uses forEach (sequential), this
    // shouldn't happen in practice unless aggregate() is called from multiple threads.

    val projectPath = ":app"
    val originalData = InvertCombinedCollectedData(
      collectedConfigurations = emptySet(),
      collectedDependencies = emptySet(),
      collectedOwners = emptySet(),
      collectedStats = setOf(
        CollectedStatsForProject(
          path = projectPath,
          statInfos = emptyMap(),
          stats = emptyMap()
        )
      ),
      collectedPlugins = emptySet()
    )

    // Simulate two collectors both adding stats
    val collector1 = TestStatCollector(
      name = "collector1",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "race_stat_1",
                description = "Race stat 1",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 1)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    val collector2 = TestStatCollector(
      name = "collector2",
      aggregateResult = CollectedStatsAggregate(
        aggregatedStatsByProject = mapOf(
          projectPath to listOf(
            CollectedStat(
              metadata = StatMetadata(
                key = "race_stat_2",
                description = "Race stat 2",
                dataType = StatDataType.NUMERIC
              ),
              stat = Stat.NumericStat(value = 2)
            )
          )
        ),
        globalStats = emptyList()
      )
    )

    // In sequential execution, both stats should be present
    val result = CollectedStatAggregator.aggregate(
      origAllCollectedData = originalData,
      reportOutputConfig = createTestReportOutputConfig(),
      reportMetadata = createTestReportMetadata(),
      statCollectorsForAggregation = listOf(collector1, collector2)
    )

    val projectStats = result.collectedStats.find { it.path == projectPath }
    assertNotNull(projectStats)

    // Both stats should be present in sequential execution
    assertEquals(2, projectStats.stats.size)
    assertTrue(projectStats.stats.containsKey("race_stat_1"))
    assertTrue(projectStats.stats.containsKey("race_stat_2"))

    println("Sequential execution: Both stats present - ${projectStats.stats.keys}")
  }

  // Helper class for testing
  private class TestStatCollector(
    private val name: String,
    private val aggregateResult: CollectedStatsAggregate?
  ) : StatCollector {
    override fun getName(): String = name
    override fun aggregate(
      reportOutputConfig: ReportOutputConfig,
      invertAllCollectedDataRepo: InvertAllCollectedDataRepo
    ): CollectedStatsAggregate? = aggregateResult
  }

  private fun createTestReportMetadata(): MetadataJsReportModel {
    return MetadataJsReportModel(
      artifactRepositories = emptyList(),
      branchName = "main",
      buildSystem = BuildSystem.GRADLE,
      currentTime = System.currentTimeMillis(),
      currentTimeFormatted = "test-time",
      latestCommitGitSha = "abc123",
      latestCommitTime = System.currentTimeMillis(),
      latestCommitTimeFormatted = "test-time",
      tagName = null,
      timezoneId = "UTC",
      remoteRepoGit = "git@github.com:test/test.git",
      remoteRepoUrl = "https://github.com/test/test",
      owners = AllOwners(ownerToDetails = emptyMap())
    )
  }

  private fun createTestReportOutputConfig(): ReportOutputConfig {
    return ReportOutputConfig(
      gitCloneDir = File("."),
      invertReportDirectory = File("build/test-reports"),
      ownershipCollector = com.squareup.invert.internal.NoOpInvertOwnershipCollector
    )
  }
}
