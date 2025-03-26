package com.squareup.invert.common.pages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.charts.ChartsJs
import com.squareup.invert.common.model.StatComparisonResult
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.TechDebtNavRoute.Companion.PARAM_EMBED
import com.squareup.invert.common.utils.EmbedMode
import com.squareup.invert.common.utils.HistoricalComparison
import com.squareup.invert.common.utils.MathUtils.formatted
import com.squareup.invert.common.utils.MathUtils.percentage
import com.squareup.invert.models.OrgName
import com.squareup.invert.models.OwnerInfo
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import com.squareup.invert.models.js.HistoricalData
import com.squareup.invert.models.js.StatTotalAndMetadata
import kotlinx.browser.window
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H2
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.H6
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import ui.BootstrapColumn
import ui.BootstrapJumbotron
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import ui.MarkdownText
import ui.NavRouteLink
import kotlin.reflect.KClass

data class TechDebtNavRoute(
  val remainingKey: String?,
  val completedKey: String?,
  val embed: Boolean? = null,
) : BaseNavRoute(TechDebtNavPage) {
  override fun toSearchParams(): Map<String, String> {
    val params = super.toSearchParams().toMutableMap()
    remainingKey?.let {
      params[PARAM_REMAINING_STAT_KEY] = it
    }
    completedKey?.let {
      params[PARAM_COMPLETED_STAT_KEY] = it
    }
    if (embed == true) {
      params[PARAM_EMBED] = embed.toString()
    }
    return params
  }

  companion object {
    const val PARAM_COMPLETED_STAT_KEY = "completed"
    const val PARAM_REMAINING_STAT_KEY = "remaining"
    const val PARAM_EMBED = "embed"
  }
}

val TechDebtNavPage = NavPage(
  pageId = "tech_debt",
  displayName = "Tech Debt",
  navIconSlug = "piggy-bank",
  navRouteParser = { params: Map<String, String?> ->
    TechDebtNavRoute(
      completedKey = params[TechDebtNavRoute.PARAM_COMPLETED_STAT_KEY]?.trim(),
      remainingKey = params[TechDebtNavRoute.PARAM_REMAINING_STAT_KEY]?.trim(),
      embed = params[PARAM_EMBED] == "true",
    )
  }
)

object TechDebtReportPage : InvertReportPage<TechDebtNavRoute> {
  override val navPage: NavPage = TechDebtNavPage

  override val navRouteKClass: KClass<TechDebtNavRoute> =
    TechDebtNavRoute::class

  override val composableContent: @Composable (TechDebtNavRoute) -> Unit = { navRoute ->
    TechDebtComposable(navRoute)
  }
}

@Composable
fun TechDebtComposable(
  navRoute: TechDebtNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val isEmbed = (navRoute.embed == true)
  if (isEmbed) {
    EmbedMode.enableEmbedMode()
  } else {
    EmbedMode.disableEmbedMode()
  }

  val historicalDataOrig by reportDataRepo.historicalData.collectAsState(null)
  val ownerNamesOrig by reportDataRepo.allOwnerNames.collectAsState(null)
  val statTotalsOrig by reportDataRepo.statTotals.collectAsState(null)
  val reportMetadataOrig by reportDataRepo.reportMetadata.collectAsState(null)

  if (
    listOf(
      historicalDataOrig,
      ownerNamesOrig,
      statTotalsOrig,
      reportMetadataOrig,
    ).any { it == null }
  ) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  val reportMetadata = reportMetadataOrig!!

  val ownerToOrg: (OwnerName) -> OrgName = { ownerName ->
    reportMetadata.owners.ownerToDetails[ownerName]?.orgName ?: OwnerInfo.UNOWNED
  }

  val historicalData = historicalDataOrig!!
  val ownerData = ownerNamesOrig!!
  val statTotalsReportJsModel = statTotalsOrig!!

  val statToMetadata: Map<StatKey, StatMetadata> = statTotalsReportJsModel.statTotals.mapValues {
    it.value.metadata
  }

  val codeReferenceStatTypes = statToMetadata.values
    .filter { it.dataType == StatDataType.CODE_REFERENCES }
    .sortedBy { it.description }

  val remainingStatKey = navRoute.remainingKey
  val completedStatKey = navRoute.completedKey
  if (remainingStatKey.isNullOrBlank()) {
    val codeReferencesByCategory = codeReferenceStatTypes.groupBy { it.category }
    TechDebtListComposable(
      navRoute = navRoute,
      codeReferencesByCategory = codeReferencesByCategory,
      navRouteUpdated = navRouteRepo::pushNavRoute
    )
    return
  } else {
    val initiative = codeReferenceStatTypes.firstOrNull { it.key == remainingStatKey }
    if (initiative != null) {
      InitiativeDetailComposable(
        updateNavRoute = {
          navRouteRepo.pushNavRoute(it)
        },
        historicalData = historicalData,
        allOwnerNames = ownerData,
        completedStatInfo = statToMetadata[completedStatKey],
        remainingStatInfo = statToMetadata[remainingStatKey] ?: run {
          H1 { Text("Could not find $remainingStatKey") }
          return
        },
        ownerToOrg = ownerToOrg,
      )
      if (!isEmbed) {
        Br()
        NavRouteLink(navRoute.copy(embed = true), navRouteRepo::pushNavRoute) {
          Small { Text("Get Embed Link") }
        }
      }
    } else {
      window.alert("Initiative $remainingStatKey not found.")
    }
  }
}

