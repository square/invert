package com.squareup.invert.common.pages


import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.AllStatsReportPage.navPage
import com.squareup.invert.common.utils.FormattingUtils.formatDecimalSeparator
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapButton
import ui.BootstrapButtonType
import ui.BootstrapColumn
import ui.BootstrapJumbotron
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapRow
import ui.BootstrapTable
import kotlin.reflect.KClass


data class AllStatsNavRoute(
  val statType: StatDataType? = null
) : BaseNavRoute(
  navPage
) {
  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      statType?.let {
        params[STAT_TYPE_PARAM] = statType.name
      }
    }

  companion object {

    private const val STAT_TYPE_PARAM = "types"

    fun parser(params: Map<String, String?>): AllStatsNavRoute {
      val statTypeParam = params[STAT_TYPE_PARAM]

      val statType = StatDataType.fromString(statTypeParam)
      return AllStatsNavRoute(
        statType = statType,
      )
    }
  }
}

object AllStatsReportPage : InvertReportPage<AllStatsNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "stats",
    displayName = "All Stats",
    navIconSlug = "pie-chart",
    navRouteParser = { AllStatsNavRoute.parser(it) }
  )
  override val navRouteKClass: KClass<AllStatsNavRoute> = AllStatsNavRoute::class

  override val composableContent: @Composable (AllStatsNavRoute) -> Unit = { navRoute ->
    AllStatsComposable(navRoute)
  }

}

@Composable
fun AllStatsComposable(
  statsNavRoute: AllStatsNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val statsDataOrig by reportDataRepo.statsData.collectAsState(null)
  val statTotalsOrig by reportDataRepo.statTotals.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  H1({classes("text-center")}) { Text("Stat Totals") }

  if (moduleToOwnerMapFlowValue == null) {
    BootstrapLoadingSpinner()
    return
  }

  if (statsDataOrig == null || statTotalsOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading...")
    return
  }

  val statTotals = statTotalsOrig!!
  val statsData = statsDataOrig!!

  val statInfos = statsData.statInfos.values

  StatDataType.entries.forEach { statDataType: StatDataType ->
    val statsOfType = statTotals.statTotals.filterKeys { it.dataType == statDataType }
    if (statsOfType.isNotEmpty()) {
      H1 { Text("${statDataType.displayName} Stat Counts") }
      StatTiles(statsOfType) { statKey ->
        navRouteRepo.updateNavRoute(
          if (statDataType == StatDataType.CODE_REFERENCES) {
            CodeReferencesNavRoute(
              statKey = statKey,
            )
          } else {
            StatDetailNavRoute(
              pluginIds = listOf(),
              statKeys = listOf(statKey)
            )
          }
        )
      }
    }
  }

  BootstrapButton("View All",
    BootstrapButtonType.PRIMARY,
    onClick = {
      navRouteRepo.updateNavRoute(
        StatDetailNavRoute(
          pluginIds = listOf(),
          statKeys = statInfos.map { it.key }
        )
      )
    }
  )

  val stats = statsData.statInfos.values

  val statTotalsMap = statTotalsOrig?.statTotals

  BootstrapTable(
    headers = listOf("Key", "Description", "Type", "Category", "Count"),
    maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
    rows = stats
      .filter { it.dataType != StatDataType.STRING && it.dataType != StatDataType.DI_PROVIDES_AND_INJECTS }
      .map { statMetadata ->
        mutableListOf<String>(
          statMetadata.key,
          statMetadata.description,
          statMetadata.dataType.name,
          statMetadata.category
        ).apply {
          statTotalsMap?.get(statMetadata)?.let { count ->
            add(count.toString())
          }
        }
      },
    onItemClickCallback = { cellValues ->
      val statKey = cellValues[0]
      val statDataType = StatDataType.fromString(cellValues[2])
      navRouteRepo.updateNavRoute(
        if (statDataType == StatDataType.CODE_REFERENCES) {
          CodeReferencesNavRoute(
            statKey = statKey,
          )
        } else {
          StatDetailNavRoute(
            pluginIds = listOf(),
            statKeys = listOf(statKey)
          )
        }
      )

    }
  )
}

@Composable
fun StatTiles(statTotals: Map<StatMetadata, Int>, onClick: (StatKey) -> Unit) {
  BootstrapRow {
    statTotals.entries.forEach { statTotal ->
      BootstrapColumn(4) {
        BootstrapJumbotron(
          centered = true,
          paddingNum = 2,
          headerContent = {
            Text(statTotal.value.formatDecimalSeparator())
          }
        ) {
          A(href = "#", {
            onClick {
              onClick(statTotal.key.key)
            }
          }) {
            Text(statTotal.key.description)
          }
        }
      }
    }
  }
}