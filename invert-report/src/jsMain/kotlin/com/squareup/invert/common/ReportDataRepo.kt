package com.squareup.invert.common

import com.squareup.invert.common.PerformanceAndTiming.computeMeasureDuration
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.pages.InvertedDependenciesNavRoute
import com.squareup.invert.common.utils.DependencyComputations
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDataRepo(
  private val navRoute: Flow<NavRouteRepo.NavChangeEvent>,
  private val collectedDataRepo: CollectedDataRepo,
) {

  val reportMetadata: Flow<MetadataJsReportModel?> = collectedDataRepo.reportMetadata

  val allModules: Flow<List<ModulePath>?> = collectedDataRepo.home
    .mapLatest {
      computeMeasureDuration("allModules") {
        it?.modules?.sorted()
      }
    }

  val statsData: Flow<StatsJsReportModel?> = collectedDataRepo.statsData

  val historicalData: Flow<List<HistoricalData>?> = collectedDataRepo.historicalData.mapLatest { it }

  val statInfos: Flow<Collection<StatMetadata>?> = collectedDataRepo.statsData.mapLatest { it?.statInfos?.values }

  val statTotals: Flow<CollectedStatTotalsJsReportModel?> = collectedDataRepo.statTotals

  val collectedPluginInfoReport: Flow<PluginsJsReportModel?> = collectedDataRepo.collectedPluginInfoReport

  val moduleToOwnerMap: Flow<Map<ModulePath, OwnerName>?> = collectedDataRepo.ownersInfo.mapLatest {
    it?.modules
  }

  val moduleQuery: Flow<String?> = navRoute
    .filterIsInstance<InvertedDependenciesNavRoute>()
    .mapLatest { it.moduleQuery }
    .distinctUntilChanged()

  val allInvertedDependencies: Flow<Map<DependencyId, Map<ModulePath, List<ConfigurationName>>>?> =
    collectedDataRepo.combinedReportData
      .mapLatest { it?.invertedDependencies }

  val allDependencyIds: Flow<Set<DependencyId>?> =
    collectedDataRepo.combinedReportData
      .mapLatest { it?.invertedDependencies }
      .map { it?.keys }


  val allPlugins: Flow<Map<ModulePath, List<GradlePluginId>>?> =
    collectedPluginInfoReport.mapLatest { it?.modules }

  val allModulesMatchingQuery = moduleQuery.combine(allModules) { query, allModules ->
    computeMeasureDuration("allModulesMatchingQuery") {
      if (query != null) {
        allModules?.filter { it.contains(query) }?.sorted()
      } else {
        listOf()
      }
    }
  }

  val allDirectDependencies: Flow<Map<ModulePath, Map<ConfigurationName, Set<DependencyId>>>?> =
    collectedDataRepo.directDependenciesData.mapLatest {
      it?.directDependencies
    }

  val allPluginIds: Flow<List<GradlePluginId>?> =
    collectedDataRepo.home.mapLatest {
      it?.plugins?.filter { !it.endsWith("\$Inject") }?.toList()?.sorted()
    }

  val allAnalyzedConfigurationNames: Flow<Set<String>?> = collectedDataRepo.configurations.mapLatest {
    it?.configurationNameToModules?.keys
  }

  val allAvailableConfigurationNames: Flow<Set<String>?> = collectedDataRepo.configurations.mapLatest {
    it?.allConfigurationNames
  }

  val pluginIdToAllModulesMap: Flow<Map<GradlePluginId, List<ModulePath>>?> =
    collectedDataRepo.collectedPluginInfoReport
      .mapLatest {
        computeMeasureDuration("pluginIdToAllModulesMap") {
          it?.plugins
        }
      }

  val allOwnerNames: Flow<List<String>?> = collectedDataRepo.ownersInfo
    .mapLatest { ownersInfo ->
      computeMeasureDuration("allOwners") {
        ownersInfo?.teams?.sorted()
      }
    }

  val ownerNameToModulesMap: Flow<Map<OwnerName, List<ModulePath>>?> = collectedDataRepo.ownersInfo
    .mapLatest { ownersInfo ->
      computeMeasureDuration("ownerToModulesMap") {
        ownersInfo?.modules?.entries
          ?.groupBy { entry: Map.Entry<ModulePath, OwnerName> -> entry.value }
          ?.mapValues { entry -> entry.value.map { it.key } }
      }
    }

  val allArtifacts: Flow<List<String>?> = collectedDataRepo.home
    .mapLatest {
      computeMeasureDuration("allArtifacts") {
        it?.artifacts?.sorted()
      }
    }


  fun moduleDirectlyUsedBy(path: ModulePath): Flow<Map<ModulePath, List<ConfigurationName>>?> =
    collectedDataRepo.directDependenciesData.mapLatest { directDependenciesData ->
      val directDependencies = directDependenciesData?.directDependencies
      directDependencies?.entries?.filter { (modulePath, configurationToDependencyIds) ->
        configurationToDependencyIds.any { it.value.contains(path) }
      }?.associate { (modulePath, configurationToDependencyIds) ->
        modulePath to configurationToDependencyIds.keys.toList()
      }
    }

  fun moduleTransitivelyUsedBy(path: ModulePath): Flow<Map<ModulePath, List<ConfigurationName>>?> =
    allInvertedDependencies.mapLatest { allInvertedDependenciesMap: Map<DependencyId, Map<ModulePath, List<ConfigurationName>>>? ->
      if (allInvertedDependenciesMap != null) {
        allInvertedDependenciesMap[path] ?: mapOf()
      } else {
        null
      }
    }


  fun directDependenciesOf(modulePath: ModulePath?): Flow<Map<ConfigurationName, Set<DependencyId>>?> =
    allDirectDependencies.mapLatest { directDependenciesData ->
      directDependenciesData?.get(modulePath)
    }

  fun statsForKey(statKey: StatKey): Flow<MutableList<ModuleOwnerAndCodeReference>> =
    statsData.mapLatest { statsJsReportModel ->
      val allForKey = mutableListOf<ModuleOwnerAndCodeReference>()
      moduleToOwnerMap.first().also { moduleToOwnerMap ->
        statsJsReportModel?.statsByModule?.map { moduleToStatsMap ->
          moduleToStatsMap.value[statKey]?.let { stat: Stat ->
            val moduleName = moduleToStatsMap.key

            if (stat is Stat.CodeReferencesStat) {
              stat.value.forEach { codeReference ->
                allForKey.add(
                  ModuleOwnerAndCodeReference(
                    codeReference = codeReference,
                    module = moduleName,
                    owner = codeReference.owner ?: moduleToOwnerMap?.get(moduleName) ?: "",
                    metadata = statsJsReportModel.statInfos[statKey]!!
                  )
                )
              }
            }
          }
        }
      }
      allForKey
    }

  fun dependenciesOf(modulePath: ModulePath?): Flow<Map<ConfigurationName, List<DependencyId>>?> =
    collectedDataRepo.combinedReportData.mapLatest { combinedReport ->
      if (combinedReport != null) {
        DependencyComputations.dependenciesOf(
          modulePath = modulePath,
          combinedReport = combinedReport
        )
      } else {
        null
      }
    }

  fun modulesWithPlugin(pluginId: GradlePluginId): Flow<List<GradlePluginId>?> {
    return pluginIdToAllModulesMap.mapLatest { it?.get(pluginId) }
  }
}

data class ModuleOwnerAndCodeReference(
  val module: ModulePath,
  val owner: OwnerName,
  val codeReference: Stat.CodeReferencesStat.CodeReference,
  val metadata: StatMetadata,
)
