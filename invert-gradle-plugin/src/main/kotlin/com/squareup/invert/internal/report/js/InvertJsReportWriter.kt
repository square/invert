package com.squareup.invert.internal.report.js

import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.InvertSerialization.InvertJson
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.Stat
import com.squareup.invert.models.js.ChunkManifest
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
import com.squareup.invert.models.js.StatJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.SetSerializer
import okio.buffer
import okio.sink
import java.io.File

class InvertJsReportWriter(
  val logger: InvertLogger,
  rootBuildReportsDir: File,
) {
  private val rootBuildHtmlReportDir = File(rootBuildReportsDir, InvertFileUtils.JS_FOLDER_NAME)

  fun createInvertHtmlReport(
    allProjectsDependencyData: Set<CollectedDependenciesForProject>,
    allProjectsConfigurationsData: Set<CollectedConfigurationsForProject>,
    allProjectsStatsData: StatsJsReportModel,
    directDependencies: DirectDependenciesJsReportModel,
    invertedDependencies: DependenciesJsReportModel,
    allPluginsData: Set<CollectedPluginsForProject>,
    collectedOwnershipInfo: OwnershipJsReportModel,
    globalStatTotals: CollectedStatTotalsJsReportModel,
    reportMetadata: MetadataJsReportModel,
    historicalData: Set<HistoricalData>,
  ) {
    val pluginsReport = InvertJsReportUtils.toCollectedPlugins(allPluginsData)
    val modulesList = allProjectsDependencyData.map { it.path }

    val statsMapData: Set<StatJsReportModel> = InvertJsReportUtils.buildStatJsReportModelList(allProjectsStatsData)
    statsMapData.forEach { statModel ->
      val fileKey = JsReportFileKey.STAT.key + "_" + statModel.statInfo.key
      val serializedJson = InvertJson.encodeToString(StatJsReportModel.serializer(), statModel)
      writeJsFileInDir(
        fileKey = fileKey,
        serializedJson = serializedJson,
      )
      writeChunkedStatIfNeeded(
        fileKey = fileKey,
        statModel = statModel,
        serializedJsonLength = serializedJson.length,
      )
    }

    val collectedDependencies = HomeJsReportModel(
      modules = modulesList.sorted(),
      artifacts = invertedDependencies.getAllArtifactIds(reportMetadata.buildSystem).sorted(),
      plugins = pluginsReport.plugins.keys.toList().sorted()
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.METADATA.key,
      serializer = MetadataJsReportModel.serializer(),
      value = reportMetadata
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.HISTORICAL_DATA.key,
      serializer = SetSerializer(HistoricalData.serializer()),
      value = historicalData
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.OWNERS.key,
      serializer = OwnershipJsReportModel.serializer(),
      value = collectedOwnershipInfo
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.STAT_TOTALS.key,
      serializer = CollectedStatTotalsJsReportModel.serializer(),
      value = globalStatTotals
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.CONFIGURATIONS.key,
      serializer = ConfigurationsJsReportModel.serializer(),
      value = InvertJsReportUtils.toCollectedConfigurations(allProjectsConfigurationsData)
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.PLUGINS.key,
      serializer = PluginsJsReportModel.serializer(),
      value = pluginsReport
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.INVERTED_DEPENDENCIES.key,
      serializer = DependenciesJsReportModel.serializer(),
      value = invertedDependencies
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.DIRECT_DEPENDENCIES.key,
      serializer = DirectDependenciesJsReportModel.serializer(),
      value = directDependencies
    )

    writeJsFileInDir(
      fileKey = JsReportFileKey.HOME.key,
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
          it.sink().buffer().use { sink ->
            sink.write(contents)
          }
        }
      }
    }
  }

  fun <T> writeJsFileInDir(
    fileKey: String,
    serializer: KSerializer<T>,
    value: T,
  ) = writeJsFile(
    logger = logger,
    fileKey = fileKey,
    jsOutputFile = InvertFileUtils.outputFile(
      directory = rootBuildHtmlReportDir,
      filename = "$fileKey.js"
    ),
    serializer = serializer,
    value = value
  )

  fun writeJsFileInDir(
    fileKey: String,
    serializedJson: String,
  ) {
    val jsOutputFile = InvertFileUtils.outputFile(
      directory = rootBuildHtmlReportDir,
      filename = "$fileKey.js"
    )
    if (!jsOutputFile.parentFile.exists()) {
      jsOutputFile.parentFile.mkdirs()
    }
    jsOutputFile.sink().buffer().use { sink ->
      sink.writeUtf8(invertJsGlobalVariableAssignment(fileKey = fileKey, value = serializedJson))
    }
    logger.lifecycle("Writing JavaScript $fileKey to file://${jsOutputFile.canonicalPath}")
  }

  fun <T> writeJsFileInDir(
    fileKey: String,
    jsFilename: String,
    serializer: KSerializer<T>,
    value: T,
  ) = writeJsFile(
    logger = logger,
    fileKey = fileKey,
    jsOutputFile = InvertFileUtils.outputFile(
      directory = rootBuildHtmlReportDir,
      filename = jsFilename
    ),
    serializer = serializer,
    value = value
  )

  /**
   * If [statModel] serializes to more than [CHUNK_THRESHOLD_BYTES], emit a chunk manifest
   * and individual JSON chunk files alongside the legacy single-file JS.
   *
   * Each chunk is a valid [StatJsReportModel] containing the full [statInfo] and a subset
   * of [statsByModule] entries, split on sorted module-key boundaries.
   */
  internal fun writeChunkedStatIfNeeded(
    fileKey: String,
    statModel: StatJsReportModel,
    serializedJsonLength: Int = InvertJson.encodeToString(StatJsReportModel.serializer(), statModel).length,
  ) {
    if (serializedJsonLength < CHUNK_THRESHOLD_BYTES) return

    val sortedModuleKeys = statModel.statsByModule.keys.sorted()
    if (sortedModuleKeys.isEmpty()) return

    val chunks = buildChunks(statModel, sortedModuleKeys)

    val chunkFileNames = chunks.mapIndexed { index, _ ->
      "$fileKey.chunk.$index.json"
    }

    // Write chunk files
    chunks.forEachIndexed { index, chunkModel ->
      val chunkFile = InvertFileUtils.outputFile(
        directory = rootBuildHtmlReportDir,
        filename = chunkFileNames[index]
      )
      chunkFile.sink().buffer().use { sink ->
        sink.writeUtf8(InvertJson.encodeToString(StatJsReportModel.serializer(), chunkModel))
      }
      logger.lifecycle("Writing chunk ${index + 1}/${chunks.size} for $fileKey to file://${chunkFile.canonicalPath}")
    }

    // Write manifest
    val manifest = ChunkManifest(
      key = fileKey,
      totalChunks = chunks.size,
      chunkFiles = chunkFileNames,
    )
    val manifestFile = InvertFileUtils.outputFile(
      directory = rootBuildHtmlReportDir,
      filename = "$fileKey.manifest.json"
    )
    manifestFile.sink().buffer().use { sink ->
      sink.writeUtf8(InvertJson.encodeToString(ChunkManifest.serializer(), manifest))
    }
    logger.lifecycle("Writing chunk manifest for $fileKey (${chunks.size} chunks) to file://${manifestFile.canonicalPath}")
  }

  companion object {
    /**
     * Stat payloads larger than this threshold (in bytes of serialized JSON) are also
     * emitted as chunked JSON files alongside the legacy single-file JS.
     */
    const val CHUNK_THRESHOLD_BYTES = 2_000_000

    /**
     * Target maximum size per chunk in bytes. Chunks may exceed this slightly because
     * splitting happens on module boundaries.
     */
    private const val TARGET_CHUNK_SIZE_BYTES = 2_000_000

    /**
     * Build a list of [StatJsReportModel] chunks from the given model, splitting on
     * sorted module-key boundaries so each chunk targets approximately [TARGET_CHUNK_SIZE_BYTES].
     */
    internal fun buildChunks(
      statModel: StatJsReportModel,
      sortedModuleKeys: List<ModulePath>,
    ): List<StatJsReportModel> {
      val chunks = mutableListOf<StatJsReportModel>()
      var currentModules = mutableMapOf<ModulePath, Stat>()
      var currentSize = 0L

      // Pre-compute the fixed overhead of a chunk (statInfo + JSON structure) so we only
      // measure the incremental cost of each module entry in the loop.
      val emptyChunkSize = InvertJson.encodeToString(
        StatJsReportModel.serializer(),
        StatJsReportModel(statInfo = statModel.statInfo, statsByModule = emptyMap()),
      ).length.toLong()

      for (moduleKey in sortedModuleKeys) {
        val stat = statModel.statsByModule[moduleKey] ?: continue
        // Estimate incremental size: full single-entry model minus the fixed overhead.
        val singleEntryModel = StatJsReportModel(
          statInfo = statModel.statInfo,
          statsByModule = mapOf(moduleKey to stat),
        )
        val entrySize = InvertJson.encodeToString(StatJsReportModel.serializer(), singleEntryModel).length.toLong() - emptyChunkSize

        if (currentModules.isNotEmpty() && currentSize + entrySize > TARGET_CHUNK_SIZE_BYTES) {
          chunks.add(StatJsReportModel(statInfo = statModel.statInfo, statsByModule = currentModules))
          currentModules = mutableMapOf()
          currentSize = 0L
        }

        currentModules[moduleKey] = stat
        currentSize += entrySize
      }

      if (currentModules.isNotEmpty()) {
        chunks.add(StatJsReportModel(statInfo = statModel.statInfo, statsByModule = currentModules))
      }

      return chunks
    }

    /**
     * Utility method that concatenates the JS window variable assignment with the actual JSON.
     *
     * This method of variable assignment is used by the invert web ui to get around Web CORS rules.
     */
    private fun invertJsGlobalVariableAssignment(
      fileKey: String,
      value: String
    ): String {
      return buildString {
        appendLine("window.invert_report[\"${fileKey}\"]=")
        appendLine(value)
      }
    }

    fun <T> writeJsFile(
      logger: InvertLogger,
      fileKey: String,
      jsOutputFile: File,
      serializer: KSerializer<T>,
      value: T,
    ) = jsOutputFile.apply {
      if (!parentFile.exists()) {
        parentFile.mkdirs()
      }
      sink().buffer().use { sink ->
        sink.writeUtf8(
          invertJsGlobalVariableAssignment(
            fileKey = fileKey,
            value = InvertJson.encodeToString(
              serializer = serializer,
              value = value
            )
          )
        )
      }
      logger.lifecycle("Writing JavaScript $fileKey to file://$canonicalPath")
    }
  }
}
