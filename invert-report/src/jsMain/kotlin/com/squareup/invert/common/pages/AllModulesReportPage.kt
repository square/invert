package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.AllModulesReportPage.navPage
import com.squareup.invert.models.ModulePath
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapAccordion
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapSearchBox
import ui.BootstrapTabData
import ui.BootstrapTabPane
import ui.BootstrapTable
import kotlin.reflect.KClass


data class AllModulesNavRoute(
  val query: String? = null,
) : BaseNavRoute(navPage) {
  override fun toSearchParams() = toParamsWithOnlyPageId(this).also { map ->
    query?.let {
      map[QUERY_PARAM] = it
    }
  }

  companion object {

    private const val QUERY_PARAM = "query"
    fun parser(params: Map<String, String?>): NavRoute {
      val queryParam = params[QUERY_PARAM]
      return AllModulesNavRoute(queryParam)
    }
  }
}


object AllModulesReportPage : InvertReportPage<AllModulesNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "modules",
    displayName = "Modules",
    navIconSlug = "folder",
    navRouteParser = {
      AllModulesNavRoute.parser(it)
    }
  )
  override val navRouteKClass: KClass<AllModulesNavRoute> = AllModulesNavRoute::class

  override val composableContent: @Composable (AllModulesNavRoute) -> Unit = { navRoute ->
    ModulesComposable(navRoute)
  }
}


@Composable
fun ModulesComposable(
  modulesNavRoute: AllModulesNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val allModulesCollected by reportDataRepo.allModules.collectAsState(null)

  val query = modulesNavRoute.query


  if (allModulesCollected == null) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val allModules = allModulesCollected!!

  val allModulesMatchingQuery = if (query != null && query != ":" && query.isNotEmpty()) {
    allModules.filter { it.contains(query) }
  } else {
    allModules
  }


  val tabs = mutableListOf<BootstrapTabData>()
  tabs.add(BootstrapTabData("By Name") {
    BootstrapSearchBox(
      query = query ?: "",
      placeholderText = "Module Query...",
    ) {
      navRouteRepo.updateNavRoute(modulesNavRoute.copy(query = it))
    }
    ModulesByNameComposable(allModulesMatchingQuery) {
      navRouteRepo.updateNavRoute(ModuleDetailNavRoute(it))
    }
  })
//  tabs.add(BootstrapTabData("By Plugin") {
//    ModulesByPluginComposable(
//      reportDataRepo = reportDataRepo,
//      navRouteRepo = navRouteRepo,
//    )
//  })
  BootstrapTabPane(
    tabs
  )
}


@Composable
fun ModulesByNameComposable(allModules: List<String>?, moduleClicked: (ModulePath) -> Unit) {
  ModuleListComposable(allModules) { cellValues ->
    moduleClicked(cellValues[0])
  }
}

@Composable
fun ModuleListComposable(allModules: List<String>?, limit: Int = MAX_RESULTS, onRowClicked: (List<String>) -> Unit) {
  if (allModules.isNullOrEmpty()) {
    H1 { Text("Loading...") }
  } else {
    BootstrapTable(
      headers = listOf("Module"),
      rows = allModules.map { listOf(it) },
      types = listOf(String::class),
      maxResultsLimitConstant = limit,
      onItemClickCallback = onRowClicked,
    )
  }
}


@Composable
fun ModulesByPluginComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo) {
  val pluginIdToAllModulesMap by reportDataRepo.pluginIdToAllModulesMap.collectAsState(null)
  pluginIdToAllModulesMap?.keys?.sorted()?.forEach { pluginId ->
    val modules = pluginIdToAllModulesMap!![pluginId]
    val headerText = pluginId + " (${modules?.size})"
    BootstrapAccordion({ Text(headerText) }) {
      ModuleListComposable(modules) { cellValues ->
        navRouteRepo.updateNavRoute(ModuleDetailNavRoute(cellValues[0]))
      }
    }
  }

}


