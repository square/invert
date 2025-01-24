package com.squareup.invert.internal.report

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.internal.models.InvertCombinedCollectedData
import com.squareup.invert.internal.models.InvertPluginFileKey
import java.io.File

/**
 * Reads from all the individual project reports and combines them.
 */
object GradleProjectAnalysisCombiner {
  fun combineAnalysisResults(resultDirPaths: List<String>): InvertCombinedCollectedData {
    val collectedConfigurations = mutableSetOf<CollectedConfigurationsForProject>()
    val collectedDependencies = mutableSetOf<CollectedDependenciesForProject>()
    val collectedOwners = mutableSetOf<CollectedOwnershipForProject>()
    val collectedStats = mutableSetOf<CollectedStatsForProject>()
    val collectedPlugins = mutableSetOf<CollectedPluginsForProject>()

    resultDirPaths
      .map { File(it) }
      .forEach { subprojectInvertReportDirFile ->
        if (subprojectInvertReportDirFile.exists()) {
          File(
            subprojectInvertReportDirFile,
            InvertPluginFileKey.DEPENDENCIES.filename
          ).also { file ->
            if (file.exists()) {
              InvertReportFileUtils.buildModuleToFeaturesMap(file)?.let {
                collectedDependencies.add(it)
              }
            }
          }

          File(
            subprojectInvertReportDirFile,
            InvertPluginFileKey.CONFIGURATIONS.filename
          ).also { file ->
            if (file.exists()) {
              InvertReportFileUtils.readCollectedConfigurationsForAllModules(file)?.let {
                collectedConfigurations.add(it)
              }
            }
          }

          File(
            subprojectInvertReportDirFile,
            InvertPluginFileKey.STATS.filename
          ).also { file ->
            if (file.exists()) {
              InvertReportFileUtils.readCollectedStatsForAllProjectsFromDisk(file)?.let {
                collectedStats.add(it)
              }
            }
          }

          File(
            subprojectInvertReportDirFile,
            InvertPluginFileKey.OWNERS.filename
          ).also { file ->
            InvertReportFileUtils.readCollectedOwnershipForAllProjectsFromDisk(file)
              ?.let { collectedOwners.add(it) }
          }

          File(
            subprojectInvertReportDirFile,
            InvertPluginFileKey.PLUGINS.filename
          ).also { file ->
            if (file.exists()) {
              InvertReportFileUtils.readCollectedPluginsForAllModules(file)?.let {
                synchronized(collectedPlugins) {
                  collectedPlugins.add(it)
                }
              }
            }
          }
        }
      }
    return InvertCombinedCollectedData(
      collectedConfigurations = collectedConfigurations,
      collectedDependencies = collectedDependencies,
      collectedOwners = collectedOwners,
      collectedStats = collectedStats,
      collectedPlugins = collectedPlugins
    )
  }
}