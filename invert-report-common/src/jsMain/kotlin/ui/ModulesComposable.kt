package ui


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.AllModulesNavRoute
import com.squareup.invert.common.navigation.routes.ModuleDetailNavRoute
import com.squareup.invert.models.GradlePath
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text

@Composable
fun ModulesComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo, modulesNavRoute: AllModulesNavRoute) {
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
fun ModulesByNameComposable(allModules: List<String>?, moduleClicked: (GradlePath) -> Unit) {
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
      onItemClick = onRowClicked,
    )
  }
}


@Composable
fun ModulesByPluginComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo) {
  val pluginIdToAllModulesMap by reportDataRepo.pluginIdToAllModulesMap.collectAsState(null)
  pluginIdToAllModulesMap?.keys?.sorted()?.forEach { pluginId ->
    val modules = pluginIdToAllModulesMap!![pluginId]
    val headerText = pluginId + " (${modules?.size})"
    BootstrapAccordion({ Text(headerText) }, {
      ModuleListComposable(modules) { cellValues ->
        navRouteRepo.updateNavRoute(ModuleDetailNavRoute(cellValues[0]))
      }
    })
  }

}

