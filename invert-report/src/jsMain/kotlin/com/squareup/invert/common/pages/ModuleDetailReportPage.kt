package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.ModuleDetailNavRoute.Companion.parser
import com.squareup.invert.models.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.Text
import ui.*
import kotlin.reflect.KClass

data class ModuleDetailNavRoute(
    val path: GradlePath,
    val configurationName: ConfigurationName? = null,
) : BaseNavRoute(ModuleDetailReportPage.navPage) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)
        .also { map ->
            map[PATH_PARAM] = path
            configurationName?.let {
                map[CONFIGURATION_PARAM] = it
            }
        }

    companion object {

        private const val PATH_PARAM = "path"
        private const val CONFIGURATION_PARAM = "configuration"
        fun parser(params: Map<String, String?>): NavRoute {
            val path = params[PATH_PARAM]
            val configurationName = params[CONFIGURATION_PARAM]
            return if (!path.isNullOrEmpty()) {
                ModuleDetailNavRoute(
                    path = path,
                    configurationName = configurationName
                )
            } else {
                AllModulesNavRoute()
            }
        }
    }
}

object ModuleDetailReportPage : InvertReportPage<ModuleDetailNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "module_detail",
        navRouteParser = { parser(it) }
    )
    override val navRouteKClass: KClass<ModuleDetailNavRoute> = ModuleDetailNavRoute::class

    override val composableContent: @Composable (ModuleDetailNavRoute) -> Unit = { navRoute ->
        ModuleDetailComposable(navRoute)
    }
}

