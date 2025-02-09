package com.squareup.invert.common.pages


import PagingConstants
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
import com.squareup.invert.common.pages.PluginDetailNavRoute.Companion.parser
import com.squareup.invert.models.GradlePluginId
import org.jetbrains.compose.web.dom.Br
import ui.BootstrapClickableList
import ui.BootstrapLoadingSpinner
import ui.TitleRow
import kotlin.reflect.KClass

data class PluginDetailNavRoute(
    val pluginId: GradlePluginId,
) : BaseNavRoute(PluginDetailReportPage.navPage) {
    override fun toSearchParams() = toParamsWithOnlyPageId(this)
        .apply {
            this[PLUGIN_ID_PARAM] = pluginId
        }

    companion object {

        const val PLUGIN_ID_PARAM = "id"
        fun parser(params: Map<String, String?>): NavRoute {
            params[PLUGIN_ID_PARAM]?.let {
                return PluginDetailNavRoute(it)
            }
            return GradlePluginsNavRoute(null)
        }
    }
}

object PluginDetailReportPage : InvertReportPage<PluginDetailNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "plugin_detail",
        navRouteParser = { parser(it) }
    )

    override val navRouteKClass: KClass<PluginDetailNavRoute> = PluginDetailNavRoute::class

    override val composableContent: @Composable (PluginDetailNavRoute) -> Unit = { navRoute ->
        PluginDetailComposable(navRoute)
    }
}

@Composable
fun PluginDetailComposable(
    navRoute: PluginDetailNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val pluginId = navRoute.pluginId
    val pluginIdToModulesMap by reportDataRepo.pluginIdToAllModulesMap.collectAsState(mapOf())
    val modules = pluginIdToModulesMap?.get(pluginId)
    if (modules == null) {
        BootstrapLoadingSpinner()
    } else {
        TitleRow("Plugin Detail ${navRoute.pluginId} is used by ${modules.size} Modules:")
        Br()
        BootstrapClickableList("Modules Using ${pluginId}", modules, PagingConstants.MAX_RESULTS) { gradlePath ->
            navRouteRepo.pushNavRoute(ModuleDetailNavRoute(gradlePath))
        }
    }

}