package com.squareup.invert.common.pages

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ModuleOwnerAndCodeReference
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.charts.ChartJsLineChartComposable
import com.squareup.invert.common.charts.ChartsJs
import com.squareup.invert.common.charts.PlotlyTreeMapComposable
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRoute
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.CodeReferencesNavRoute.Companion.parser
import com.squareup.invert.common.utils.FormattingUtils.formatEpochToDate
import com.squareup.invert.models.ExtraDataType
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.StatKey
import com.squareup.invert.models.StatMetadata
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.Li
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import org.jetbrains.compose.web.dom.Ul
import ui.BootstrapButton
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapRow
import ui.BootstrapSelectDropdown
import ui.BootstrapSelectOption
import ui.BootstrapTable
import ui.NavRouteLink
import kotlin.reflect.KClass

data class CodeReferencesNavRoute(
  val statKey: String,
  val owner: String? = null,
  val module: String? = null,
  val treemap: Boolean? = null,
  val chart: Boolean? = null,
  val extras: Map<String, String>? = null
) : BaseNavRoute(CodeReferencesReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      statKey.let {
        params[STATKEY_PARAM] = it
      }
      if (!owner.isNullOrBlank()) {
        params[OWNER_PARAM] = owner
      }
      if (!module.isNullOrBlank()) {
        params[MODULE_PARAM] = module
      }
      treemap?.let {
        params[TREEMAP_PARAM] = treemap.toString()
      }
      chart?.let {
        params[CHART_PARAM] = chart.toString()
      }
      extras?.let {
        it.forEach { (key, value) ->
          params["extra_$key"] = value
        }
      }
    }

  companion object {

    private const val STATKEY_PARAM = "statkey"
    private const val CHART_PARAM = "chart"
    private const val OWNER_PARAM = "owner"
    private const val MODULE_PARAM = "module"
    private const val TREEMAP_PARAM = "treemap"
    private const val EXTRAS_PARAM_PREFIX = "extra_"


    fun parser(params: Map<String, String?>): NavRoute {
      var statKey: String? = null
      var owner: String? = null
      var module: String? = null
      var treemap: Boolean? = null
      var chart: Boolean? = null
      var extras: MutableMap<String, String> = mutableMapOf()
      params.forEach { (key, value) ->
        val trimmedValue = value?.trim()?.let {
          if (it.isNotBlank()) {
            it
          } else {
            null
          }
        }
        if (trimmedValue != null) {
          when (key) {
            STATKEY_PARAM -> {
              statKey = trimmedValue
            }
            OWNER_PARAM -> {
              owner = trimmedValue
            }
            MODULE_PARAM -> {
              module = trimmedValue
            }
            TREEMAP_PARAM -> {
              treemap = trimmedValue.toBoolean()
            }
            CHART_PARAM -> {
              chart = trimmedValue.toBoolean()
            }
            else -> {
              if (key.startsWith(EXTRAS_PARAM_PREFIX)) {
                val extraKey = key.substring(EXTRAS_PARAM_PREFIX.length)
                extras[extraKey] = trimmedValue
              }
            }
          }
        }
      }

      return if (statKey == null) {
        AllStatsNavRoute()
      } else {
        CodeReferencesNavRoute(
          statKey = statKey,
          owner = owner,
          module = module,
          treemap = treemap,
          chart = chart,
          extras = extras.let { if (it.isEmpty()) null else it }
        )
      }
    }
  }
}

object CodeReferencesReportPage : InvertReportPage<CodeReferencesNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "code_references",
    navRouteParser = { parser(it) },
    displayName = "Code References",
    navIconSlug = "code"
  )

  override val navRouteKClass: KClass<CodeReferencesNavRoute> = CodeReferencesNavRoute::class

  override val composableContent: @Composable (CodeReferencesNavRoute) -> Unit = { navRoute ->
    CodeReferencesComposable(navRoute)
  }
}

