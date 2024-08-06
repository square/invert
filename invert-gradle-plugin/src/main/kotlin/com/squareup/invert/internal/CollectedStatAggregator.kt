package com.squareup.invert.internal

import com.squareup.invert.CollectedStatsAggregate
import com.squareup.invert.InvertAllCollectedDataRepo
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.MetadataJsReportModel

object CollectedStatAggregator {

  /**
   * This gives the opportunity for [StatCollector]s to run a final time with the context of the entire project.
   */
  fun aggregate(
    allCollectedData: InvertCombinedCollectedData,
    reportMetadata: MetadataJsReportModel,
    statCollectors: List<StatCollector>?
  ): InvertCombinedCollectedData {
    val statMap: MutableMap<String, CollectedStatsForProject> =
      allCollectedData.collectedStats.associateBy { it.path }.toMutableMap()
    statCollectors?.forEach { statCollector: StatCollector ->
      val result: CollectedStatsAggregate = statCollector.aggregate(
        InvertAllCollectedDataRepo(
          projectMetadata = reportMetadata,
          allCollectedData = allCollectedData
        )
      )
      result.projectStats.entries.forEach { (gradlePath, stats) ->
        val curr: CollectedStatsForProject = statMap[gradlePath] ?: CollectedStatsForProject(
          path = gradlePath,
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
        statMap[gradlePath] = curr.copy(
          path = curr.path,
          statInfos = newStatInfos,
          stats = newStats,
        )
      }
    }
    return allCollectedData.copy(
      collectedStats = statMap.values.toList()
    )
  }
}