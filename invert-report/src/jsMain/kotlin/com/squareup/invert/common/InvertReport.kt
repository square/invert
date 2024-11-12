package com.squareup.invert.common

import com.squareup.invert.common.navigation.NavGroupsRepo
import com.squareup.invert.common.navigation.NavPageGroup
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteManager
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.toNavItem
import com.squareup.invert.common.pages.AllModulesReportPage
import com.squareup.invert.common.pages.AllStatsReportPage
import com.squareup.invert.common.pages.AnnotationProcessorsReportPage
import com.squareup.invert.common.pages.ArtifactDetailReportPage
import com.squareup.invert.common.pages.ArtifactsReportPage
import com.squareup.invert.common.pages.CodeReferencesReportPage
import com.squareup.invert.common.pages.ConfigurationDetailReportPage
import com.squareup.invert.common.pages.ConfigurationsReportPage
import com.squareup.invert.common.pages.DependencyDiffReportPage
import com.squareup.invert.common.pages.GitHubMarkdownReportPage
import com.squareup.invert.common.pages.GradlePluginsReportPage
import com.squareup.invert.common.pages.GradleRepositoriesReportPage
import com.squareup.invert.common.pages.HomeReportPage
import com.squareup.invert.common.pages.InvertedDependenciesReportPage
import com.squareup.invert.common.pages.KotlinCompilerPluginsReportPage
import com.squareup.invert.common.pages.LeafModulesReportPage
import com.squareup.invert.common.pages.ModuleDependencyGraphReportPage
import com.squareup.invert.common.pages.ModuleDetailReportPage
import com.squareup.invert.common.pages.OwnerDetailReportPage
import com.squareup.invert.common.pages.OwnersReportPage
import com.squareup.invert.common.pages.PluginDetailReportPage
import com.squareup.invert.common.pages.StatDetailReportPage
import com.squareup.invert.common.pages.SuppressAnnotationReportPage
import com.squareup.invert.common.pages.UnusedModulesReportPage
import invertComposeMain
import kotlinx.browser.window
import kotlinx.coroutines.Dispatchers
import navigation.RemoteJsLoadingProgress

class InvertReport(
  customReportPages: List<InvertReportPage<out NavRoute>> = emptyList(),
  customNavGroups: Set<NavPageGroup> = emptySet(),
) {
  private val routeManager = NavRouteManager()

  init {
    val allReportPages = DEFAULT_REPORT_PAGES + customReportPages
    allReportPages.forEach { reportPage ->
      routeManager.registerParser(reportPage.navPage)
    }
    allReportPages.forEach { reportPage ->
      routeManager.registerRoute(
        clazz = reportPage.navRouteKClass,
        content = {
          reportPage.composableContentWithRouteCast(it)
        }
      )
    }
  }

  private val initialRoute: NavRoute = routeManager.parseUrlToRoute(window.location.toString())

  val navRouteRepo = NavRouteRepo(initialRoute)

  val collectedDataRepo = CollectedDataRepo(
    coroutineDispatcher = Dispatchers.Default,
    loadFileData = { jsFileKey, reportRepoData ->
      RemoteJsLoadingProgress.loadJavaScriptFile(jsFileKey) { json ->
        RemoteJsLoadingProgress.handleLoadedJsFile(reportRepoData, jsFileKey, json)
      }
    },
  )

  val navGroupsRepo = NavGroupsRepo(
    setOf(NavPageGroup(
      groupTitle = "Custom Pages",
      navItems = customReportPages.filter { it.showInNav }
        .map { it.navPage.toNavItem() }
        .toSet(),
    )) + customNavGroups
  )

  val reportDataRepo = ReportDataRepo(
    navRoute = navRouteRepo.navRoute,
    collectedDataRepo = collectedDataRepo,
  )

  init {
    invertComposeMain(
      initialRoute = initialRoute,
      routeManager = routeManager,
      navRouteRepo = navRouteRepo,
      reportDataRepo = reportDataRepo,
      navGroupsRepo = navGroupsRepo,
    )
  }

  init {
    // REALLY HACKY Static DI Graph
    DependencyGraph.initialize(
      collectedDataRepo = collectedDataRepo,
      navRouteRepo = navRouteRepo,
      reportDataRepo = reportDataRepo,
    )
  }

  companion object {
    private val DEFAULT_REPORT_PAGES = listOf<InvertReportPage<*>>(
      AllModulesReportPage,
      AllStatsReportPage,
      ArtifactDetailReportPage,
      ArtifactsReportPage,
      AnnotationProcessorsReportPage,
      CodeReferencesReportPage,
      ConfigurationDetailReportPage,
      ConfigurationsReportPage,
      DependencyDiffReportPage,
      ModuleDependencyGraphReportPage,
      HomeReportPage,
      KotlinCompilerPluginsReportPage,
      LeafModulesReportPage,
      ModuleDetailReportPage,
      InvertedDependenciesReportPage,
      OwnerDetailReportPage,
      OwnersReportPage,
      PluginDetailReportPage,
      GradlePluginsReportPage,
      StatDetailReportPage,
      UnusedModulesReportPage,
      SuppressAnnotationReportPage,
      GitHubMarkdownReportPage,
      GradleRepositoriesReportPage,
    )
  }
}
