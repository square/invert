package com.squareup.invert.common.navigation

import com.squareup.invert.common.pages.*

typealias NavPageId = String

class NavPage(
    val pageId: NavPageId,
    val navRouteParser: (params: Map<String, String?>) -> NavRoute,
    val displayName: String = pageId,
    val navIconSlug: String = "arrow-right-circle",
) {

    data class NavItem(
        val itemTitle: String,
        val navPage: NavPage? = null,
        val destinationNavRoute: NavRoute = navPage?.navRouteParser?.invoke(mapOf())
            ?: throw RuntimeException("Requires destinationNavRoute"),
        val matchesCurrentNavRoute: (NavRoute) -> Boolean = { currentNavRoute ->
            currentNavRoute::class == destinationNavRoute::class
        },
        val navIconSlug: String = navPage?.navIconSlug ?: "record"
    )

    data class NavPageGroup(
        val groupTitle: String,
        val navItems: Set<NavItem>,
    )


    companion object {
        fun Set<NavPage>.toNavItems(): Set<NavItem> {
            return map {
                NavItem(
                    itemTitle = it.displayName,
                    navPage = it,
                )
            }.toSet()
        }


        val ROOT_NAV_ITEMS
            get() = listOf(
                NavPageGroup(
                    "General", setOf(
                        HomeReportPage.navPage,
                        OwnersReportPage.navPage,
                    ).toNavItems()
                ),
                NavPageGroup(
                    "Gradle", setOf(
                        AllModulesReportPage.navPage,
                        ArtifactsReportPage.navPage,
                        ConfigurationsNavRoute.navPage,
                        PluginsNavRoute.navPage,
                        AnnotationProcessorsReportPage.navPage,
                        KotlinCompilerPluginsReportPage.navPage,
                    ).toNavItems()
                ),
                NavPageGroup(
                    "Stats", setOf(
                        AllStatsReportPage.navPage,
                        SuppressAnnotationGraphReportPage.navPage,
                    ).toNavItems().plus(
                        NavItem(
                            itemTitle = "@Suppress Usages",
                            destinationNavRoute = StatDetailNavRoute(
                                statKeys = listOf("SuppressAnnotationUsages"),
                                pluginIds = emptyList(),
                            ),
                            matchesCurrentNavRoute = { currentNavRoute ->
                                if (currentNavRoute is StatDetailNavRoute) {
                                    currentNavRoute.statKeys == listOf("SuppressAnnotationUsages")
                                } else {
                                    false
                                }
                            }
                        )
                    )
                ),
                NavPageGroup(
                    "Insights", setOf(
                        LeafModulesNavRoute.navPage,
                        UnusedModulesReportPage.navPage,
                    ).toNavItems()
                ),
                NavPageGroup(
                    "Explore", setOf(
                        InvertedDependenciesReportPage.navPage,
                        DependencyInjectionReportPage.navPage,
                        DependencyDiffReportPage.navPage,
                        ModuleDependencyGraphReportPage.navPage,
                    ).toNavItems()
                ),
            )
    }
}