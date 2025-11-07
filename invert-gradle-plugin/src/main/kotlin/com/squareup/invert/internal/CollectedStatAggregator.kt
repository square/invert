package com.squareup.invert.internal

import com.squareup.invert.CollectedStatsAggregate
import com.squareup.invert.InvertAllCollectedDataRepo
import com.squareup.invert.ReportOutputConfig
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.internal.report.sarif.InvertSarifReportWriter
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ExtraMetadata
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.MetadataJsReportModel
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class AggregatedCodeReferences(
  val metadata: StatMetadata,
  val values: List<Stat.CodeReferencesStat.CodeReference>,
)

/**
 * Utilities to compute aggregate stats with [StatCollector]s that can access data
 * from all modules via a [InvertAllCollectedDataRepo].
 */
object CollectedStatAggregator {

  /**
   * This gives the opportunity for [StatCollector]s to run a final time with the context of the entire project.
   */
  fun aggregate(
    origAllCollectedData: InvertCombinedCollectedData,
    reportOutputConfig: ReportOutputConfig,
    reportMetadata: MetadataJsReportModel,
    statCollectorsForAggregation: List<StatCollector>?
  ): InvertCombinedCollectedData {
    val projectPathToCollectedStatsForProject: MutableMap<ModulePath, CollectedStatsForProject> =
      origAllCollectedData.collectedStats
        .associateBy { it.path }
        .toMutableMap()
    statCollectorsForAggregation?.forEach { statCollectorForAggregation: StatCollector ->
      val aggregationResult: CollectedStatsAggregate? = statCollectorForAggregation.aggregate(
        reportOutputConfig = reportOutputConfig,
        invertAllCollectedDataRepo = InvertAllCollectedDataRepo(
          projectMetadata = reportMetadata,
          allCollectedData = origAllCollectedData
        ),
      )

      aggregationResult?.aggregatedStatsByProject?.entries?.forEach { (projectPath, stats) ->
        val curr: CollectedStatsForProject =
          projectPathToCollectedStatsForProject[projectPath] ?: CollectedStatsForProject(
            path = projectPath,
            statInfos = emptyMap(),
            stats = emptyMap()
          )

        val newStatInfos: MutableMap<StatKey, StatMetadata> = curr.statInfos.toMutableMap()
        val newStats: MutableMap<StatKey, Stat> = curr.stats.toMutableMap()
        stats.forEach { collectedStat ->
          collectedStat.stat?.let { stat ->
            newStats[collectedStat.metadata.key] = stat
            newStatInfos[collectedStat.metadata.key] = collectedStat.metadata
          }
        }
        synchronized(projectPathToCollectedStatsForProject) {
          projectPathToCollectedStatsForProject[projectPath] = curr.copy(
            path = curr.path,
            statInfos = newStatInfos,
            stats = newStats,
          )
        }
      }
    }
    return origAllCollectedData.copy(
      collectedStats = projectPathToCollectedStatsForProject.values.toSet()
    )
  }
}