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
    collectedOwners: Set<CollectedOwnershipForProject>
  ): OwnershipJsReportModel {
    return OwnershipJsReportModel(
      modules = collectedOwners
        .associateBy { it.path }
        .mapValues { it.value.ownerName },
      teams = collectedOwners.map { it.ownerName }.toSet()
    )
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

          StatDataType.STRING -> false
        }
      }


    val globalTotals = mutableMapOf<StatKey, StatTotalAndMetadata>()
    allStatMetadatas.forEach { statMetadata: StatMetadata ->
      var totalCount = 0 // Total count of the stat across all modules

      val ownerToTotalCountForStat = mutableMapOf<OwnerName, Int>()

      allProjectsStatsData.statsByModule.entries.forEach { (modulePath, statTotalAndMetadata) ->
        val stat: Stat? = statTotalAndMetadata[statMetadata.key]
        if (stat != null) {
          val moduleOwnerName = moduleToOwnerMap[modulePath]!!

          val currentCountForOwner: Int = ownerToTotalCountForStat.getOrDefault(moduleOwnerName, 0)
          when (stat) {
            is Stat.NumericStat -> {
              ownerToTotalCountForStat[moduleOwnerName] = currentCountForOwner + stat.value
            }

            is Stat.CodeReferencesStat -> {
              stat.value.forEach { codeReference ->
                val owner = codeReference.owner ?: moduleOwnerName
                println("Adding for $owner, $codeReference")
                val currentCountForOwner: Int = ownerToTotalCountForStat.getOrDefault(owner, 0)
                ownerToTotalCountForStat[owner] = currentCountForOwner + 1
              }
            }

            is Stat.BooleanStat -> {
              val currentModuleOwnerCount: Int = ownerToTotalCountForStat.getOrDefault(moduleOwnerName, 0)
              ownerToTotalCountForStat[moduleOwnerName] = currentModuleOwnerCount + if (stat.value) {
                1
              } else {
                0
              }
            }

            is Stat.StringStat -> {
              val currentModuleOwnerCount: Int = ownerToTotalCountForStat.getOrDefault(moduleOwnerName, 0)
              ownerToTotalCountForStat[moduleOwnerName] = currentModuleOwnerCount + 1
            }
          }

          globalTotals[statMetadata.key] = StatTotalAndMetadata(
            metadata = statMetadata,
            total = ownerToTotalCountForStat.values.sum(),
            totalByOwner = ownerToTotalCountForStat
          ).also { total ->
            println(total)
          }
        }
      }
    }

    return globalTotals
  }

  /**
   * Takes all [CollectedStatsForProject] collected by Invert, and creates the JS Report Model.
   */
  fun buildModuleToStatsMap(collectedStats: Set<CollectedStatsForProject>): StatsJsReportModel {
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
  fun toCollectedPlugins(allPlugins: Set<CollectedPluginsForProject>): PluginsJsReportModel {
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
    collectedDependenciesForProjects: Set<CollectedDependenciesForProject>
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
    allProjectsConfigurationsData: Set<CollectedConfigurationsForProject>
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
    allProjectsDependencyData: Set<CollectedDependenciesForProject>
  ): DirectDependenciesJsReportModel {
    return DirectDependenciesJsReportModel(
      allProjectsDependencyData.associate { it.path to it.directDependencies }
    )
  }
}
