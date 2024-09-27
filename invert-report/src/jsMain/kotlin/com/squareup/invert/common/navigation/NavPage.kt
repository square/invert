package com.squareup.invert.common.navigation

import com.squareup.invert.common.pages.AllModulesNavRoute
import com.squareup.invert.common.pages.AllModulesReportPage
import com.squareup.invert.common.pages.AllStatsNavRoute
import com.squareup.invert.common.pages.AnnotationProcessorsReportPage
import com.squareup.invert.common.pages.ArtifactDetailNavRoute
import com.squareup.invert.common.pages.ArtifactsNavRoute
import com.squareup.invert.common.pages.ArtifactsReportPage
import com.squareup.invert.common.pages.CodeReferencesReportPage
import com.squareup.invert.common.pages.ConfigurationDetailNavRoute
import com.squareup.invert.common.pages.ConfigurationsNavRoute
import com.squareup.invert.common.pages.DependencyDiffReportPage
import com.squareup.invert.common.pages.DependencyInjectionReportPage
import com.squareup.invert.common.pages.GitHubMarkdownReportPage
import com.squareup.invert.common.pages.GithubReadMeNavRoute
import com.squareup.invert.common.pages.GradlePluginsNavRoute
import com.squareup.invert.common.pages.GradlePluginsReportPage
import com.squareup.invert.common.pages.GradleRepositoriesReportPage
import com.squareup.invert.common.pages.HomeReportPage
import com.squareup.invert.common.pages.InvertedDependenciesReportPage
import com.squareup.invert.common.pages.KotlinCompilerPluginsReportPage
import com.squareup.invert.common.pages.LeafModulesNavRoute
import com.squareup.invert.common.pages.ModuleDependencyGraphReportPage
import com.squareup.invert.common.pages.ModuleDetailNavRoute
import com.squareup.invert.common.pages.OwnerDetailNavRoute
import com.squareup.invert.common.pages.OwnersNavRoute
import com.squareup.invert.common.pages.OwnersReportPage
import com.squareup.invert.common.pages.PluginDetailNavRoute
import com.squareup.invert.common.pages.SuppressAnnotationReportPage
import com.squareup.invert.common.pages.UnusedModulesReportPage

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
}