@Composable
fun CodeReferencesComposable(
  codeReferencesNavRoute: CodeReferencesNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
  val allModulesOrig by reportDataRepo.allModules.collectAsState(null)
  val historicalDataOrig by reportDataRepo.historicalData.collectAsState(null)
  val allOwnerNames by reportDataRepo.allOwnerNames.collectAsState(null)
  val moduleToOwnerMapFlowValue: Map<ModulePath, OwnerName>? by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val statInfosOrig by reportDataRepo.statInfos.collectAsState(null)
  if (statInfosOrig == null || allOwnerNames == null || historicalDataOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading Stats")
    return
  }

  val statInfos = statInfosOrig!!

  val historicalData = historicalDataOrig!!

  val currentStatMetadata: StatMetadata? = statInfos.firstOrNull { it.key == codeReferencesNavRoute.statKey }
  if (currentStatMetadata == null) {
    H1 { Text("No stat with key '${codeReferencesNavRoute.statKey}' found") }
    BootstrapButton("View All Stats") {
      navRouteRepo.pushNavRoute(AllStatsNavRoute())
    }
    return
  }


  val metadata by reportDataRepo.reportMetadata.collectAsState(null)
  BootstrapRow {
    BootstrapColumn(8) {
      H4 {
        Text(buildString {
          append("Code References")
          append(" for ${currentStatMetadata.title} (${codeReferencesNavRoute.statKey})")
        })
      }
    }
    BootstrapColumn(4) {
      codeReferencesNavRoute.statKey.let { statKey ->
        P {
          Ul {
            Li {
              NavRouteLink(
                StatDetailNavRoute(statKey = statKey),
                navRouteRepo::pushNavRoute
              ) {
                Text("View Grouped by Module")
              }
            }
            if (historicalData.size > 1) {
              Li {
                NavRouteLink(
                  codeReferencesNavRoute.copy(
                    chart = if (codeReferencesNavRoute.chart != null) {
                      !codeReferencesNavRoute.chart
                    } else {
                      true
                    }
                  ),
                  navRouteRepo::pushNavRoute
                ) {
                  if (codeReferencesNavRoute.chart == true) {
                    Text("Hide Chart")
                  } else {
                    Text("Show Chart")
                  }
                }
              }
            }
            Li {
              NavRouteLink(
                codeReferencesNavRoute.copy(
                  treemap = if (codeReferencesNavRoute.treemap != null) {
                    !codeReferencesNavRoute.treemap
                  } else {
                    true
                  }
                ),
                navRouteRepo::pushNavRoute,
              ) {
                if (codeReferencesNavRoute.treemap == true) {
                  Text("Hide Treemap")
                } else {
                  Text("Show Treemap")
                }
              }
            }
            Li {
              NavRouteLink(
                OwnerBreakdownNavRoute(
                  statKey = codeReferencesNavRoute.statKey,
                  owner = codeReferencesNavRoute.owner,
                ),
                navRouteRepo::pushNavRoute
              ) {
                Text("View Owner Breakdown")
              }
            }
          }
        }
      }
    }
  }

  if (moduleToOwnerMapFlowValue == null || metadata == null) {
    BootstrapLoadingSpinner()
    return
  }

  if (allModulesOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading Modules")
    return
  }

  val projectMetadata by reportDataRepo.reportMetadata.collectAsState(null)
  if (projectMetadata == null) {
    BootstrapLoadingMessageWithSpinner("Loading Metadata")
    return
  }

  val statKey = codeReferencesNavRoute.statKey

  val statsForKey: MutableList<ModuleOwnerAndCodeReference>? by reportDataRepo.statsForKey(statKey)
    .collectAsState(null)
  if (statsForKey == null) {
    BootstrapLoadingMessageWithSpinner("Loading Stats for $statKey")
    return
  }

  val allCodeReferencesForStat: Set<ModuleOwnerAndCodeReference> = statsForKey!!.toSet()

  val filteredByOwner: List<ModuleOwnerAndCodeReference> = allCodeReferencesForStat
    // Filter By Module
    .filter { moduleOwnerAndCodeReference: ModuleOwnerAndCodeReference ->
      val moduleFromModule = moduleOwnerAndCodeReference.module
      val moduleFromNavRoute = codeReferencesNavRoute.module
      if (!moduleFromNavRoute.isNullOrBlank()) {
        moduleFromNavRoute == moduleOwnerAndCodeReference.module || moduleFromNavRoute == moduleFromModule
      } else {
        true
      }

    }
    // Filter by Owner
    .filter { ownerAndCodeReference: ModuleOwnerAndCodeReference ->
      val ownerOfModule = ownerAndCodeReference.owner
      val codeReferenceOwner = ownerAndCodeReference.codeReference.owner
      val ownerFromNavRoute = codeReferencesNavRoute.owner
      if (!ownerFromNavRoute.isNullOrBlank()) {
        ownerFromNavRoute == codeReferenceOwner || ownerFromNavRoute == ownerOfModule
      } else {
        true
      }
    }
    // Filter by extras
    .filter { ownerAndCodeReference: ModuleOwnerAndCodeReference ->
      val codeReference = ownerAndCodeReference.codeReference
      val extras = codeReferencesNavRoute.extras ?: mapOf()
      if (extras.isEmpty()) {
        true
      } else {
        extras.all { (key, value) ->
          codeReference.extras[key] == value
        }
      }
    }

  if (codeReferencesNavRoute.treemap == true) {
    BootstrapRow {
      BootstrapColumn {
        PlotlyTreeMapComposable(
          filePaths = filteredByOwner.map { it.codeReference.filePath },
        )
      }
    }
  }

  if (codeReferencesNavRoute.chart == true) {
    BootstrapRow {
      BootstrapColumn {
        val datasets = mutableListOf<ChartsJs.ChartJsDataset>()
        val currentHistoricalData = historicalData.last()
        val remainingStatKeys: List<StatKey> = listOf(statKey)
        remainingStatKeys.forEach { remainingStatKey ->
          val values: List<Int> = historicalData.map { historicalDataPoint ->
            historicalDataPoint.statTotalsAndMetadata.statTotals[remainingStatKey]?.total ?: 0
          }
          val remainingStat = currentHistoricalData.statTotalsAndMetadata.statTotals[remainingStatKey]!!.metadata
          datasets.add(
            ChartsJs.ChartJsDataset(
              label = remainingStat.title,
              data = values
            )
          )
        }

        val chartJsData = ChartsJs.ChartJsData(
          labels = historicalData.map { formatEpochToDate(it.reportMetadata.latestCommitTime) },
          datasets = datasets,
        )

        ChartJsLineChartComposable(
          data = chartJsData,
          onClick = { label, value ->

          })
      }
    }
  }

  BootstrapRow {
    H3 {
      Text("Filters")
    }
  }

  val codeReferencesByOwner = allCodeReferencesForStat.groupBy { it.owner }
  val totalCodeReferenceCount = allCodeReferencesForStat.size
  BootstrapRow {
    BootstrapColumn(6) {
      H5 {
        Text("Owner")
        BootstrapSelectDropdown(
          placeholderText = "-- All Owners ($totalCodeReferenceCount Total) --",
          currentValue = codeReferencesNavRoute.owner,
          options = codeReferencesByOwner.map {
            BootstrapSelectOption(
              value = it.key,
              displayText = "${it.key} (${it.value.size} of $totalCodeReferenceCount)"
            )
          }.sortedBy { it.displayText }
        ) {
          navRouteRepo.pushNavRoute(
            codeReferencesNavRoute.copy(
              owner = it?.value,
            )
          )
        }
      }
    }
    val codeReferencesByModule =
      allCodeReferencesForStat.groupBy { it.module }
    BootstrapColumn(6) {
      H5 {
        Text("Module")
        BootstrapSelectDropdown(
          placeholderText = "-- All Modules --",// (${codeReferencesByModule.size} Total) --",
          currentValue = codeReferencesNavRoute.module,
          options = codeReferencesByModule.map {
            BootstrapSelectOption(
              value = it.key,
              displayText = it.key,// (${it.value.size} of $totalCodeReferenceCount)"
            )
          }.sortedBy { it.displayText }
        ) {
          navRouteRepo.pushNavRoute(
            codeReferencesNavRoute.copy(
              module = it?.value
            )
          )
        }
      }
    }
  }
  // Limit filterable extras to ones where the amount of possible values is within a reasonable limit
  val filterableExtraCountLimit = 5000
  val allExtraValuesByKey = allCodeReferencesForStat.flatMap {
    it.codeReference.extras.entries.toList()
  }.groupBy { it.key }.mapValues { it.value.map { entry -> entry.value }.toSet().sorted() }
  val filterableExtras = currentStatMetadata.extras.filter {
    val extraValues = allExtraValuesByKey[it.key] ?: emptyList()
    (it.type == ExtraDataType.STRING  || it.type == ExtraDataType.BOOLEAN) &&
      extraValues.size < filterableExtraCountLimit
  }.sortedBy { it.description }
  if (filterableExtras.isNotEmpty()) {
    filterableExtras.chunked(size = 2).map { extraGroup ->
      BootstrapRow {
        extraGroup.forEach { extra ->
          BootstrapColumn(6) {
            H5 {
              Text(extra.description)
              BootstrapSelectDropdown(
                placeholderText = "-- All Values --",
                currentValue = codeReferencesNavRoute.extras?.get(extra.key),
                options = allCodeReferencesForStat.mapNotNull {
                  it.codeReference.extras[extra.key]
                }.toSet().map {
                  BootstrapSelectOption(
                    value = it,
                    displayText = it,
                  )
                }.sortedBy { it.displayText }
              ) {
                val value = it?.value
                var newExtras: Map<String, String>? = null
                val currentExtras = codeReferencesNavRoute.extras ?: mapOf()
                if (value != null) {
                  newExtras = currentExtras + mapOf(extra.key to value)
                } else {
                  newExtras = currentExtras.minus(extra.key)
                  if (newExtras.isEmpty()) {
                    newExtras = null
                  }
                }
                navRouteRepo.pushNavRoute(
                  codeReferencesNavRoute.copy(
                    extras = newExtras
                  )
                )
              }
            }
          }
        }
      }
    }
  }

  BootstrapTable(
    headers = listOf(
      "Module",
      "Owner",
      "File",
      "Code"
    ) + currentStatMetadata.extras.map { "${it.description} (${it.key})" },
    rows = filteredByOwner
      .map {
        val listOfExtraValues: List<String> =
          currentStatMetadata.extras.map { extra -> it.codeReference.extras[extra.key] ?: "" }
        listOf(
          it.module,
          it.codeReference.owner ?: (it.owner + " (Owns Module)"),
          it.codeReference.toHrefLink(projectMetadata!!, false),
          it.codeReference.code ?: ""
        ) + listOfExtraValues
      },
    maxResultsLimitConstant = PagingConstants.MAX_RESULTS,
    sortAscending = true,
    sortByColumn = 2,
    types = listOf(
      String::class,
      String::class,
      String::class,
      String::class
    ) + currentStatMetadata.extras.map { extra ->
      when (extra.type) {
        ExtraDataType.BOOLEAN -> Boolean::class
        ExtraDataType.NUMERIC -> Int::class
        ExtraDataType.STRING -> String::class
      }
    }
  )
}
