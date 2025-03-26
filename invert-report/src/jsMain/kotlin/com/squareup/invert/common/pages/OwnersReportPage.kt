package com.squareup.invert.common.pages


import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.charts.ChartJsChartComposable
import com.squareup.invert.common.charts.ChartsJs
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapLoadingSpinner
import ui.BootstrapTable
import kotlin.reflect.KClass

object OwnersNavRoute : BaseNavRoute(OwnersReportPage.navPage)

object OwnersReportPage : InvertReportPage<OwnersNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "owners",
    displayName = "Owners",
    navIconSlug = "people",
    navRouteParser = { OwnersNavRoute }
  )
  override val navRouteKClass: KClass<OwnersNavRoute> = OwnersNavRoute::class

  override val composableContent: @Composable (OwnersNavRoute) -> Unit = { navRoute ->
    OwnersComposable(navRoute)
  }
}


@Composable
fun OwnersComposable(
  navRoute: OwnersNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
  H1 {
    Text("Owners")
  }

  val ownersCollected: Map<OwnerName, List<ModulePath>>? by reportDataRepo.ownerNameToModulesMap.collectAsState(null)

  if (ownersCollected == null) {
    BootstrapLoadingSpinner()
    return
  }
  val owners = ownersCollected!!

  ChartJsChartComposable(
    data = ChartsJs.ChartJsData(
      labels = owners.keys.map { it },
      datasets = listOf(
        ChartsJs.ChartJsDataset(
          label = "Module Count",
          data = owners.values.map { it.size }
        )
      )
    ),
    onClick = { label, value ->
      navRouteRepo.pushNavRoute(
        OwnerDetailNavRoute(
          owner = label,
        )
      )
    }
  )


  val ownerToModuleCount: List<List<String>> = owners.map {
    listOf(it.key, it.value.size.toString())
  }

  BootstrapTable(
    headers = listOf("Owner", "Module Count"),
    rows = ownerToModuleCount,
    types = listOf(String::class, Int::class),
    maxResultsLimitConstant = PagingConstants.MAX_RESULTS
  ) { cellValues ->
    val owner = cellValues[0]
    navRouteRepo.pushNavRoute(OwnerDetailNavRoute(owner))
  }
}
