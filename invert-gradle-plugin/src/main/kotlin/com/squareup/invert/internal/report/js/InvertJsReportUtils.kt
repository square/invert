package com.squareup.invert.internal.report.js

import com.squareup.invert.internal.models.CollectedConfigurationsForProject
import com.squareup.invert.internal.models.CollectedDependenciesForProject
import com.squareup.invert.internal.models.CollectedOwnershipForProject
import com.squareup.invert.internal.models.CollectedPluginsForProject
import com.squareup.invert.internal.models.CollectedStatsForProject
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.ConfigurationsJsReportModel
import com.squareup.invert.models.js.DependenciesJsReportModel
import com.squareup.invert.models.js.DirectDependenciesJsReportModel
import com.squareup.invert.models.js.OwnershipJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatTotalAndMetadata
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

  fun countFromStat(stat: Stat?): Int {
    return when (stat) {
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

  fun computeGlobalTotals(
    allProjectsStatsData: StatsJsReportModel,
    collectedOwnershipInfo: OwnershipJsReportModel
  ): Map<StatKey, StatTotalAndMetadata> {
    val moduleToOwnerMap = collectedOwnershipInfo.modules
    val allStatMetadatas: List<StatMetadata> = allProjectsStatsData.statInfos.values
      .filter { statInfo ->
        when (statInfo.dataType) {
          StatDataType.BOOLEAN,
          StatDataType.NUMERIC,
          StatDataType.CODE_REFERENCES -> true

          else -> {
            false
          }
        }
      }


    val globalTotals: Map<StatKey, StatTotalAndMetadata> = allStatMetadatas.associate { statMetadata: StatMetadata ->
      var totalCount = 0 // Total count of the stat across all modules
      val totalByModule: Map<ModulePath, Int> = allProjectsStatsData.statsByModule.entries
        .mapNotNull { (modulePath: ModulePath, statsForModule: Map<StatKey, Stat>) ->
          val stat: Stat? = statsForModule[statMetadata.key]
          if (stat != null) {
            val countForStat = countFromStat(stat)
            totalCount += countForStat
            modulePath to countForStat
          } else {
            null
          }
        }
        .filter { it.second != 0 } // Drop all the `0` entries to avoid clutter in the JSON files
        .toMap()

      val ownerToTotalCount: Map<OwnerName, Int> = totalByModule.entries
        .groupBy { (modulePath, totalForModule) ->
          val ownerName = moduleToOwnerMap[modulePath]

          ownerName
        }
        .map { (modulePath, entry) ->
          modulePath to entry.sumOf { it.value }
        }
        .mapNotNull { (ownerName, totalCountForOwner) ->
          if (ownerName == null) {
            null
          } else {
            ownerName to totalCountForOwner
          }
        }
        .toMap()

      statMetadata.key to StatTotalAndMetadata(
        metadata = statMetadata,
        total = totalCount,
        totalByModule = totalByModule,
        totalByOwner = ownerToTotalCount
      )
    }.toMap()

    return globalTotals
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

    val statData = mutableMapOf<ModulePath, Map<StatKey, Stat>>()
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
      plugins = mutableMapOf<GradlePluginId, MutableList<ModulePath>>()
        .also { resultingDepIdToModuleUsageInfo ->
          allPlugins.forEach { collectedDataFromModule ->
            collectedDataFromModule.plugins.forEach { pluginId ->
              val curr = resultingDepIdToModuleUsageInfo[pluginId] ?: mutableListOf()
              curr.add(collectedDataFromModule.path)
              resultingDepIdToModuleUsageInfo[pluginId] = curr
            }
          }
        },
      modules = mutableMapOf<ModulePath, List<GradlePluginId>>()
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
      mutableMapOf<DependencyId, MutableMap<ModulePath, MutableList<ConfigurationName>>>()
        .also { resultingDepIdToModuleUsageInfo ->
          collectedDependenciesForProjects.forEach { collectedDataFromModule ->
            val collectedDataFromModuleGradlePath = collectedDataFromModule.path
            collectedDataFromModule.dependencies.forEach { (dependencyId, usedInConfigurationNames) ->
              val currDataForDepName: MutableMap<ModulePath, MutableList<ConfigurationName>> =
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
    val moduleToAllConfigurationNames = mutableMapOf<ModulePath, Set<ConfigurationName>>()
    val moduleToAnalyzedConfigurationNames = mutableMapOf<ModulePath, Set<ConfigurationName>>()
    val analyzedConfigurationNameToModules = mutableMapOf<ConfigurationName, MutableSet<ModulePath>>()

    allProjectsConfigurationsData.forEach { projectConfigurationsData ->
      allConfigurationNames.addAll(projectConfigurationsData.allConfigurationNames)
      moduleToAllConfigurationNames[projectConfigurationsData.modulePath] =
        projectConfigurationsData.allConfigurationNames
      moduleToAnalyzedConfigurationNames[projectConfigurationsData.modulePath] =
        projectConfigurationsData.analyzedConfigurationNames
      projectConfigurationsData.analyzedConfigurationNames.forEach { analyzedConfigurationName ->
        val modules =
          analyzedConfigurationNameToModules[analyzedConfigurationName] ?: mutableSetOf()
        modules.add(projectConfigurationsData.modulePath)
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
