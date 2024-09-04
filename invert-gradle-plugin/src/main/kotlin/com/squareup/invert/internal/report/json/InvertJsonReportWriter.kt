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
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

class InvertJsonReportWriter(
  private val logger: InvertLogger,
  rootBuildReportsDir: File,
) {
  private val rootBuildJsonReportsDir = File(rootBuildReportsDir, InvertFileUtils.JSON_FOLDER_NAME)
  fun createInvertJsonReport(
    allConfigurationsData: List<CollectedConfigurationsForProject>,
    allProjectsDependencyData: List<CollectedDependenciesForProject>,
    allProjectsStatsData: StatsJsReportModel,
    allPluginsData: List<CollectedPluginsForProject>,
    allOwnersData: List<CollectedOwnershipForProject>,
    globalStats: Map<StatMetadata, Int>,
    reportMetadata: MetadataJsReportModel,
  ) {
    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.METADATA,
      serializer = MetadataJsReportModel.serializer(),
      value = reportMetadata
    )
    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.PLUGINS,
      serializer = ListSerializer(CollectedPluginsForProject.serializer()),
      value = allPluginsData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.STAT_TOTALS,
      serializer = CollectedStatTotalsJsReportModel.serializer(),
      value = CollectedStatTotalsJsReportModel(globalStats)
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.CONFIGURATIONS,
      serializer = ListSerializer(CollectedConfigurationsForProject.serializer()),
      value = allConfigurationsData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.DEPENDENCIES,
      serializer = ListSerializer(CollectedDependenciesForProject.serializer()),
      value = allProjectsDependencyData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.STATS,
      serializer = StatsJsReportModel.serializer(),
      value = allProjectsStatsData
    )

    writeJsonFileInDir(
      jsonFileKey = InvertPluginFileKey.OWNERS,
      serializer = ListSerializer(CollectedOwnershipForProject.serializer()),
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
