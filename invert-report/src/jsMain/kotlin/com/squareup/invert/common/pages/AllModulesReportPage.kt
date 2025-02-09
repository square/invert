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
import com.squareup.invert.models.OwnerName
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

data class ModuleWithOwner(
  val modulePath: ModulePath,
  val ownerName: OwnerName
)

@Composable
fun ModulesComposable(
  modulesNavRoute: AllModulesNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val allModulesOrig by reportDataRepo.allModules.collectAsState(null)
  val moduleToOwnerMap by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val query = modulesNavRoute.query

  if (
    listOf(
      allModulesOrig,
      moduleToOwnerMap,
    ).any { it == null }
  ) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val modulesWithOwner = moduleToOwnerMap!!.entries.map { (modulePath, ownerName) ->
    ModuleWithOwner(
      modulePath = modulePath,
      ownerName = ownerName
    )
  }

  val allModulesWithOwnerMatchingQuery = if (query != null && query != ":" && query.isNotEmpty()) {
    modulesWithOwner.filter { it.modulePath.contains(query) }
  } else {
    modulesWithOwner
  }

  val tabs = mutableListOf<BootstrapTabData>()
  tabs.add(BootstrapTabData(" Name") {
    BootstrapSearchBox(
      query = query ?: "",
      placeholderText = "Module Query...",
    ) {
      navRouteRepo.replaceNavRoute(modulesNavRoute.copy(query = it))
    }
    ModulesByNameComposable(allModulesWithOwnerMatchingQuery) {
      navRouteRepo.pushNavRoute(ModuleDetailNavRoute(it))
    }
  })
  BootstrapTabPane(
    tabs
  )
}

@Composable
fun ModulesByNameComposable(allModules: List<ModuleWithOwner>, moduleClicked: (ModulePath) -> Unit) {
  ModuleListComposable(allModules) { cellValues ->
    moduleClicked(cellValues[0])
  }
}

@Composable
fun ModuleListComposable(
  modulesWithOwner: List<ModuleWithOwner>,
  limit: Int = MAX_RESULTS,
  onRowClicked: (List<String>) -> Unit
) {
  val headers = listOf("Module", "Owner")
  BootstrapTable(
    headers = headers,
    rows = modulesWithOwner.map { listOf(it.modulePath, it.ownerName) },
    types = headers.map { String::class },
    maxResultsLimitConstant = limit,
    onItemClickCallback = onRowClicked,
  )
}

