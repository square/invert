package com.squareup.invert.internal.report.json

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.InvertPluginFileKey
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.logging.SystemOutInvertLogger
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.StatTotalAndMetadata
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import java.io.File

class InvertJsonReportWriter(
  private val logger: InvertLogger,
  rootBuildReportsDir: File,
) {
  private val rootBuildJsonReportsDir = File(rootBuildReportsDir, InvertFileUtils.JSON_FOLDER_NAME)
  fun createInvertJsonReport(
    allConfigurationsData: Set<CollectedConfigurationsForProject>,
    allProjectsDependencyData: Set<CollectedDependenciesForProject>,
    allProjectsStatsData: StatsJsReportModel,
    allPluginsData: Set<CollectedPluginsForProject>,
    allOwnersData: Set<CollectedOwnershipForProject>,
    globalStats: Map<StatKey, StatTotalAndMetadata>,
    reportMetadata: MetadataJsReportModel,
    historicalData: Set<HistoricalData>,
  ) {
    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.METADATA,
      serializer = MetadataJsReportModel.serializer(),
      value = reportMetadata
    )
    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.PLUGINS,
      serializer = SetSerializer(CollectedPluginsForProject.serializer()),
      value = allPluginsData
    )
    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.HISTORICAL_DATA,
      serializer = SetSerializer(HistoricalData.serializer()),
      value = historicalData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.STAT_TOTALS,
      serializer = CollectedStatTotalsJsReportModel.serializer(),
      value = CollectedStatTotalsJsReportModel(globalStats)
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.CONFIGURATIONS,
      serializer = SetSerializer(CollectedConfigurationsForProject.serializer()),
      value = allConfigurationsData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.DEPENDENCIES,
      serializer = SetSerializer(CollectedDependenciesForProject.serializer()),
      value = allProjectsDependencyData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.OWNERS,
      serializer = SetSerializer(CollectedOwnershipForProject.serializer()),
      value = allOwnersData
    )
  }

  fun <T> writeJsonFileInDir(
    jsonFileKey: InvertPluginFileKey,
    serializer: KSerializer<T>,
    value: T,
  ) = writeJsonFile(
    logger = logger,
    jsonFileKey = jsonFileKey,
    jsonOutputFile = InvertFileUtils.outputFile(
      directory = rootBuildJsonReportsDir,
      filename = jsonFileKey.filename
    ),
    serializer = serializer,
    value = value
  )

  companion object {
    fun <T> writeJsonFile(
      logger: InvertLogger,
      jsonFileKey: InvertPluginFileKey,
      jsonOutputFile: File,
      serializer: KSerializer<T>,
      value: T,
    ) = jsonOutputFile.apply {
      writeText(
        InvertJson.encodeToString(
          serializer = serializer,
          value = value
        )
      )
      logger.lifecycle("Writing JSON ${jsonFileKey.description} to file://$canonicalPath")
    }

    fun <T> writeJsonFile(
      description: String,
      jsonOutputFile: File,
      serializer: KSerializer<T>,
      value: T,
      logger: InvertLogger = SystemOutInvertLogger,
    ) = jsonOutputFile.apply {
      writeText(
        InvertJson.encodeToString(
          serializer = serializer,
          value = value
        )
      )
      logger.lifecycle("Writing JSON ${description} to file://$canonicalPath")
    }
  }
}
