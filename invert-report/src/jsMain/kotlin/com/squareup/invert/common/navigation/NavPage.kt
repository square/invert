package com.squareup.invert.common.navigation

import com.squareup.invert.common.pages.*

typealias NavPageId = String

class NavPage(
    val pageId: NavPageId,
    val navRouteParser: (params: Map<String, String?>) -> NavRoute,
    val displayName: String = pageId,
    val navIconSlug: String = "arrow-right-circle",
) {

    data class NavPageGroup(
        val groupTitle: String,
        val navItems: Set<NavPage>,
    )

    companion object {

        val ROOT_NAV_ITEMS
            get() = listOf(
                NavPageGroup(
                    "General", setOf(
                        HomeReportPage.navPage,
                        AllStatsReportPage.navPage,
                        OwnersReportPage.navPage,
                    )
                ),
                NavPageGroup(
                    "Gradle", setOf(
                        AllModulesReportPage.navPage,
                        ArtifactsReportPage.navPage,
                        ModuleConsumptionReportPage.navPage,
                        ConfigurationsNavRoute.navPage,
                        DependencyDiffReportPage.navPage,
                        LeafModulesNavRoute.navPage,
                        UnusedModulesReportPage.navPage,
                        ModuleDependencyGraphReportPage.navPage,
                        PluginsNavRoute.navPage,
                        AnnotationProcessorsReportPage.navPage,
                        KotlinCompilerPluginsReportPage.navPage,
                    )
                ),
                NavPageGroup(
                    "Add Ons", setOf(
                        DependencyInjectionReportPage.navPage,
                    )
                )
            )
    }
}