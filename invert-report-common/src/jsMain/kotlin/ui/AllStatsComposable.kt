package ui

import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.AllStatsNavRoute
import com.squareup.invert.common.navigation.routes.StatDetailNavRoute
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text


@Composable
fun AllStatsComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo, statsNavRoute: AllStatsNavRoute) {
  val statsDataOrig by reportDataRepo.statsData.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  H1 { Text("Stats") }

  if (moduleToOwnerMapFlowValue == null) {
    BootstrapLoadingSpinner()
    return
  }

  if (statsDataOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading...")
    return
  }
  val statsData = statsDataOrig!!

  val statInfos = statsData.statInfos.values

  val rows = statInfos.map { statInfo -> listOf(statInfo.name, statInfo.statType.name, statInfo.description) }

  BootstrapTable(
    headers = listOf("Name", "Type", "Description"),
    rows = rows,
    types = statInfos.map { String::class },
    maxResultsLimitConstant = MAX_RESULTS
  ) { cellValues ->
    navRouteRepo.updateNavRoute(
      StatDetailNavRoute(
        pluginIds = listOf(),
        statKeys = listOf(cellValues[0])
      )
    )
  }

  BootstrapButton("View All",
    BootstrapButtonType.PRIMARY,
    onClick = {
      navRouteRepo.updateNavRoute(
        StatDetailNavRoute(
          pluginIds = listOf(),
          statKeys = statInfos.map { it.name }
        )
      )
    })

}