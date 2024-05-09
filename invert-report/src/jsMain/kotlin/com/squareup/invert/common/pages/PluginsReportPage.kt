package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.navigation.routes.PluginDetailNavRoute
import ui.BootstrapClickableList
import ui.BootstrapLoadingSpinner
import ui.TitleRow
import kotlin.reflect.KClass

object PluginsNavRoute : BaseNavRoute(PluginsReportPage.navPage)

object PluginsReportPage : InvertReportPage<PluginsNavRoute> {
    override val navPage: NavPage = NavPage(
        pageId = "plugins",
        displayName = "Plugins",
        navIconSlug = "plugin",
        navRouteParser = { PluginsNavRoute },
    )

    override val navRouteKClass: KClass<PluginsNavRoute> = PluginsNavRoute::class

    override val composableContent: @Composable (PluginsNavRoute) -> Unit = { navRoute ->
        PluginsComposable(navRoute)
    }
}

@Composable
fun PluginsComposable(
    pluginsNavRoute: PluginsNavRoute,
    reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
    navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
    val pluginIdListOrig by reportDataRepo.allPluginIds.collectAsState(null)
    pluginIdListOrig?.let { pluginIds ->
        val count = pluginIds.size
        TitleRow("Applied Plugins ($count Total)")

        BootstrapClickableList("Plugins", pluginIds, MAX_RESULTS) { gradlePluginId ->
            navRouteRepo.updateNavRoute(PluginDetailNavRoute(gradlePluginId))
        }
    } ?: BootstrapLoadingSpinner()
}


