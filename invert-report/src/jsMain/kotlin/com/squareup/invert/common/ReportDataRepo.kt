package com.squareup.invert.common

import com.squareup.invert.common.PerformanceAndTiming.computeMeasureDuration
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.pages.InvertedDependenciesNavRoute
import com.squareup.invert.common.utils.DependencyComputations
import com.squareup.invert.models.*
import com.squareup.invert.models.js.MetadataJsReportModel
import com.squareup.invert.models.js.PluginsJsReportModel
import com.squareup.invert.models.js.StatsJsReportModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDataRepo(
    private val navRoute: Flow<NavRoute>,
    private val collectedDataRepo: CollectedDataRepo,
) {

    val reportMetadata: Flow<MetadataJsReportModel?> = collectedDataRepo.reportMetadata

    val statsData: Flow<StatsJsReportModel?> = collectedDataRepo.statsData

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

    val allModules: Flow<List<GradlePath>?> = collectedDataRepo.home
        .mapLatest {
            computeMeasureDuration("allModules") {
                it?.modules?.sorted()
            }
        }

    val allPlugins: Flow<Map<GradlePath, List<GradlePluginId>>?> = collectedPluginInfoReport.mapLatest { it?.modules }

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

    fun moduleUsedBy(path: GradlePath): Flow<Map<GradlePath, List<ConfigurationName>>?> =
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
