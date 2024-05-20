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
        private fun NavPage.toNavItem(): NavItem {
            return NavItem(
                itemTitle = this.displayName,
                navPage = this,
            )
        }

        private fun gitHubContentNavItem(title: String, destinationNavRoute: NavRoute): NavItem {
            return NavItem(
                navPage = GitHubMarkdownReportPage.navPage,
                itemTitle = title,
                navIconSlug = "github",
                destinationNavRoute = destinationNavRoute,
                matchesCurrentNavRoute = { it == destinationNavRoute }
            )
        }

        val ROOT_NAV_ITEMS
            get() = listOf(
                NavPageGroup(
                    "General", setOf(
                        HomeReportPage.navPage.toNavItem(),
                        OwnersReportPage.navPage.toNavItem().copy(
                            matchesCurrentNavRoute = {
                                it is OwnerDetailNavRoute || it is OwnersNavRoute
                            }
                        ),
                        AllStatsNavRoute().navPage.toNavItem(),
                    )
                ),
                NavPageGroup(
                    "Gradle", setOf(
                        AllModulesReportPage.navPage.toNavItem().copy(
                            matchesCurrentNavRoute = {
                                it is ModuleDetailNavRoute || it is AllModulesNavRoute
                            }
                        ),
                        GradlePluginsReportPage.navPage.toNavItem().copy(
                            matchesCurrentNavRoute = {
                                it is GradlePluginsNavRoute || it is PluginDetailNavRoute
                            },
                            destinationNavRoute = GradlePluginsNavRoute(null)
                        ),
                        ArtifactsReportPage.navPage.toNavItem().copy(
                            matchesCurrentNavRoute = {
                                it is ArtifactsNavRoute || it is ArtifactDetailNavRoute
                            }
                        ),
                        GradleRepositoriesReportPage.navPage.toNavItem(),
                        ConfigurationsNavRoute.navPage.toNavItem().copy(
                            matchesCurrentNavRoute = {
                                it is ConfigurationsNavRoute || it is ConfigurationDetailNavRoute
                            }
                        ),
                        AnnotationProcessorsReportPage.navPage.toNavItem(),
                        KotlinCompilerPluginsReportPage.navPage.toNavItem(),
                    )
                ),
                NavPageGroup(
                    "Insights", setOf(
                        LeafModulesNavRoute.navPage.toNavItem(),
                        UnusedModulesReportPage.navPage.toNavItem(),
                        SuppressAnnotationReportPage.navPage.toNavItem(),
                    )
                ),
                NavPageGroup(
                    "Explore", setOf(
                        InvertedDependenciesReportPage.navPage.toNavItem(),
                        DependencyInjectionReportPage.navPage.toNavItem(),
                        DependencyDiffReportPage.navPage.toNavItem(),
                        ModuleDependencyGraphReportPage.navPage.toNavItem(),
                    )
                ),
                NavPageGroup(
                    "GitHub", setOf(
                        gitHubContentNavItem(
                            title = "README.md",
                            destinationNavRoute = GithubReadMeNavRoute(
                                "square/okhttp",
                                "README.md"
                            )
                        ),
                        gitHubContentNavItem(
                            title = "OkHttp.kt",
                            destinationNavRoute = GithubReadMeNavRoute(
                                "square/okhttp",
                                "okhttp/src/main/kotlin/okhttp3/OkHttp.kt"
                            )
                        ),
                    )
                )
            )
    }
}