data class OwnerProgress(
  val ownerName: String,
  val completed: Int,
  val remaining: Int,
  val org: OrgName
) {
  val total = completed + remaining
  val totalFormatted = total.formatted()
  val percentCompleteFormatted = percentage(completed, total)
  val percentComplete = percentCompleteFormatted.toDouble()
  val completedFormatted = completed.formatted()
  val remainingFormatted = remaining.formatted()
  val emoji = when (percentComplete) {
    100.0 -> {
      "✅"
    }

    else -> {
      "⏳"
    }
  }
  val burndownCompleteEmoji = when (remaining) {
    0 -> {
      "✅"
    }

    else -> {
      "⏳"
    }
  }
}

class OrgProgress(
  val orgName: OrgName,
  val ownerProgressList: List<OwnerProgress>
) {
  val completed: Int = ownerProgressList.sumOf { it.completed }
  val remaining: Int = ownerProgressList.sumOf { it.remaining }
  val total = completed + remaining
  val percentCompleteFormatted = percentage(completed, total)
  val percentComplete = percentCompleteFormatted.toDouble()
  val totalFormatted = total.formatted()
  val completedFormatted = completed.formatted()
  val remainingFormatted = remaining.formatted()
  val emoji = when (percentComplete) {
    100.0 -> {
      "✅"
    }

    else -> {
      "⏳"
    }
  }
  val burndownCompleteEmoji = when (remaining) {
    0 -> {
      "✅"
    }

    else -> {
      "⏳"
    }
  }
}

@Composable
fun InitiativeDetailComposable(
  updateNavRoute: (NavRoute) -> Unit,
  historicalData: List<HistoricalData>,
  allOwnerNames: List<String>,
  completedStatInfo: StatMetadata?,
  remainingStatInfo: StatMetadata,
  ownerToOrg: (OwnerName) -> OrgName,
) {
  val comparisons = mutableListOf<StatComparisonResult>()
  val datasets = mutableListOf<ChartsJs.ChartJsDataset>()
  val firstHistoricalData = historicalData.first()
  val currentHistoricalData = historicalData.last()

  val currentNumeratorData: StatTotalAndMetadata? = completedStatInfo?.let { statMetadata ->
    currentHistoricalData.statTotalsAndMetadata.statTotals[statMetadata.key]
  }

  val currentDenominatorData: StatTotalAndMetadata =
    currentHistoricalData.statTotalsAndMetadata.statTotals[remainingStatInfo.key] ?: run {
      H1 { Text("Could not find ${remainingStatInfo.key}") }
      return
    }

  val allOwnerProgress: Map<OwnerName, OwnerProgress> =
    allOwnerNames.associateWith { owner ->
      OwnerProgress(
        ownerName = owner,
        completed = currentNumeratorData?.totalByOwner?.get(owner) ?: 0,
        remaining = currentDenominatorData.totalByOwner[owner] ?: 0,
        org = ownerToOrg(owner)
      )
    }

  completedStatInfo?.key?.also { statKey ->
    compareOldestToNewest(firstHistoricalData, currentHistoricalData, statKey)
      ?.let {
        comparisons.add(it)
      }

    datasets.add(
      ChartsJs.ChartJsDataset(
        label = remainingStatInfo.description,
        data = historicalData.map { historicalDataPoint ->
          historicalDataPoint.statTotalsAndMetadata.statTotals[statKey]?.total ?: 0
        }
      )
    )
  }

  if (completedStatInfo != null && currentNumeratorData != null) {
    ProgressComposable(
      completedStatInfo = completedStatInfo,
      remainingStatInfo = remainingStatInfo,
      currentNumeratorData = currentNumeratorData,
      currentDenominatorData = currentDenominatorData,
      updateNavRoute = updateNavRoute,
      allOwnerProgress = allOwnerProgress.filter { entry: Map.Entry<OwnerName, OwnerProgress> ->
        // Ensuring we only show teams relevant to this initiative
        entry.value.total > 0
      },
    )
  } else {
    BurndownComposable(
      remainingStatInfo = remainingStatInfo,
      currentDenominatorData = currentDenominatorData,
      updateNavRoute = updateNavRoute,
      allOwnerProgress = allOwnerProgress,
    )
  }
}

