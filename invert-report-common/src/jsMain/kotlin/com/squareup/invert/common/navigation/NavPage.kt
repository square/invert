package com.squareup.invert.common.navigation

import com.squareup.invert.common.navigation.routes.*
import com.squareup.invert.common.pages.AllModulesReportPage
import com.squareup.invert.common.pages.AllStatsReportPage
import com.squareup.invert.common.pages.AnnotationProcessorsReportPage
import com.squareup.invert.common.pages.HomeReportPage

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
            OwnersNavRoute.navPage,
            PluginsNavRoute.navPage,
            ModuleConsumptionNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            ArtifactsNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            ConfigurationsNavRoute.navPage,
            DependencyDiffNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            LeafModulesNavRoute.navPage,
            UnusedModulesNavRoute().navPage,// DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            ModuleDependencyGraphNavRoute().navPage,// DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
        ).filterNotNull()
    }
}