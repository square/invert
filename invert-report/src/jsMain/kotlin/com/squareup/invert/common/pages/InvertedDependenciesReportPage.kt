package com.squareup.invert.common.pages


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Button
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import ui.BoostrapExpandingCard
import ui.BootstrapAccordion
import ui.BootstrapButton
import ui.BootstrapColumn
import ui.BootstrapIcon
import ui.BootstrapJumbotron
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import ui.BootstrapSearchBox
import ui.BootstrapSettingsCheckbox
import ui.BootstrapTabData
import ui.BootstrapTabPane
import ui.BootstrapTable
import ui.GenericList
import ui.MarkdownText
import ui.TitleRow
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
    val allModules by reportDataRepo.allModules.collectAsState(null)
    val allConfigurationNames by reportDataRepo.allAnalyzedConfigurationNames.collectAsState(listOf())
    val allPluginIds by reportDataRepo.allPluginIds.collectAsState(listOf())

    if (allModules == null || allConfigurationNames == null || allPluginIds == null) {
        BootstrapLoadingMessageWithSpinner()
        return
    }

    BootstrapTabPane(
        listOf(
            BootstrapTabData("Results") {
                if (navRoute.pluginGroupByFilter.isEmpty() || navRoute.configurations.isEmpty()) {
                    BootstrapJumbotron(
                        headerContent = {
//                            BootstrapIcon("exclamation-triangle", 48)
                            Text(" Inverted Dependency Search")
                        }
                    ) {
                        Br()
                        P({
//                            classes("text-center")
                        }) {
                            MarkdownText(
                                """
                                ## Find modules that depend on your selected module(s)
                                1. Click the 'Settings' tab, select the scanned configurations you are interested in.
                                1. Selected the Gradle configuration(s) you are interested in
                                1. Select the Plugin(s) to group the search results from.
                                1. Click the 'Results' tab, and then find you target module using the search box.
                                """.trimIndent()
                            )
                            Br()
                            Text("or... ")
                            Br()
                            BootstrapButton("Start by Searching Everything") {
                                navRouteRepo.updateNavRoute(
                                    InvertedDependenciesNavRoute(
                                        pluginGroupByFilter = allPluginIds!!,
                                        configurations = allConfigurationNames!!.toList(),
                                        moduleQuery = ":"
                                    )
                                )
                            }
                        }
                    }
                } else {
                    TitleRow("Inverted Dependency Search (Grouped By Plugin Type)")
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
                            val matching by reportDataRepo.allModulesMatchingQuery.collectAsState(null)
                            val totalCount = allModules?.size
                            GenericList(
                                "Matching ${matching?.size} of $totalCount",
                                matching ?: listOf(),
                                onItemClick = {
                                    navRouteRepo.updateNavRoute(
                                        navRoute.copy(
                                            moduleQuery = it
                                        )
                                    )
                                }
                            )
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
                    navRouteRepo = navRouteRepo,
                    allConfigurationNames = allConfigurationNames?.toList() ?: emptyList(),
                    allPluginIds = allPluginIds?.toList() ?: emptyList()
                )
            },
        )
    )
}

@Composable
fun SettingsComposable(
    reportDataRepo: ReportDataRepo,
    navRouteRepo: NavRouteRepo,
    allConfigurationNames: List<ConfigurationName>,
    allPluginIds: List<GradlePluginId>,
) {
    val navRoute by navRouteRepo.navRoute.collectAsState(null)
    if (navRoute == null) {
        return
    } else {
        val invertedDependenciesNavRoute = navRoute!! as InvertedDependenciesNavRoute
        H1 {
            Text("Inverted Transitive Dependency Search Settings")
        }
        Br {}
        BootstrapRow {
            BootstrapColumn(6) {
                H5 { Text("Configurations") }
                BootstrapButton("Select All") {
                    navRouteRepo.updateNavRoute(
                        invertedDependenciesNavRoute.copy(configurations = allConfigurationNames)
                    )
                }
                BootstrapButton("Unselect All") {
                    navRouteRepo.updateNavRoute(
                        invertedDependenciesNavRoute.copy(configurations = listOf())
                    )
                }
                allConfigurationNames.sorted().forEach { configurationName ->
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
                    val groupByFilterItems = invertedDependenciesNavRoute.pluginGroupByFilter
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
                                onItemClickCallback = null
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