private data class DependencyIdAndConfiguration(
    val dependencyId: DependencyId,
    val configurationName: ConfigurationName,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun ModuleDetailComposable(
    navRoute: ModuleDetailNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val modulePath = navRoute.path

    val pluginsForModule by reportDataRepo.allPlugins.mapLatest { it?.get(navRoute.path) }.collectAsState(null)

    val directDependenciesMapOrig by reportDataRepo.directDependenciesOf(modulePath).collectAsState(null)
    val moduleToOwnerMapCollected by reportDataRepo.moduleToOwnerMap.collectAsState(null)
    val statInfos by reportDataRepo.statInfos.collectAsState(null)

    if (moduleToOwnerMapCollected == null || pluginsForModule == null || statInfos == null) {
        H2 {
            BootstrapLoadingMessageWithSpinner()
        }
        return
    }
    val moduleToOwnerMap = moduleToOwnerMapCollected!!
    val ownerName: OwnerName? = moduleToOwnerMap[modulePath]
    ownerName?.let {
        H2 {
            Text("Module $modulePath is owned by ")
            AppLink({
                onClick {
                    navRouteRepo.updateNavRoute(OwnerDetailNavRoute(ownerName))
                }
            }) {
                Text(ownerName)
            }
            Br { }
            Br { }
        }
    }

    if (directDependenciesMapOrig == null) {
        BootstrapLoadingMessageWithSpinner("Loading Direct Dependencies...")
        return
    }

    val directDependenciesMap = directDependenciesMapOrig!!

    val pageTabs = mutableListOf<BootstrapTabData>()
    pageTabs.add(
        BootstrapTabData(tabName = "Stats") {
            val statsForModule: Map<StatKey, Stat>? by reportDataRepo.statsData.map { it?.statsByModule?.get(navRoute.path) }
                .collectAsState(null)

            if (statsForModule != null && statInfos != null) {
                val statsForModuleMap: Map<StatMetadata?, Stat> = statsForModule!!.mapKeys {
                    statInfos!!.first { statInfo -> statInfo.key == it.key }
                }
                BootstrapTable(
                    headers = listOf("Stat", "Value"),
                    rows = statsForModuleMap.filter { it.key != null && it.value is Stat.NumericStat }
                        .map { (key, value) ->
                            listOf(key!!.description, (value as Stat.NumericStat).value.toString())
                        },
                    types = listOf(String::class, Int::class),
                    maxResultsLimitConstant = MAX_RESULTS,
                    onItemClick = {
                        navRouteRepo.updateNavRoute(
                            StatDetailNavRoute(
                                statKeys = listOf(statInfos!!.first { statInfo -> statInfo.description == it[0] }).map { it.key }
                            )
                        )
                    },
                    sortAscending = false,
                    sortByColumn = 0
                )
            }

        })
    pageTabs.add(
        BootstrapTabData(tabName = "Direct Dependencies") {

            val allDirectDependencyToConfigurationEntries = mutableSetOf<DependencyIdAndConfiguration>()
            directDependenciesMap.entries.forEach {
                it.value
                    .filter { it.startsWith(":") }
                    .forEach { depId: DependencyId ->
                        allDirectDependencyToConfigurationEntries.add(
                            DependencyIdAndConfiguration(
                                dependencyId = depId,
                                configurationName = it.key
                            )
                        )
                    }
            }
            val directDepsDependencyIdToConfigurations: Map<DependencyId, List<ConfigurationName>> =
                allDirectDependencyToConfigurationEntries
                    .groupBy { it.dependencyId }
                    .mapValues { entry ->
                        entry.value
                            .map { it.configurationName }
                            .distinct()
                            .sorted()
                    }

            BootstrapTable(
                headers = listOf("Module", "Configurations"),
                rows = directDepsDependencyIdToConfigurations.entries.map {
                    listOf(
                        it.key,
                        it.value.joinToString("\n")
                    )
                },
                types = listOf(String::class, String::class),
                maxResultsLimitConstant = MAX_RESULTS,
                onItemClick = {
                    navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it[0]))
                }
            )
        }
    )

    val configurationToDependencyMapCollected by reportDataRepo.dependenciesOf(navRoute.path).collectAsState(null)
    if (configurationToDependencyMapCollected != null) {
        pageTabs.add(BootstrapTabData(tabName = "Transitive Dependencies") {
            val configurationToDependencyMap = configurationToDependencyMapCollected!!
            val allTransitiveDependencyToConfigurationEntries = mutableSetOf<DependencyIdAndConfiguration>()
            configurationToDependencyMap.entries.forEach {
                it.value
                    .filter { it.startsWith(":") }
                    .forEach { depId: DependencyId ->
                        allTransitiveDependencyToConfigurationEntries.add(
                            DependencyIdAndConfiguration(
                                dependencyId = depId,
                                configurationName = it.key
                            )
                        )
                    }
            }

            val transitiveDepsDependencyIdToConfigurations: Map<DependencyId, List<ConfigurationName>> =
                allTransitiveDependencyToConfigurationEntries
                    .groupBy { it.dependencyId }
                    .mapValues { entry ->
                        entry.value
                            .map { it.configurationName }
                            .distinct()
                            .sorted()
                    }

            BootstrapTable(
                headers = listOf("Module", "Configurations"),
                rows = transitiveDepsDependencyIdToConfigurations.entries.map {
                    listOf(
                        it.key,
                        it.value.joinToString("\n")
                    )
                },
                types = listOf(String::class, String::class),
                maxResultsLimitConstant = MAX_RESULTS,
                onItemClick = {
                    navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it[0]))
                }
            )
        })
    }

    val moduleUsageCollected by reportDataRepo.moduleTransitivelyUsedBy(modulePath).collectAsState(null)
    moduleUsageCollected?.let {
        pageTabs.add(BootstrapTabData(tabName = "Used Transitively By") {
            val moduleUsage = moduleUsageCollected!!
            BootstrapTable(
                headers = listOf("Module", "Used in Configurations"),
                rows = moduleUsage.keys.map { key -> listOf(key, moduleUsage[key]?.joinToString() ?: "") },
                types = moduleUsage.keys.map { String::class },
                maxResultsLimitConstant = MAX_RESULTS,
                onItemClick = {
                    navRouteRepo.updateNavRoute(
                        ModuleDetailNavRoute(
                            it[0]
                        )
                    )
                }
            )
        })
    }

    pageTabs.add(BootstrapTabData(tabName = "Gradle Plugins") {
        val pluginsForModuleNonNull = pluginsForModule!!
        BootstrapClickableList("Gradle Plugins", pluginsForModuleNonNull, MAX_RESULTS) {
            navRouteRepo.updateNavRoute(
                PluginDetailNavRoute(
                    pluginId = it
                )
            )
        }
    })

    BootstrapTabPane(pageTabs)
}