@Composable
fun BurndownComposable(
  remainingStatInfo: StatMetadata,
  currentDenominatorData: StatTotalAndMetadata,
  updateNavRoute: (NavRoute) -> Unit,
  allOwnerProgress: Map<OwnerName, OwnerProgress>,
) {
  InitiativeHeaderAndJumbotron(remainingStatInfo)
  Hr()
  Br()
  Div({ classes("text-center") }) {
    H2 {
      Text("Overall Remaining: ${currentDenominatorData.total.formatted()}")
    }
    H5 {
      Text("[")
      NavRouteLink(
        OwnerBreakdownNavRoute(
          statKey = remainingStatInfo.key,
          owner = null
        ),
        updateNavRoute,
      ) {
        Text("Remaining Stat")
      }
      Text("]")
    }
  }
  Br()
  BootstrapRow(classes = listOf("fw-bold")) {
    BootstrapColumn(4) {
      Text("Org Progress")
    }
    BootstrapColumn(8) {
      BootstrapRow {
        BootstrapColumn(6) {
          Text("Owner Progress")
        }
        BootstrapColumn(6) {
          Text("Code References")
        }
      }
    }
  }

  val allOrgProgress: List<OrgProgress> = allOwnerProgress.values.groupBy {
    it.org
  }.entries.map { (orgName, ownerProgress) ->
    OrgProgress(orgName = orgName, ownerProgressList = ownerProgress)
  }
  allOrgProgress.sortedBy { it.orgName }.forEach { orgProgress: OrgProgress ->
    BootstrapRow {
      BootstrapColumn(4, classes = listOf("border")) {
        BootstrapRow {
          BootstrapColumn(6) {
            Text("${orgProgress.burndownCompleteEmoji} ${orgProgress.orgName}")
          }
          BootstrapColumn(6) {
            Text(" ${orgProgress.remainingFormatted} Remaining")
          }
        }
      }
      BootstrapColumn(8, classes = listOf("border")) {
        orgProgress.ownerProgressList.sortedBy { it.ownerName }
          .forEach { ownerProgress: OwnerProgress ->
            BootstrapRow {
              BootstrapColumn(6) {
                Text("${ownerProgress.burndownCompleteEmoji} ${ownerProgress.ownerName}")
              }
              BootstrapColumn(6) {
                NavRouteLink(
                  OwnerBreakdownNavRoute(
                    statKey = remainingStatInfo.key,
                    owner = ownerProgress.ownerName
                  ), updateNavRoute
                ) {
                  Text("${ownerProgress.remainingFormatted} Remaining")
                }
              }
            }
          }
      }
    }
  }
  Br()
}

@Composable
fun InitiativeHeaderAndJumbotron(
  remainingStatInfo: StatMetadata,
) {
  H2({ classes("text-center") }) {
    Text("Tech Debt: ${remainingStatInfo.title}")
  }
  if (remainingStatInfo.title != remainingStatInfo.description) {
    BootstrapJumbotron(content = {
      BootstrapRow {
        BootstrapColumn(12) {
          MarkdownText(remainingStatInfo.description)
        }
      }
    }, headerContent = {})
  }
}

