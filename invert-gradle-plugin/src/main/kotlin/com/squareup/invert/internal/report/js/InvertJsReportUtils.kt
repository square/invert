package com.squareup.invert.internal.report.js

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.models.CollectedStatType.BOOLEAN
import com.squareup.invert.models.CollectedStatType.CODE_REFERENCES
import com.squareup.invert.models.CollectedStatType.DI_PROVIDES_AND_INJECTS
import com.squareup.invert.models.CollectedStatType.NUMERIC
import com.squareup.invert.models.CollectedStatType.STRING
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.ConfigurationsJsReportModel
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.DirectDependenciesJsReportModel
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel

/**
 * A collection of transforms to create the output models used in the JS Web Report.
 */
object InvertJsReportUtils {

  /**
   * Takes all [CollectedOwnershipForProject] collected by Invert, and creates the JS Report Model.
   */
  fun buildModuleToOwnerMap(
    collectedOwners: List<CollectedOwnershipForProject>
  ): OwnershipJsReportModel {
    return OwnershipJsReportModel(
      modules = collectedOwners
        .associateBy { it.path }
        .mapValues { it.value.ownerInfo.name },
      teams = collectedOwners
        .associateBy { it.ownerInfo.name }
        .mapValues { it.value.ownerInfo }
    )
  }


  fun computeGlobalStats(allProjectsStatsData: StatsJsReportModel): Map<StatMetadata, Int> {
    val globalStats: Map<StatMetadata, Int> = allProjectsStatsData.statInfos.values
      .filter {
        when (it.statType) {
          BOOLEAN,
          CODE_REFERENCES,
          NUMERIC -> {
            true
          }

          STRING,
          DI_PROVIDES_AND_INJECTS -> {
            false
          }
        }
      }
      .associateWith { statMetadata ->
        val statKey = statMetadata.key
        allProjectsStatsData.statsByModule.values.sumOf {
          val stat = it[statKey]
          when (stat) {
            is Stat.NumericStat -> stat.value
            is Stat.CodeReferencesStat -> stat.value.size
            is Stat.BooleanStat -> if (stat.value) {
              1
            } else {
              0
            }

            else -> {
              0 // Default Value
            }
          }
        }
      }.toMap()
    return globalStats
  }

  /**
   * Takes all [CollectedStatsForProject] collected by Invert, and creates the JS Report Model.
   */
  fun buildModuleToStatsMap(collectedStats: List<CollectedStatsForProject>): StatsJsReportModel {
    val statInfos: Map<StatKey, StatMetadata> = mutableSetOf<StatMetadata>()
      .also { statInfos ->
        collectedStats.forEach { collectedStatsForProject ->
          statInfos.addAll(collectedStatsForProject.statInfos.values)
        }
      }
      .associateBy { it.key }

    val statData = mutableMapOf<GradlePath, Map<StatKey, Stat>>()
    collectedStats.forEach { collectedStatForProject: CollectedStatsForProject ->
      statData[collectedStatForProject.path] = collectedStatForProject.stats
    }

    return StatsJsReportModel(
      statInfos = statInfos,
      statsByModule = statData,
    )
  }

  /**
   * Takes all [CollectedPluginsForProject] collected by Invert, and creates the JS Report Model.
   */
  fun toCollectedPlugins(allPlugins: List<CollectedPluginsForProject>): PluginsJsReportModel {
    return PluginsJsReportModel(
      plugins = mutableMapOf<GradlePluginId, MutableList<GradlePath>>()
        .also { resultingDepIdToModuleUsageInfo ->
          allPlugins.forEach { collectedDataFromModule ->
            collectedDataFromModule.plugins.forEach { pluginId ->
              val curr = resultingDepIdToModuleUsageInfo[pluginId] ?: mutableListOf()
              curr.add(collectedDataFromModule.path)
              resultingDepIdToModuleUsageInfo[pluginId] = curr
            }
          }
        },
      modules = mutableMapOf<GradlePath, List<GradlePluginId>>()
        .also { map ->
          allPlugins.forEach { collectedDataFromModule ->
            collectedDataFromModule.plugins.onEach {
              map[collectedDataFromModule.path] = collectedDataFromModule.plugins
            }
          }
        },
    )
  }

  /**
   * Takes all [CollectedDependenciesForProject] collected by Invert, and creates the JS Report Model.
   */
  fun toInvertedDependenciesJsReportModel(
    collectedDependenciesForProjects: List<CollectedDependenciesForProject>
  ): DependenciesJsReportModel {
    return DependenciesJsReportModel(
      mutableMapOf<DependencyId, MutableMap<GradlePath, MutableList<ConfigurationName>>>()
        .also { resultingDepIdToModuleUsageInfo ->
          collectedDependenciesForProjects.forEach { collectedDataFromModule ->
            val collectedDataFromModuleGradlePath = collectedDataFromModule.path
            collectedDataFromModule.dependencies.forEach { (dependencyId, usedInConfigurationNames) ->
              val currDataForDepName: MutableMap<GradlePath, MutableList<ConfigurationName>> =
                resultingDepIdToModuleUsageInfo[dependencyId] ?: mutableMapOf()
              val currConfigsForPath: MutableList<ConfigurationName> =
                currDataForDepName[collectedDataFromModuleGradlePath] ?: mutableListOf()
              currConfigsForPath.addAll(usedInConfigurationNames)
              currDataForDepName[collectedDataFromModuleGradlePath] = currConfigsForPath
              resultingDepIdToModuleUsageInfo[dependencyId] = currDataForDepName
            }
          }
        }
    )
  }

  fun toCollectedConfigurations(
    allProjectsConfigurationsData: List<CollectedConfigurationsForProject>
  ): ConfigurationsJsReportModel {
    val allConfigurationNames = mutableSetOf<String>()
    val moduleToAllConfigurationNames = mutableMapOf<GradlePath, Set<ConfigurationName>>()
    val moduleToAnalyzedConfigurationNames = mutableMapOf<GradlePath, Set<ConfigurationName>>()
    val analyzedConfigurationNameToModules = mutableMapOf<ConfigurationName, MutableSet<GradlePath>>()

    allProjectsConfigurationsData.forEach { projectConfigurationsData ->
      allConfigurationNames.addAll(projectConfigurationsData.allConfigurationNames)
      moduleToAllConfigurationNames[projectConfigurationsData.path] =
        projectConfigurationsData.allConfigurationNames
      moduleToAnalyzedConfigurationNames[projectConfigurationsData.path] =
        projectConfigurationsData.analyzedConfigurationNames
      projectConfigurationsData.analyzedConfigurationNames.forEach { analyzedConfigurationName ->
        val modules =
          analyzedConfigurationNameToModules[analyzedConfigurationName] ?: mutableSetOf()
        modules.add(projectConfigurationsData.path)
        analyzedConfigurationNameToModules[analyzedConfigurationName] = modules
      }
    }

    return ConfigurationsJsReportModel(
      allConfigurationNames = allConfigurationNames,
      moduleToAllConfigurationNames = moduleToAllConfigurationNames,
      moduleToAnalyzedConfigurationNames = moduleToAnalyzedConfigurationNames,
      configurationNameToModules = analyzedConfigurationNameToModules
    )
  }

  fun toDirectDependenciesJsReportModel(
    allProjectsDependencyData: List<CollectedDependenciesForProject>
  ): DirectDependenciesJsReportModel {
    return DirectDependenciesJsReportModel(
      allProjectsDependencyData.associate { it.path to it.directDependencies }
    )
  }
}
