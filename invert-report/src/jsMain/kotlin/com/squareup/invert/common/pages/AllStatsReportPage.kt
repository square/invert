package com.squareup.invert.common.pages


import PagingConstants
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
import com.squareup.invert.common.pages.AllStatsReportPage.navPage
import com.squareup.invert.common.utils.FormattingUtils.formatDecimalSeparator
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.StatTotalAndMetadata
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
import ui.NavRouteLink
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
  val statTotalsOrig by reportDataRepo.statTotals.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  if (moduleToOwnerMapFlowValue == null) {
    BootstrapLoadingSpinner()
    return
  }

  if (statTotalsOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading...")
    return
  }

  val statTotals = statTotalsOrig!!

  val statInfos = statTotals.statTotals.values

  StatDataType.entries.forEach { statDataType: StatDataType ->
    val statsOfType = statTotals.statTotals.values.filter { it.metadata.dataType == statDataType }
    if (statsOfType.isNotEmpty()) {
      H1 { Text("${statDataType.displayName} Stats") }
      StatTiles(statsOfType, navRouteRepo::updateNavRoute)
    }
  }


  val statTotalsMap: Map<StatKey, StatTotalAndMetadata>? = statTotalsOrig?.statTotals

  BootstrapTable(
    headers = listOf("Key", "Description", "Type", "Category", "Count"),
    maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
    rows = statInfos
      .map { it.metadata }
      .filter { it.dataType != StatDataType.STRING }
      .map { statMetadata: StatMetadata ->
        mutableListOf<String>(
          statMetadata.key,
          statMetadata.description,
          statMetadata.dataType.name,
          statMetadata.category
        ).apply {
          statTotalsMap?.get(statMetadata.key)?.let { statTotalAndMetadata: StatTotalAndMetadata ->
            add(statTotalAndMetadata.total.toString())
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

  BootstrapButton(
    "View All, Grouped By Module",
    BootstrapButtonType.SECONDARY,
    onClick = {
      navRouteRepo.updateNavRoute(
        StatDetailNavRoute(
          pluginIds = listOf(),
          statKeys = statInfos.map { it.metadata.key }
        )
      )
    }
  )
}

@Composable
fun StatTiles(codeReferenceStatTotals: List<StatTotalAndMetadata>, onClick: (NavRoute) -> Unit) {
  BootstrapRow {
    codeReferenceStatTotals.sortedBy { it.metadata.description }.forEach { statTotalAndMetadata ->
      val statMetadata = statTotalAndMetadata.metadata
      BootstrapColumn(4) {
        BootstrapJumbotron(
          centered = true,
          paddingNum = 2,
          headerContent = {
            Text(statTotalAndMetadata.total.formatDecimalSeparator())
          }
        ) {
          NavRouteLink(
            if (statMetadata.dataType == StatDataType.CODE_REFERENCES) {
              CodeReferencesNavRoute(
                statKey = statMetadata.key,
              )
            } else {
              StatDetailNavRoute(
                pluginIds = listOf(),
                statKeys = listOf(statMetadata.key)
              )
            },
            onClick,
          ) {
            Text(statTotalAndMetadata.metadata.description)
          }
        }
      }
    }
  }
}