package com.squareup.invert.internal

import com.squareup.invert.CollectedStatsAggregate
import com.squareup.invert.InvertAllCollectedDataRepo
import com.squareup.invert.ReportOutputConfig
import com.squareup.invert.StatCollector
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
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

  private val MODULE_EXTRA_METADATA = ExtraMetadata(
    key = "module",
    type = ExtraDataType.STRING,
    description = "Module"
  )
  private val OWNER_EXTRA_METADATA = ExtraMetadata(
    key = "owner",
    type = ExtraDataType.STRING,
    description = "Owner"
  )

  private fun exportFullListOfCodeReferences(
    reportOutputConfig: ReportOutputConfig,
    origAllCollectedData: InvertCombinedCollectedData
  ) {
    val allStatMetadatas = origAllCollectedData.collectedStats.flatMap { it.statInfos.values }.distinct()

    val moduleToOwnerMap: Map<ModulePath, OwnerName> =
      origAllCollectedData.collectedOwners.associate { it.path to it.ownerName }

    allStatMetadatas.forEach { statMetadata: StatMetadata ->
      val statKey = statMetadata.key
      val allCodeReferencesForStatWithProjectPathExtra = mutableListOf<Stat.CodeReferencesStat.CodeReference>()
      // Create Code References Export after Aggregation
      origAllCollectedData.collectedStats.forEach { collectedStatsForProject: CollectedStatsForProject ->
        collectedStatsForProject.stats[statKey]?.takeIf { it is Stat.CodeReferencesStat }?.let { stat ->
          val collectedCodeReferenceStat = stat as Stat.CodeReferencesStat

          val codeReferences = collectedCodeReferenceStat.value
          if (codeReferences.isNotEmpty()) {
            synchronized(allCodeReferencesForStatWithProjectPathExtra) {
              allCodeReferencesForStatWithProjectPathExtra.addAll(
                collectedCodeReferenceStat.value.map { codeReference: Stat.CodeReferencesStat.CodeReference ->
                  // Adding addition "extra" field named "project"
                  codeReference.copy(
                    extras = codeReference.extras.apply {
                      plus(MODULE_EXTRA_METADATA.key to collectedStatsForProject.path)
                      moduleToOwnerMap[collectedStatsForProject.path]?.let { ownerName ->
                        plus(
                          OWNER_EXTRA_METADATA.key to ownerName
                        )
                      }
                    }
                  )
                }
              )
            }
          }
        }
      }
      if (allCodeReferencesForStatWithProjectPathExtra.isNotEmpty()) {
        InvertJsonReportWriter.writeJsonFile(
          description = "All CodeReferences for ${statMetadata.key}",
          jsonOutputFile = InvertFileUtils.outputFile(
            File(reportOutputConfig.invertReportDirectory, "json"),
            "code_references_${statMetadata.key}.json"
          ),
          serializer = AggregatedCodeReferences.serializer(),
          value = AggregatedCodeReferences(
            metadata = statMetadata.copy(
              extras = statMetadata.extras
                .plus(MODULE_EXTRA_METADATA)
                .plus(OWNER_EXTRA_METADATA)
            ),
            values = allCodeReferencesForStatWithProjectPathExtra
          )
        )
      }
    }
  }

  /**
   * This gives the opportunity for [StatCollector]s to run a final time with the context of the entire project.
   */
  fun aggregate(
    origAllCollectedData: InvertCombinedCollectedData,
    reportOutputConfig: ReportOutputConfig,
    reportMetadata: MetadataJsReportModel,
    statCollectorsForAggregation: List<StatCollector>?
  ): InvertCombinedCollectedData {

    // Exports all Code References to individual JSON files.
    // TODO Move out of "Aggregate" phase since it doesn't aggregate
    exportFullListOfCodeReferences(
      reportOutputConfig = reportOutputConfig,
      origAllCollectedData = origAllCollectedData
    )

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