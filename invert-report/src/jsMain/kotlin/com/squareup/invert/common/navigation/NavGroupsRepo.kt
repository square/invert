package com.squareup.invert.common.navigation

import com.squareup.invert.common.navigation.NavPage.NavItem
import com.squareup.invert.common.pages.AllModulesNavRoute
import com.squareup.invert.common.pages.AllModulesReportPage
import com.squareup.invert.common.pages.AllStatsNavRoute
import com.squareup.invert.common.pages.AnnotationProcessorsReportPage
import com.squareup.invert.common.pages.ArtifactDetailNavRoute
import com.squareup.invert.common.pages.ArtifactsNavRoute
import com.squareup.invert.common.pages.ArtifactsReportPage
import com.squareup.invert.common.pages.OwnerBreakdownReportPage
import com.squareup.invert.common.pages.CodeReferencesReportPage
import com.squareup.invert.common.pages.ConfigurationDetailNavRoute
import com.squareup.invert.common.pages.ConfigurationsNavRoute
import com.squareup.invert.common.pages.DependencyDiffReportPage
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
import com.squareup.invert.common.pages.UnusedModulesReportPage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Source of truth for the current [NavPageGroup]s and [NavPage.NavItem]s
 */
class NavGroupsRepo(additionalGroups: Set<NavPageGroup>) {

  private val _navGroups = MutableStateFlow(DefaultNavItems.ROOT_NAV_ITEMS.toSet() + additionalGroups)

  val navGroups: Flow<Set<NavPageGroup>> = _navGroups

  fun add(item: NavPageGroup) {
    this._navGroups.value = this._navGroups.value.plus(item)
  }

  fun update(newValue: Set<NavPageGroup>) {
    this._navGroups.value = newValue
  }

}

object DefaultNavItems {

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
          AllModulesReportPage.navPage.toNavItem().copy(
            matchesCurrentNavRoute = {
              it is ModuleDetailNavRoute || it is AllModulesNavRoute
            }
          ),
        )
      ),
      NavPageGroup(
        "Collected Stats", setOf(
          AllStatsNavRoute().navPage.toNavItem(),
          OwnerBreakdownReportPage.navPage.toNavItem()
        )
      ),
      NavPageGroup(
        "Insights", setOf(
          LeafModulesNavRoute.navPage.toNavItem(),
          UnusedModulesReportPage.navPage.toNavItem(),
        )
      ),
      NavPageGroup(
        "Explore", setOf(
          InvertedDependenciesReportPage.navPage.toNavItem(),
          DependencyDiffReportPage.navPage.toNavItem(),
          ModuleDependencyGraphReportPage.navPage.toNavItem(),
        )
      )
    )
}

fun NavPage.toNavItem(): NavItem {
  return NavItem(
    itemTitle = this.displayName,
    navPage = this,
  )
}