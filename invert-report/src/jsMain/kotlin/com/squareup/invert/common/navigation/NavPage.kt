package com.squareup.invert.common.navigation

import com.squareup.invert.common.pages.*

typealias NavPageId = String

data class NavPage(
    val pageId: NavPageId,
    val navRouteParser: (params: Map<String, String?>) -> NavRoute,
    val displayName: String = pageId,
    val navIconSlug: String = "arrow-right-circle",
) {
    companion object {

        val ROOT_NAV_ITEMS = setOf(
            HomeReportPage.navPage,
            AllModulesReportPage.navPage,
            AllStatsReportPage.navPage,
            AnnotationProcessorsReportPage.navPage,
            OwnersReportPage.navPage,
            PluginsNavRoute.navPage,
            ModuleConsumptionReportPage.navPage,
            ArtifactsReportPage.navPage,
            ConfigurationsNavRoute.navPage,
            DependencyDiffReportPage.navPage,
            LeafModulesNavRoute.navPage,
            UnusedModulesReportPage.navPage,
            ModuleDependencyGraphReportPage.navPage,
        ).filterNotNull()
    }
}