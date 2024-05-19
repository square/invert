package com.squareup.invert.common.pages


import androidx.compose.runtime.*
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.InvertedDependenciesNavRoute.Companion.parser
import com.squareup.invert.common.utils.DependencyComputations
import com.squareup.invert.models.ConfigurationName
import com.squareup.invert.models.GradlePath
import com.squareup.invert.models.GradlePluginId
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.*
import ui.*
import kotlin.reflect.KClass


data class InvertedDependenciesNavRoute(
    val pluginGroupByFilter: List<GradlePluginId> = listOf(),
    val configurations: List<ConfigurationName> = listOf(),
    val moduleQuery: String? = null,
) : BaseNavRoute(InvertedDependenciesReportPage.navPage) {
    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
        .apply {
            if (pluginGroupByFilter.isNotEmpty()) {
                this[PLUGIN_GROUP_BY_FILTER_QUERY_PARAM_NAME] =
                    pluginGroupByFilter.joinToString(separator = ",")
            }
            if (configurations.isNotEmpty()) {
                this[CONFIGURATIONS_QUERY_PARAM_NAME] = configurations.joinToString(separator = ",")
            }
            moduleQuery?.let {
                this[MODULE_QUERY_PARAM_NAME] = moduleQuery
            }
        }

    companion object {

        private const val MODULE_QUERY_PARAM_NAME = "module"
        private const val PLUGIN_GROUP_BY_FILTER_QUERY_PARAM_NAME = "plugin_id"
        private const val CONFIGURATIONS_QUERY_PARAM_NAME = "configurations"
        fun parser(params: Map<String, String?>): InvertedDependenciesNavRoute = InvertedDependenciesNavRoute(
            pluginGroupByFilter = params[PLUGIN_GROUP_BY_FILTER_QUERY_PARAM_NAME]?.split(",")
                ?.filter { it.isNotBlank() } ?: listOf(),
            configurations = params[CONFIGURATIONS_QUERY_PARAM_NAME]?.split(",")
                ?.filter { it.isNotBlank() }
                ?: listOf(),
            moduleQuery = params[MODULE_QUERY_PARAM_NAME]
        )
    }
}


object InvertedDependenciesReportPage : InvertReportPage<InvertedDependenciesNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "inverted",
        displayName = "Inverted Dependencies",
        navIconSlug = "bar-chart",
        navRouteParser = { parser(it) }
    )

    override val navRouteKClass: KClass<InvertedDependenciesNavRoute> = InvertedDependenciesNavRoute::class

    override val composableContent: @Composable (InvertedDependenciesNavRoute) -> Unit = { navRoute ->
        InverteDependenciesComposable(navRoute)
    }
}


@Composable
fun InverteDependenciesComposable(
    navRoute: InvertedDependenciesNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    BootstrapTabPane(
        listOf(
            BootstrapTabData("Results") {
                if (navRoute.pluginGroupByFilter.isEmpty() || navRoute.configurations.isEmpty()) {
                    BootstrapJumbotron(
                        headerContent = {
                            BootstrapIcon("exclamation-triangle", 48)
                            Text(" No configuration provided.")
                        }
                    ) {
                        P({
                            classes("text-center")
                        }) {
                            Text("Plugins and Configuration Names are not selected.  Go to the settings tab, set them and return to this page.")
                        }
                    }
                } else {
                    TitleRow("Module Consumption (Grouped By Plugin Type)")
                    val query by reportDataRepo.moduleQuery.collectAsState(null)
                    BootstrapSearchBox(query ?: "", "Search For Module...") {
                        navRouteRepo.updateNavRoute(
                            navRoute.copy(
                                moduleQuery = it
                            )
                        )
                    }
                    BootstrapRow {
                        BootstrapColumn(6) {
                            val allCountModules by reportDataRepo.allModules.collectAsState(null)
                            val matching by reportDataRepo.allModulesMatchingQuery.collectAsState(null)
                            val totalCount = allCountModules?.size
                            GenericList(
                                "Matching ${matching?.size} of $totalCount",
                                matching ?: listOf(),
                                onItemClick = {
                                    navRouteRepo.updateNavRoute(
                                        navRoute.copy(
                                            moduleQuery = it
                                        )
                                    )
                                })
                        }
                        BootstrapColumn(6) {
                            RightColumn(reportDataRepo, navRoute)
                        }
                    }
                }
            },
            BootstrapTabData("Settings") {
                SettingsComposable(
                    reportDataRepo = reportDataRepo,
                    navRouteRepo = navRouteRepo
                )
            },
        )
    )

}