@Composable
fun ProgressComposable(
  completedStatInfo: StatMetadata,
  remainingStatInfo: StatMetadata,
  currentNumeratorData: StatTotalAndMetadata,
  currentDenominatorData: StatTotalAndMetadata,
  updateNavRoute: (NavRoute) -> Unit,
  allOwnerProgress: Map<OwnerName, OwnerProgress>,
) {
  val currentNumeratorTotal = currentNumeratorData.total
  val currentDenominatorTotal = currentNumeratorTotal + (currentDenominatorData.total)

  InitiativeHeaderAndJumbotron(remainingStatInfo)
  Hr()
  Br()
  Div({ classes("text-center") }) {
    H2 {
      if (completedStatInfo != null) {
        Text(
          "Overall Progress: ${percentage(currentNumeratorTotal, currentDenominatorTotal)}%"
              + " Complete (${currentNumeratorTotal.formatted()} of ${currentDenominatorTotal.formatted()})"
        )
      } else {
        Text("Overall Remaining: ${currentDenominatorData.total.formatted()}")
      }
    }
    H5 {
      Text("[")
      NavRouteLink(
        OwnerBreakdownNavRoute(
          statKey = remainingStatInfo.key,
          owner = null
        ),
        updateNavRoute,
      ) {
        Text("Remaining Stat")
      }
      Text("]")
      completedStatInfo?.key?.let {
        Space()
        Text("[")
        NavRouteLink(
          OwnerBreakdownNavRoute(
            statKey = completedStatInfo.key,
            owner = null
          ),
          updateNavRoute,
        ) {
          Text("Completed Stat")
        }
        Text("]")
      }
    }
  }
  Br()
  BootstrapRow(classes = listOf("fw-bold")) {
    BootstrapColumn(4) {
      Text("Org Progress")
    }
    BootstrapColumn(8) {
      BootstrapRow {
        BootstrapColumn(3) {
          Text("Owner Progress")
        }
        BootstrapColumn(4) {
          Text("Owner Progress")
        }
        BootstrapColumn(5) {
          Text("Code References")
        }
      }
    }
  }

  val allOrgProgress: List<OrgProgress> = allOwnerProgress.values.groupBy {
    it.org
  }.entries.map { (orgName, ownerProgress) ->
    OrgProgress(orgName = orgName, ownerProgressList = ownerProgress)
  }
  allOrgProgress.sortedBy { it.orgName }.forEach { orgProgress: OrgProgress ->
    BootstrapRow {
      BootstrapColumn(4, classes = listOf("border")) {
        BootstrapRow {
          BootstrapColumn(4) {
            Text(orgProgress.orgName)
          }
          BootstrapColumn(8) {
            Text("${orgProgress.emoji} ${orgProgress.percentCompleteFormatted}% Done (${orgProgress.completedFormatted} of ${orgProgress.totalFormatted})")
          }
        }
      }
      BootstrapColumn(8, classes = listOf("border")) {
        orgProgress.ownerProgressList.sortedBy { it.ownerName }
          .forEach { ownerProgress: OwnerProgress ->
            BootstrapRow {
              BootstrapColumn(3) {
                Text(ownerProgress.ownerName)
              }
              BootstrapColumn(4) {
                Text("${ownerProgress.emoji} ${ownerProgress.percentCompleteFormatted}% (${ownerProgress.completedFormatted} of ${ownerProgress.totalFormatted})")
              }
              BootstrapColumn(5) {
                NavRouteLink(
                  OwnerBreakdownNavRoute(
                    statKey = remainingStatInfo.key,
                    owner = ownerProgress.ownerName
                  ), updateNavRoute
                ) {
                  Text("${ownerProgress.remainingFormatted} Remaining")
                }
              }
            }
          }
      }
    }
  }
  Br()
}

fun compareOldestToNewest(
  firstHistoricalData: HistoricalData,
  currentHistoricalData: HistoricalData,
  remainingStatKey: StatKey
): StatComparisonResult? {
  val first: StatTotalAndMetadata? =
    firstHistoricalData.statTotalsAndMetadata.statTotals[remainingStatKey]
  val current: StatTotalAndMetadata? =
    currentHistoricalData.statTotalsAndMetadata.statTotals[remainingStatKey]
  return HistoricalComparison.compareOldestAndNewest(
    oldStats = first,
    currentStats = current,
  )
}

@Composable
fun Space() {
  Text(" ")
}

@Composable
fun TechDebtListComposable(
  navRoute: TechDebtNavRoute,
  codeReferencesByCategory: Map<String, List<StatMetadata>>,
  navRouteUpdated: (NavRoute) -> Unit,
) {
  BootstrapRow {
    BootstrapColumn {
      H3 {
        Text("Tech Debt")
      }
      codeReferencesByCategory.entries.sortedBy { it.key }.forEach { (category, codeReferenceStatTypes) ->
        H6 {
          Text(category)
        }
        Ul {
          codeReferenceStatTypes.forEach { statMetadata ->
            Li {
              NavRouteLink(
                navRoute.copy(remainingKey = statMetadata.key),
                navRouteUpdated
              ) {
                Text(statMetadata.title + " (" + statMetadata.key + ")")
              }
            }
          }
        }
      }
    }
  }
}
