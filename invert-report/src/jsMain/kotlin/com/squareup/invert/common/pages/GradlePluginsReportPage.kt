package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.charts.ChartJsChartComposable
import com.squareup.invert.common.charts.ChartsJs
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.models.GradlePluginId
import ui.BootstrapClickableList
import ui.BootstrapColumn
import ui.BootstrapLoadingSpinner
import ui.BootstrapRow
import ui.BootstrapSearchBox
import ui.TitleRow
import kotlin.reflect.KClass

data class GradlePluginsNavRoute(
    val query: String?,
) : BaseNavRoute(GradlePluginsReportPage.navPage) {

    override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
        .also { params ->
            query?.let {
                params[QUERY_PARAM] = query
            }
        }

    companion object {

        private const val QUERY_PARAM = "query"

        fun parser(params: Map<String, String?>): ArtifactsNavRoute {
            return ArtifactsNavRoute(
                query = params[QUERY_PARAM]
            )
        }
    }
}

object GradlePluginsReportPage : InvertReportPage<GradlePluginsNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "plugins",
        displayName = "Gradle Plugins",
        navIconSlug = "plugin",
        navRouteParser = { GradlePluginsNavRoute.parser(it) }
    )

    override val navRouteKClass: KClass<GradlePluginsNavRoute> = GradlePluginsNavRoute::class

    override val composableContent: @Composable (GradlePluginsNavRoute) -> Unit = { navRoute ->
        PluginsComposable(navRoute)
    }
}

@Composable
fun PluginsComposable(
    navRoute: GradlePluginsNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val allPluginIds: List<GradlePluginId>? by reportDataRepo.allPluginIds.collectAsState(null)

    val pluginIdToModulesMap by reportDataRepo.pluginIdToAllModulesMap.collectAsState(mapOf())

    if (allPluginIds == null || pluginIdToModulesMap == null) {
        BootstrapLoadingSpinner()
        return
    }

    val pluginIdToCount = allPluginIds!!
        .associateWith { pluginIdToModulesMap!!.getValue(it).size }
        .entries
        .sortedByDescending { it.value }

    val count = allPluginIds!!.size
    TitleRow("Applied Plugins ($count Total)")

    BootstrapSearchBox(
        navRoute.query ?: "",
        "Search For Artifact..."
    ) {
        navRouteRepo.updateNavRoute(
            ArtifactsNavRoute(it)
        )
    }
    BootstrapRow {
        BootstrapColumn(6) {
            BootstrapClickableList("Plugins", allPluginIds!!, MAX_RESULTS) { gradlePluginId ->
                navRouteRepo.updateNavRoute(PluginDetailNavRoute(gradlePluginId))
            }
        }
        BootstrapColumn(6) {
            ChartJsChartComposable(
                type = "bar",
                data = ChartsJs.ChartJsData(
                    labels = pluginIdToCount.map { it.key },
                    datasets = listOf(
                        ChartsJs.ChartJsDataset(
                            label = "Plugin",
                            data = pluginIdToCount.map { it.value }
                        )
                    )
                ),
                onClick = { label, value ->
                    navRouteRepo.updateNavRoute(
                        PluginDetailNavRoute(
                            pluginId = label,
                        )
                    )
                }
            )
        }
    }
}


