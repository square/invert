package com.squareup.invert.common.utils

import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel

/**
 * A set of multiplatform computations and transformations
 * that can be shared by the plugin and web targets.
 */
object DependencyComputations {

  internal data class PathAndConfigurations(
    val path: ModulePath,
    val configurations: List<ConfigurationName>
  )

  internal fun computePluginIdToGradlePathsMatchingQuery(
    matchingQueryModulesList: List<ModulePath>,
    pluginGroupByFilter: List<GradlePluginId>,
    configurations: List<ConfigurationName>,
    invertedDeps: Map<DependencyId, Map<ModulePath, List<ConfigurationName>>>?,
    collectedPlugins: PluginsJsReportModel?
  ): Map<GradlePluginId, Map<ModulePath, Map<ModulePath, List<PathAndConfigurations>>>> =
    mutableMapOf<
        GradlePluginId,
        MutableMap<ModulePath, MutableMap<ModulePath, MutableList<PathAndConfigurations>>>
        >().also { resultMap ->
      matchingQueryModulesList.forEach { moduleMatchingQuery: ModulePath ->
        val modulesThatReferenceThisDependencyId = invertedDeps?.get(moduleMatchingQuery) ?: mapOf()
        modulesThatReferenceThisDependencyId.forEach { (appGradlePath, configurationNamesForThisDependency) ->
          val pluginsAppliedToApp = collectedPlugins?.modules?.get(appGradlePath) ?: listOf()
          pluginsAppliedToApp.forEach { appliedPluginId ->
            if (pluginGroupByFilter.contains(appliedPluginId)) {
              (resultMap[appliedPluginId] ?: mutableMapOf()).also { appToReferencesMap ->
                (appToReferencesMap[appGradlePath] ?: mutableMapOf()).also { secondCurr ->
                  (secondCurr[appGradlePath] ?: mutableListOf()).also { thirdCurr ->
                    val filteredConfigurations = configurationNamesForThisDependency
                      .filter { configurations.contains(it) }
                    if (moduleMatchingQuery != appGradlePath && filteredConfigurations.isNotEmpty()) {
                      thirdCurr.add(
                        PathAndConfigurations(
                          path = moduleMatchingQuery,
                          configurations = filteredConfigurations,
                        )
                      )
                      secondCurr[appGradlePath] = thirdCurr
                      appToReferencesMap[appGradlePath] = secondCurr
                      resultMap[appliedPluginId] = appToReferencesMap
                    }
                  }
                }
              }
            }
          }
        }
      }
    }

  /**
   * Determine the Configuration -> List<DependencyId> Map for a Gradle Module
   */
  fun dependenciesOf(
    modulePath: ModulePath?,
    combinedReport: DependenciesJsReportModel?
  ): Map<ConfigurationName, List<DependencyId>> =
    mutableMapOf<ConfigurationName, MutableList<DependencyId>>()
      .also { configurationToDepsMap ->
        if (modulePath != null) {
          combinedReport
            ?.invertedDependencies
            ?.forEach { (dependencyId, moduleToConfigurationNames) ->
              val configurationNamesForModule = moduleToConfigurationNames[modulePath]
              configurationNamesForModule?.forEach { configurationName: ConfigurationName ->
                val dependenciesForConfiguration =
                  configurationToDepsMap[configurationName] ?: mutableListOf()
                configurationToDepsMap[configurationName] =
                  dependenciesForConfiguration.apply { add(dependencyId) }
              }
            }
        }
      }
}
