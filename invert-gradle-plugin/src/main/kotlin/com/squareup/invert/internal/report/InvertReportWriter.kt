package com.squareup.invert.internal.report

import com.squareup.invert.internal.AggregatedCodeReferences
import com.squareup.invert.internal.InvertFileUtils
import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.internal.report.js.InvertJsReportUtils
import com.squareup.invert.internal.report.js.InvertJsReportUtils.computeGlobalTotals
import com.squareup.invert.internal.report.js.InvertJsReportWriter
import com.squareup.invert.internal.report.json.InvertJsonReportWriter
import com.squareup.invert.internal.report.sarif.InvertSarifReportWriter
import com.squareup.invert.logging.InvertLogger
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ExtraMetadata
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.TechDebtInitiative
import com.squareup.invert.models.js.TechDebtInitiativeConfig
import java.io.File
import kotlin.collections.set

class InvertReportWriter(
  private val invertLogger: InvertLogger,
  private val rootBuildReportsDir: File
) {
  fun writeProjectData(
    reportMetadata: MetadataJsReportModel,
    collectedData: InvertCombinedCollectedData,
    historicalData: Set<HistoricalData>,
    techDebtInitiatives: List<TechDebtInitiative>
  ) {
    val collectedOwners: Set<CollectedOwnershipForProject> = collectedData.collectedOwners
    val collectedStats: Set<CollectedStatsForProject> = collectedData.collectedStats
    val collectedDependencies: Set<CollectedDependenciesForProject> =
      collectedData.collectedDependencies
    val collectedConfigurations: Set<CollectedConfigurationsForProject> =
      collectedData.collectedConfigurations
    val collectedPlugins: Set<CollectedPluginsForProject> = collectedData.collectedPlugins
    val collectedOwnershipInfo = InvertJsReportUtils.buildModuleToOwnerMap(collectedOwners)
    val allProjectsStatsData = InvertJsReportUtils.buildModuleToStatsMap(collectedStats)
    val directDependenciesJsReportModel =
      InvertJsReportUtils.toDirectDependenciesJsReportModel(collectedDependencies)
    val invertedDependenciesJsReportModel =
      InvertJsReportUtils.toInvertedDependenciesJsReportModel(collectedDependencies)

    assertModuleMatch(
      logger = invertLogger,
      modulesList = collectedDependencies.map { it.path },
      invertedModulesList = invertedDependenciesJsReportModel.getAllModulePaths(reportMetadata.buildSystem)
    )

    val globalStats = computeGlobalTotals(allProjectsStatsData, collectedOwnershipInfo)

    val historicalDataWithCurrent = (historicalData + HistoricalData(
      reportMetadata = reportMetadata,
      statTotalsAndMetadata = CollectedStatTotalsJsReportModel(globalStats)
    )).sortedBy { it.reportMetadata.latestCommitTime }.toSet()

    // JSON Report
    InvertJsonReportWriter(invertLogger, rootBuildReportsDir).createInvertJsonReport(
      reportMetadata = reportMetadata,
      allConfigurationsData = collectedConfigurations,
      allProjectsDependencyData = collectedDependencies,
      allProjectsStatsData = allProjectsStatsData,
      allPluginsData = collectedPlugins,
      allOwnersData = collectedOwners,
      globalStats = globalStats,
      historicalData = historicalDataWithCurrent,
    )

    // Include all stats into one SARIF report.
    InvertSarifReportWriter(invertLogger, rootBuildReportsDir).createInvertSarifReport(
      allProjectsStatsData = allProjectsStatsData
    )

    // HTML/JS Report
    InvertJsReportWriter(invertLogger, rootBuildReportsDir).createInvertHtmlReport(
      reportMetadata = reportMetadata,
      allProjectsDependencyData = collectedDependencies,
      allProjectsStatsData = allProjectsStatsData,
      directDependencies = directDependenciesJsReportModel,
      invertedDependencies = invertedDependenciesJsReportModel,
      allPluginsData = collectedPlugins,
      collectedOwnershipInfo = collectedOwnershipInfo,
      allProjectsConfigurationsData = collectedConfigurations,
      globalStatTotals = CollectedStatTotalsJsReportModel(globalStats),
      historicalData = historicalDataWithCurrent,
    )

    // Exports all Code References to individual JSON and SARIF files.
    writeIndividualCodeReferenceStatFiles(
      reportDirectory = rootBuildReportsDir,
      aggregatedCollectedData = collectedData,
    )

    // Write TDI config if we have specified any via extension.
    if (techDebtInitiatives.isNotEmpty()) {
      InvertJsonReportWriter.writeJsonFile(
        description = "Tech Debt Initiatives Configuration",
        jsonOutputFile = InvertFileUtils.outputFile(
          File(rootBuildReportsDir, "json"),
          "tdi_config.json"
        ),
        serializer = TechDebtInitiativeConfig.serializer(),
        value = TechDebtInitiativeConfig(
          techDebtInitiatives
        )
      )
    }
  }

  /**
   * This provides a warning to the user to let them know that a module was found as a dependency
   * but was not scanned itself.  In order to get a full picture of the project, all should
   * be scanned.
   */
  private fun assertModuleMatch(
    logger: InvertLogger,
    modulesList: List<ModulePath>,
    invertedModulesList: List<DependencyId>
  ) {
    if (!invertedModulesList.containsAll(modulesList)) {
      val modulesMap = modulesList.groupBy { it }
      val invertedModulesMap = invertedModulesList.groupBy { it }
      val errorString = buildString {
        appendLine("WARNING: Module Mismatch...")
        appendLine("The following modules are dependencies, but were not scanned:")
        var idx = 1
        invertedModulesMap.keys.sorted().forEach { path ->
          if (modulesMap[path] == null) {
            appendLine("${idx++}. $path")
          }
        }
      }

      logger.warn(errorString)
    }
  }

  private fun writeIndividualCodeReferenceStatFiles(
    reportDirectory: File,
    aggregatedCollectedData: InvertCombinedCollectedData,
  ) {
    val allStatMetadatas =
      aggregatedCollectedData.collectedStats.flatMap { it.statInfos.values }.distinct()

    val moduleToOwnerMap: Map<ModulePath, OwnerName> =
      aggregatedCollectedData.collectedOwners.associate { it.path to it.ownerName }

    allStatMetadatas.forEach { statMetadata: StatMetadata ->
      val statKey = statMetadata.key
      val allCodeReferencesForStatWithProjectPathExtra =
        mutableListOf<Stat.CodeReferencesStat.CodeReference>()
      // Create Code References Export after Aggregation
      aggregatedCollectedData.collectedStats.forEach { collectedStatsForProject: CollectedStatsForProject ->
        collectedStatsForProject.stats[statKey]?.takeIf { it is Stat.CodeReferencesStat }
          ?.let { stat ->
            val collectedCodeReferenceStat = stat as Stat.CodeReferencesStat

            val codeReferences = collectedCodeReferenceStat.value
            if (codeReferences.isNotEmpty()) {
              allCodeReferencesForStatWithProjectPathExtra.addAll(
                collectedCodeReferenceStat.value.map { codeReference: Stat.CodeReferencesStat.CodeReference ->
                  // Use the owner from the code reference if it exists, otherwise use the module's owner
                  val codeReferenceOwner =
                    codeReference.owner ?: moduleToOwnerMap[collectedStatsForProject.path]

                  // Updating the extras to include the "module" and "owner"
                  val updatedExtras = codeReference.extras.toMutableMap().apply {
                    this[MODULE_EXTRA_METADATA.key] = collectedStatsForProject.path
                    if (codeReferenceOwner != null) {
                      this[OWNER_EXTRA_METADATA.key] = codeReferenceOwner
                    }
                  }
                  codeReference.copy(
                    extras = updatedExtras
                  )
                }
              )
            }
          }
      }
      if (allCodeReferencesForStatWithProjectPathExtra.isNotEmpty()) {
        InvertJsonReportWriter.writeJsonFile(
          description = "All CodeReferences for ${statMetadata.key}",
          jsonOutputFile = InvertFileUtils.outputFile(
            File(reportDirectory, "json"),
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

        InvertSarifReportWriter.writeToSarifReport(
          description = "All CodeReferences for ${statMetadata.key}",
          fileName = InvertFileUtils.outputFile(
            File(reportDirectory, "sarif"),
            "code_references_${statMetadata.key}.sarif"
          ),
          metadata = statMetadata,
          values = allCodeReferencesForStatWithProjectPathExtra,
          moduleExtraKey = MODULE_EXTRA_METADATA.key,
          ownerExtraKey = OWNER_EXTRA_METADATA.key,
        )
      }
    }
  }

  companion object {
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
  }
}
