package com.squareup.invert.internal.report.js

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.ConfigurationsJsReportModel
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.DirectDependenciesJsReportModel
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.HomeJsReportModel
import com.squareup.invert.models.js.JsReportFileKey
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

class InvertJsReportWriter(
  val logger: InvertLogger,
  rootBuildReportsDir: File,
) {
  private val rootBuildHtmlReportDir = File(rootBuildReportsDir, InvertFileUtils.JS_FOLDER_NAME)

  fun createInvertHtmlReport(
    allProjectsDependencyData: List<CollectedDependenciesForProject>,
    allProjectsConfigurationsData: List<CollectedConfigurationsForProject>,
    allProjectsStatsData: StatsJsReportModel,
    directDependencies: DirectDependenciesJsReportModel,
    invertedDependencies: DependenciesJsReportModel,
    allPluginsData: List<CollectedPluginsForProject>,
    collectedOwnershipInfo: OwnershipJsReportModel,
    globalStatTotals: CollectedStatTotalsJsReportModel,
    reportMetadata: MetadataJsReportModel,
    historicalData: List<HistoricalData>,
  ) {
    val pluginsReport = InvertJsReportUtils.toCollectedPlugins(allPluginsData)
    val modulesList = allProjectsDependencyData.map { it.path }

    val collectedDependencies = HomeJsReportModel(
      modules = modulesList.sorted(),
      artifacts = invertedDependencies.getAllArtifactIds().sorted(),
      plugins = pluginsReport.plugins.keys.toList().sorted()
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.METADATA,
      serializer = MetadataJsReportModel.serializer(),
      value = reportMetadata
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.HISTORICAL_DATA,
      serializer = ListSerializer(HistoricalData.serializer()),
      value = historicalData
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.OWNERS,
      serializer = OwnershipJsReportModel.serializer(),
      value = collectedOwnershipInfo
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.STAT_TOTALS,
      serializer = CollectedStatTotalsJsReportModel.serializer(),
      value = globalStatTotals
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.STATS,
      serializer = StatsJsReportModel.serializer(),
      value = allProjectsStatsData
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.CONFIGURATIONS,
      serializer = ConfigurationsJsReportModel.serializer(),
      value = InvertJsReportUtils.toCollectedConfigurations(allProjectsConfigurationsData)
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.PLUGINS,
      serializer = PluginsJsReportModel.serializer(),
      value = pluginsReport
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.INVERTED_DEPENDENCIES,
      serializer = DependenciesJsReportModel.serializer(),
      value = invertedDependencies
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.DIRECT_DEPENDENCIES,
      serializer = DirectDependenciesJsReportModel.serializer(),
      value = directDependencies
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.HOME,
      serializer = HomeJsReportModel.serializer(),
      value = collectedDependencies
    )

    val classLoader = object {}::class.java.classLoader

    listOf(
      "index.html",
      "invert.css",
      "invert.js",
      "invert-report.js",
    ).forEach { filename ->
      val contents = classLoader.getResource("META-INF/$filename")?.readBytes()
      contents?.let {
        InvertFileUtils.outputFile(
          directory = rootBuildHtmlReportDir.parentFile,
          filename = filename
        ).also {
          logger.lifecycle("Written to file://${it.path}")
          it.writeBytes(contents)
        }
      }
    }
  }

  fun <T> writeJsFileInDir(
    fileKey: JsReportFileKey,
    serializer: KSerializer<T>,
    value: T,
  ) = writeJsFile(
    logger = logger,
    fileKey = fileKey,
    jsOutputFile = InvertFileUtils.outputFile(
      directory = rootBuildHtmlReportDir,
      filename = fileKey.jsFilename
    ),
    serializer = serializer,
    value = value
  )

  companion object {
    /**
     * Utility method that concatenates the JS window variable assignment with the actual JSON.
     *
     * This method of variable assignment is used by the invert web ui to get around Web CORS rules.
     */
    private fun invertJsGlobalVariableAssignment(
      fileKey: JsReportFileKey,
      value: String
    ): String {
      return buildString {
        appendLine("window.invert_report.${fileKey.key}=")
        appendLine(value)
      }
    }

    fun <T> writeJsFile(
      logger: InvertLogger,
      fileKey: JsReportFileKey,
      jsOutputFile: File,
      serializer: KSerializer<T>,
      value: T,
    ) = jsOutputFile.apply {
      if (!parentFile.exists()) {
        parentFile.mkdirs()
      }
      writeText(
        invertJsGlobalVariableAssignment(
          fileKey = fileKey,
          value = InvertJson.encodeToString(
            serializer = serializer,
            value = value
          )
        )
      )
      logger.lifecycle("Writing JavaScript ${fileKey.description} to file://$canonicalPath")
    }
  }
}
