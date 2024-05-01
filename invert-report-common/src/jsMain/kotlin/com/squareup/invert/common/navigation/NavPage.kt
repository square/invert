package com.squareup.invert.common.navigation

import com.squareup.invert.common.navigation.routes.*

typealias NavPageId = String

data class NavPage(
    val pageId: NavPageId,
    val navRouteParser: (params: Map<String, String?>) -> NavRoute,
    val displayName: String = pageId,
    val navIconSlug: String = "arrow-right-circle",
) {
    companion object {

        val ROOT_NAV_ITEMS = setOf(
            HomeNavRoute.navPage, // ISNT SHOWING UP???
            AllModulesNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            OwnersNavRoute.navPage,
            PluginsNavRoute.navPage,
            ModuleConsumptionNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            AnnotationProcessorsNavRoute.navPage,
            ArtifactsNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            ConfigurationsNavRoute.navPage,
            AllStatsNavRoute().navPage,// DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            DependencyDiffNavRoute().navPage, // DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            LeafModulesNavRoute.navPage,
            UnusedModulesNavRoute().navPage,// DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
            ModuleDependencyGraphNavRoute().navPage,// DONT USE DEFAULT CONSTRUCTOR TO GET NAVPAGE
        ).filterNotNull()
    }
}