@Composable
fun SettingsComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo) {
    val navRoute by navRouteRepo.navRoute.collectAsState(null)
    if (navRoute == null) {
        return
    } else {
        val invertedDependenciesNavRoute = navRoute!! as InvertedDependenciesNavRoute
        val groupByFilterItems = invertedDependenciesNavRoute.pluginGroupByFilter
        H1 {
            Text("Module Consumption Settings")
        }
        Br {}
        BootstrapRow {
            BootstrapColumn(6) {
                H5 { Text("Configurations") }
                val allConfigurationNames by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(listOf())
                BootstrapButton("Select All") {
                    navRouteRepo.updateNavRoute(
                        invertedDependenciesNavRoute.copy(configurations = allConfigurationNames?.toList() ?: listOf())
                    )
                }
                BootstrapButton("Unselect All") {
                    navRouteRepo.updateNavRoute(
                        invertedDependenciesNavRoute.copy(configurations = listOf())
                    )
                }
                allConfigurationNames?.sorted()?.forEach { configurationName ->
                    BootstrapSettingsCheckbox(
                        labelText = configurationName,
                        initialIsChecked = invertedDependenciesNavRoute.configurations.contains(configurationName)
                    ) { shouldAdd ->
                        navRouteRepo.updateNavRoute(
                            invertedDependenciesNavRoute.copy(
                                configurations = invertedDependenciesNavRoute.configurations
                                    .toMutableList()
                                    .apply {
                                        remove(configurationName)
                                        if (shouldAdd) {
                                            add(configurationName)
                                        }
                                    }
                            )
                        )
                    }
                }
            }
            BootstrapColumn(6) {
                H5 { Text("Plugins") }
                val allPluginIds by reportDataRepo.allPluginIds.collectAsState(listOf())
                BootstrapButton("Select All") {
                    navRouteRepo.updateNavRoute(
                        invertedDependenciesNavRoute.copy(
                            pluginGroupByFilter = allPluginIds ?: listOf()
                        )
                    )
                }
                BootstrapButton("Unselect All") {
                    navRouteRepo.updateNavRoute(
                        invertedDependenciesNavRoute.copy(
                            pluginGroupByFilter = listOf()
                        )
                    )
                }
                allPluginIds?.sorted()?.forEach { gradlePluginId ->
                    BootstrapSettingsCheckbox(
                        labelText = gradlePluginId,
                        initialIsChecked = groupByFilterItems.contains(gradlePluginId)
                    ) { shouldAdd ->
                        navRouteRepo.updateNavRoute(
                            invertedDependenciesNavRoute.copy(
                                pluginGroupByFilter = invertedDependenciesNavRoute.pluginGroupByFilter.toMutableList()
                                    .apply {
                                        remove(gradlePluginId)
                                        if (shouldAdd) {
                                            add(gradlePluginId)
                                        }
                                    }
                            )
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun RightColumn(
    reportDataRepo: ReportDataRepo,
    invertedDependenciesNavRoute: InvertedDependenciesNavRoute
) {
    val pluginIdToAllAppsMap: Map<GradlePluginId, List<GradlePath>>? by reportDataRepo.pluginIdToAllModulesMap.collectAsState(
        null
    )
    val invertedDeps by reportDataRepo.allInvertedDependencies.collectAsState(null)
    val allModulesMatchingQuery by reportDataRepo.allModulesMatchingQuery.collectAsState(null)
    val collectedPlugins by reportDataRepo.collectedPluginInfoReport.collectAsState(null)
    val pluginIdToGradlePathsMatchingQuery = DependencyComputations.computePluginIdToGradlePathsMatchingQuery(
        matchingQueryModulesList = allModulesMatchingQuery?.filter { it.startsWith(":") } ?: listOf(),
        pluginGroupByFilter = invertedDependenciesNavRoute.pluginGroupByFilter,
        configurations = invertedDependenciesNavRoute.configurations,
        invertedDeps = invertedDeps,
        collectedPlugins = collectedPlugins,
    )

    val limit = 10
    pluginIdToGradlePathsMatchingQuery
        .forEach { (pluginId: GradlePluginId, matchingModules: Map<GradlePath, Map<GradlePath, List<DependencyComputations.PathAndConfigurations>>>) ->
            val totalCount = pluginIdToAllAppsMap?.get(pluginId)?.size ?: 0
            val matchingCount = pluginIdToGradlePathsMatchingQuery[pluginId]?.size ?: 0

            val headerText = "$pluginId ($matchingCount of $totalCount) "
            var showAll by remember { mutableStateOf(false) }
            val matchingModulePaths = matchingModules.keys.toList()
            val expanded = false
            BoostrapExpandingCard(
                header = {
                    Text(headerText)
                },
                headerRight = {
                    Button({
                        classes("btn")
                    }) {
                        BootstrapIcon("copy", 16) {
                            window.navigator.clipboard.writeText(
                                matchingModulePaths.joinToString(
                                    separator = " "
                                )
                            )
                        }
                    }
                },
                expanded = expanded
            ) {
                var displayIndex = 1
                val matchingModulePathsLimited = matchingModulePaths
                    .sorted()
                    .subList(
                        0, minOf(
                            if (showAll) {
                                Int.MAX_VALUE
                            } else {
                                limit
                            }, matchingModules.size
                        )
                    )
                matchingModulePathsLimited
                    .forEach { matchingModulePath: GradlePath ->
                        BootstrapAccordion({
                            val accordionSubHeaderText = "$displayIndex $matchingModulePath"
                            Text(accordionSubHeaderText)
                            displayIndex++
                        }) {
                            val matchingUsages: List<DependencyComputations.PathAndConfigurations> =
                                matchingModules[matchingModulePath]?.get(matchingModulePath) ?: listOf()
                            BootstrapTable(
                                headers = listOf(
                                    "Referenced in",
                                    "in Configuration(s)"
                                ),
                                rows = matchingUsages.map { (gradlePath, configurationNames) ->
                                    listOf(
                                        gradlePath,
                                        configurationNames.toString()
                                    )
                                },
                                types = matchingModulePathsLimited.map { String::class },
                                maxResultsLimitConstant = 10,
                                onItemClick = null
                            )
                        }
                    }
                if (!showAll) {
                    Hr { }
                    Button({
                        classes("btn")
                        onClick {
                            showAll = !showAll
                        }
                    }) {
                        Text("Show All")
                    }
                }
            }
        }
}