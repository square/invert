package com.squareup.invert.common

import com.squareup.invert.common.PerformanceAndTiming.computeMeasureDuration
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.pages.InvertedDependenciesNavRoute
import com.squareup.invert.common.utils.DependencyComputations
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.DependencyId
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.CollectedStatTotalsJsReportModel
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
  private val navRoute: Flow<NavRoute>,
  private val collectedDataRepo: CollectedDataRepo,
) {

  val reportMetadata: Flow<MetadataJsReportModel?> = collectedDataRepo.reportMetadata


  val allModules: Flow<List<GradlePath>?> = collectedDataRepo.home
    .mapLatest {
      computeMeasureDuration("allModules") {
        it?.modules?.sorted()
      }
    }

  val statsData: Flow<StatsJsReportModel?> = collectedDataRepo.statsData

  val diProvidesAndInjects: Flow<List<DiProvidesAndInjectsItem>> =
    statsData.mapLatest { statsData: StatsJsReportModel? ->
      val STAT_KEY = "DiProvidesAndInjects"
      val diRowDataRows = mutableListOf<DiProvidesAndInjectsItem>()
      val statsByModule: Map<GradlePath, Map<StatKey, Stat>>? = statsData?.statsByModule
      statsByModule?.forEach { (moduleGradlePath, statsDataForModule) ->
        val stat = statsDataForModule[STAT_KEY]
        if (stat is Stat.DiProvidesAndInjectsStat) {
          stat.value.forEach { providesAndInjects ->
            if (providesAndInjects.contributions.isNotEmpty()) {
              providesAndInjects.contributions.forEach { contribution ->
                diRowDataRows.add(
                  DiProvidesAndInjectsItem.Provides(
                    module = moduleGradlePath,
                    filePath = providesAndInjects.filePath,
                    startLine = providesAndInjects.startLine,
                    endLine = providesAndInjects.endLine,
                    type = contribution.boundType,
                    implementationType = contribution.boundImplementation,
                    scope = null,
                    qualifiers = listOf()
                  )
                )
              }
            }

            if (providesAndInjects.consumptions.isNotEmpty()) {
              providesAndInjects.consumptions.forEach { consumption ->
                diRowDataRows.add(
                  DiProvidesAndInjectsItem.Injects(
                    module = moduleGradlePath,
                    filePath = providesAndInjects.filePath,
                    startLine = consumption.startLine,
                    endLine = consumption.endLine,
                    type = consumption.type,
                    qualifiers = consumption.qualifierAnnotations,
                  )
                )
              }
            }
          }
        }
      }
      diRowDataRows
    }


  fun diProvidesAndInjects(
    moduleGradlePaths: List<GradlePath>? = null,
  ): Flow<List<DiProvidesAndInjectsItem>> =
    diProvidesAndInjects.mapLatest { diProvidesAndInjects: List<DiProvidesAndInjectsItem> ->
      diProvidesAndInjects.filter {
        when (it) {
          is DiProvidesAndInjectsItem.Injects -> moduleGradlePaths?.contains(it.module) ?: true
          is DiProvidesAndInjectsItem.Provides -> moduleGradlePaths?.contains(it.module) ?: true
        }
      }
    }

  fun diInjects(moduleGradlePaths: List<GradlePath>): Flow<List<DiProvidesAndInjectsItem.Injects>> =
    diProvidesAndInjects(moduleGradlePaths).mapLatest {
      it.filterIsInstance<DiProvidesAndInjectsItem.Injects>().sortedBy { it.type }
    }

  fun diProvides(): Flow<List<DiProvidesAndInjectsItem.Provides>> =
    diProvidesAndInjects.mapLatest {
      it.filterIsInstance<DiProvidesAndInjectsItem.Provides>().sortedBy { it.type }
    }


  fun diProvides(diKey: DiKey): Flow<List<DiProvidesAndInjectsItem.Provides>> =
    diProvides().mapLatest { providesList ->
      providesList.filter { it.key == diKey }
    }

//    fun providesFor(diInjectsItem: DiProvidesAndInjectsItem.Injects) {}

  val statInfos: Flow<Collection<StatMetadata>?> = collectedDataRepo.statsData.mapLatest { it?.statInfos?.values }

  val statTotals: Flow<CollectedStatTotalsJsReportModel?> = collectedDataRepo.statTotals

  val collectedPluginInfoReport: Flow<PluginsJsReportModel?> = collectedDataRepo.collectedPluginInfoReport

  val moduleToOwnerMap: Flow<Map<GradlePath, OwnerName>?> = collectedDataRepo.ownersInfo.mapLatest {
    it?.modules
  }

  val moduleQuery: Flow<String?> = navRoute
    .filterIsInstance<InvertedDependenciesNavRoute>()
    .mapLatest { it.moduleQuery }
    .distinctUntilChanged()

  val allInvertedDependencies: Flow<Map<DependencyId, Map<GradlePath, List<ConfigurationName>>>?> =
    collectedDataRepo.combinedReportData
      .mapLatest { it?.invertedDependencies }

  val allDependencyIds: Flow<Set<DependencyId>?> =
    collectedDataRepo.combinedReportData
      .mapLatest { it?.invertedDependencies }
      .map { it?.keys }


  val allPlugins: Flow<Map<GradlePath, List<GradlePluginId>>?> =
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

  val allDirectDependencies: Flow<Map<GradlePath, Map<ConfigurationName, Set<DependencyId>>>?> =
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

  val pluginIdToAllModulesMap: Flow<Map<GradlePluginId, List<GradlePath>>?> =
    collectedDataRepo.collectedPluginInfoReport
      .mapLatest {
        computeMeasureDuration("pluginIdToAllModulesMap") {
          it?.plugins
        }
      }

  val allOwnerNames: Flow<List<String>?> = collectedDataRepo.ownersInfo
    .mapLatest { ownersInfo ->
      computeMeasureDuration("allOwners") {
        ownersInfo?.teams?.keys?.sorted()
      }
    }

  val ownerNameToModulesMap: Flow<Map<OwnerName, List<GradlePath>>?> = collectedDataRepo.ownersInfo
    .mapLatest { ownersInfo ->
      computeMeasureDuration("ownerToModulesMap") {
        ownersInfo?.modules?.entries
          ?.groupBy { entry: Map.Entry<GradlePath, OwnerName> -> entry.value }
          ?.mapValues { entry -> entry.value.map { it.key } }
      }
    }

  val allArtifacts: Flow<List<String>?> = collectedDataRepo.home
    .mapLatest {
      computeMeasureDuration("allArtifacts") {
        it?.artifacts?.sorted()
      }
    }

  fun moduleTransitivelyUsedBy(path: GradlePath): Flow<Map<GradlePath, List<ConfigurationName>>?> =
    allInvertedDependencies.mapLatest { allInvertedDependenciesMap: Map<DependencyId, Map<GradlePath, List<ConfigurationName>>>? ->
      if (allInvertedDependenciesMap != null) {
        allInvertedDependenciesMap[path] ?: mapOf()
      } else {
        null
      }
    }


  fun directDependenciesOf(gradlePath: GradlePath?): Flow<Map<ConfigurationName, Set<DependencyId>>?> =
    collectedDataRepo.directDependenciesData.mapLatest { directDependenciesData ->
      directDependenciesData?.directDependencies?.get(gradlePath)
    }

  fun statsForKey(statKey: StatKey): Flow<MutableList<ModuleOwnerAndCodeReference>> =
    statsData.mapLatest { statsJsReportModel ->
      val allForKey = mutableListOf<ModuleOwnerAndCodeReference>()
      moduleToOwnerMap.first().also { moduleToOwnerMap ->
        statsJsReportModel?.statsByModule?.map { moduleToStatsMap ->
          moduleToStatsMap.value[statKey]?.let { stat: Stat ->
            println(stat)
            val moduleName = moduleToStatsMap.key
            val ownerName = moduleToOwnerMap?.get(moduleName) ?: ""

            if (stat is Stat.CodeReferencesStat) {
              stat.value.forEach { codeReference ->
                allForKey.add(
                  ModuleOwnerAndCodeReference(
                    codeReference = codeReference,
                    module = moduleName,
                    owner = ownerName
                  )
                )
              }
            }
          }
        }
      }
      allForKey
    }

  fun dependenciesOf(gradlePath: GradlePath?): Flow<Map<ConfigurationName, List<DependencyId>>?> =
    collectedDataRepo.combinedReportData.mapLatest { combinedReport ->
      if (combinedReport != null) {
        DependencyComputations.dependenciesOf(
          gradlePath = gradlePath,
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
  val module: GradlePath,
  val owner: OwnerName,
  val codeReference: Stat.CodeReferencesStat.CodeReference
)
