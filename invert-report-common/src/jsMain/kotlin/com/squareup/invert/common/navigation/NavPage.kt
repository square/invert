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
    val AllModules = NavPage(
      pageId = "modules",
      displayName = "Modules",
      navIconSlug = "folder",
      navRouteParser = {
        AllModulesNavRoute.parser(it)
      }
    )

    val UnusedModules = NavPage(
      pageId = "unused_modules",
      displayName = "Unused Modules",
      navIconSlug = "trash",
      navRouteParser = {
        UnusedModulesNavRoute.parser(it)
      }
    )

    val LeafModules = NavPage(
      pageId = "leaf_modules",
      displayName = "Leaf Modules",
      navIconSlug = "tree",
      navRouteParser = {
        LeafModulesNavRoute.parser(it)
      }
    )

    val ROOT_NAV_ITEMS = listOf(
      HomeNavRoute.Home,
      NavPage.AllModules,
      OwnersNavRoute.Owners,
      PluginsNavRoute.Plugins,
      ModuleConsumptionNavRoute.ModuleConsumption,
      AnnotationProcessorsNavRoute.AnnotationProcessors,
      ArtifactsNavRoute.Artifacts,
      ConfigurationsNavRoute.Configurations,
      AllStatsNavRoute.AllStats,
      DependencyDiffNavRoute.DependencyDiff,
      LeafModulesNavRoute.LeafModules,
      UnusedModulesNavRoute.UnusedModules,
      ModuleDependencyGraphNavRoute.ModuleDependencyGraph,
    )
  }
}