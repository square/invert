package com.squareup.invert.common.pages

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
import com.squareup.invert.common.pages.OwnerBreakdownNavRoute.Companion.OWNER_KEY
import com.squareup.invert.common.pages.OwnerBreakdownNavRoute.Companion.STAT_KEY
import com.squareup.invert.common.utils.MathUtils
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.Stat
import com.squareup.invert.models.Stat.CodeReferencesStat.CodeReference
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.StatKey
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.Hr
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import ui.BootstrapSelectDropdown
import ui.BootstrapSelectOption
import ui.BootstrapTabData
import ui.BootstrapTabPane
import ui.BootstrapTable
import kotlin.reflect.KClass


data class OwnerBreakdownNavRoute(
  val owner: String?,
  val statKey: StatKey?,
) : BaseNavRoute(OwnerBreakdownReportPage.navPage) {
  override fun toSearchParams(): Map<String, String> {
    val params = super.toSearchParams().toMutableMap()
    owner?.trim()?.let {
      if (it.isNotEmpty()) {
        params[OWNER_KEY] = it
      }
    }
    statKey?.trim()?.let {
      if (it.isNotEmpty()) {
        params[STAT_KEY] = it
      }
    }
    return params
  }

  companion object {
    const val OWNER_KEY = "owner"
    const val STAT_KEY = "statkey"
  }
}

object OwnerBreakdownReportPage : InvertReportPage<OwnerBreakdownNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "owner_breakdown",
    displayName = "Owner Breakdown",
    navIconSlug = "people-fill",
    navRouteParser = { params: Map<String, String?> ->
      val modulePath = params[OWNER_KEY]?.trim()
      val statKey = params[STAT_KEY]?.trim()
      OwnerBreakdownNavRoute(
        owner = if (modulePath.isNullOrBlank()) {
          null
        } else {
          modulePath
        },
        statKey = if (statKey.isNullOrBlank()) {
          null
        } else {
          statKey
        },
      )
    }
  )

  override val navRouteKClass: KClass<OwnerBreakdownNavRoute> = OwnerBreakdownNavRoute::class

  override val composableContent: @Composable (OwnerBreakdownNavRoute) -> Unit = { navRoute ->
    ByOwnerComposable(navRoute)
  }
}

private fun getCodeReferenceOwnerToModulePlusCodeReferencesList(
  statKey: StatKey,
  moduleToOwnerMap: Map<ModulePath, OwnerName>,
  statsByModuleParam: Map<ModulePath, Map<StatKey, Stat>>
): Map<OwnerName, Map<ModulePath, Set<CodeReference>>> {
  val toReturnOwnerNameToModuleAndCodeReferencesMap = mutableMapOf<OwnerName, Map<ModulePath, Set<CodeReference>>>()

  statsByModuleParam.entries.forEach { (modulePath: ModulePath, moduleStatsByKey: Map<StatKey, Stat>) ->
    val moduleOwner = moduleToOwnerMap[modulePath] ?: ""
    val codeReferences =
      (moduleStatsByKey.entries.firstOrNull { it.key == statKey }?.value as? Stat.CodeReferencesStat)?.value


    codeReferences?.forEach { newCodeReference: CodeReference ->
      val owner = newCodeReference.owner ?: moduleOwner
      val curr1ModuleToCodeReferencesForOwnerMap: MutableMap<ModulePath, Set<CodeReference>> =
        toReturnOwnerNameToModuleAndCodeReferencesMap[owner]?.toMutableMap() ?: mutableMapOf()
      val curr2CodeReferencesForOwnerAndModule =
        (curr1ModuleToCodeReferencesForOwnerMap[modulePath] ?: emptyList()).toMutableSet().apply {
          add(newCodeReference)
        }
      toReturnOwnerNameToModuleAndCodeReferencesMap[owner] =
        curr1ModuleToCodeReferencesForOwnerMap.toMutableMap().apply {
          this[modulePath] = curr2CodeReferencesForOwnerAndModule + newCodeReference
        }
    }
  }

  return toReturnOwnerNameToModuleAndCodeReferencesMap
}

@Composable
fun ByOwnerComposable(
  navRoute: OwnerBreakdownNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val statsOrig by reportDataRepo.statsData.collectAsState(null)
  val statInfosOrig by reportDataRepo.statInfos.collectAsState(null)
  val moduleToOwnerMapOrig by reportDataRepo.moduleToOwnerMap.collectAsState(null)
  val allOwnerNames by reportDataRepo.allOwnerNames.collectAsState(null)

  if (
    listOf(
      allOwnerNames,
      moduleToOwnerMapOrig,
      statInfosOrig,
      statsOrig
    ).any { it == null }
  ) {
    BootstrapLoadingMessageWithSpinner()
    return
  }

  if (allOwnerNames.isNullOrEmpty()) {
    H1 {
      Text("No Owners")
    }
    P {
      Text("Specify an owners collector in the `invert` block of your build.gradle.kts file.")
      Br()
      A(href = "https://github.com/square/invert", {
        target(ATarget.Blank)
      }) { Text("View Invert Documentation") }
    }
    return
  }

  fun getStatDescription(statKey: StatKey): String {
    return statInfosOrig?.first { it.key == statKey }?.description ?: statKey
  }

  val ownerParamValue = navRoute.owner

  val codeReferenceStatTypes = statInfosOrig!!
    .filter { it.dataType == StatDataType.CODE_REFERENCES }

  BootstrapRow {
    BootstrapColumn(6) {
      H3 {
        Text("Filter by Owner")
        BootstrapSelectDropdown(
          placeholderText = "-- All Owners --",
          currentValue = ownerParamValue,
          options = allOwnerNames!!.map { BootstrapSelectOption(it, it) }
        ) {
          navRouteRepo.updateNavRoute(
            navRoute.copy(
              owner = it?.value
            )
          )
        }
      }
    }
    BootstrapColumn(6) {
      H3 {
        Text("Filter by Stat")
        BootstrapSelectDropdown(
          placeholderText = "-- All Stats --",
          currentValue = navRoute.statKey,
          options = codeReferenceStatTypes.map {
            BootstrapSelectOption(
              value = it.key,
              displayText = "${it.description} (${it.key})"
            )
          }
        ) {
          navRouteRepo.updateNavRoute(
            navRoute.copy(
              statKey = it?.value
            )
          )
        }
      }
    }
  }

  Hr()

  val codeReferenceStatTypesFilteredByNavParams = codeReferenceStatTypes
    .filter {
      if (!navRoute.statKey.isNullOrBlank()) {
        it.key == navRoute.statKey
      } else {
        true
      }
    }
    .map { it.key }

  BootstrapTabPane(
    codeReferenceStatTypesFilteredByNavParams
      .mapNotNull { statKey ->
        val statMetadata = statInfosOrig!!.firstOrNull { it.key == statKey }
        val ownerToModulePathToCodeReferences: Map<OwnerName, Map<ModulePath, Set<CodeReference>>> =
          getCodeReferenceOwnerToModulePlusCodeReferencesList(
            statKey = statKey,
            moduleToOwnerMap = moduleToOwnerMapOrig!!,
            statsByModuleParam = statsOrig!!.statsByModule
          )
        val filteredByOwners = ownerToModulePathToCodeReferences.filter {
          if (!ownerParamValue.isNullOrBlank()) {
            it.key == ownerParamValue
          } else {
            true
          }
        }

        val totalCount = ownerToModulePathToCodeReferences.entries.sumOf { it.value.values.sumOf { it.size } }
        val filteredCount = filteredByOwners.entries.sumOf { it.value.values.sumOf { it.size } }
        if (filteredCount == 0) {
          // Short Circuit
          return@mapNotNull null
        }
        val countMessage = buildString {
          if (!ownerParamValue.isNullOrBlank()) {
            append("$filteredCount of $totalCount")
          } else {
            append("$totalCount Total")
          }
        }

        BootstrapTabData(
          getStatDescription(statKey) + " ($countMessage)"
        ) {
          BootstrapRow {
            BootstrapColumn(3) {
              val bySize: Map<OwnerName, Int> =
                ownerToModulePathToCodeReferences.entries.map { it.key to it.value.values.sumOf { it.size } }
                  .sortedByDescending { it.second }.toMap()
              ChartJsChartComposable(
                type = "bar",
                data = ChartsJs.ChartJsData(
                  labels = bySize.keys,
                  datasets = listOf(
                    ChartsJs.ChartJsDataset(
                      label = "References of ${statMetadata?.description}",
                      data = bySize.values
                    )
                  )
                ),
                onClick = { label, value ->
                  navRouteRepo.updateNavRoute(
                    CodeReferencesNavRoute(
                      statKey = statKey,
                      owner = label,
                    )
                  )
                }
              )
            }
            BootstrapColumn(3) {
              val bySize: Map<OwnerName, Int> =
                ownerToModulePathToCodeReferences.entries.map { it.key to it.value.values.sumOf { it.size } }
                  .sortedByDescending { it.second }.toMap()
              ChartJsChartComposable(
                type = "pie",
                data = ChartsJs.ChartJsData(
                  labels = bySize.keys,
                  datasets = listOf(
                    ChartsJs.ChartJsDataset(
                      label = "References Count of ${statMetadata?.description}",
                      data = bySize.values
                    )
                  )
                ),
                onClick = { label, value ->
                  navRouteRepo.updateNavRoute(
                    CodeReferencesNavRoute(
                      statKey = statKey,
                      owner = label,
                    )
                  )
                }
              )
            }
          }

          val rows = filteredByOwners
            .map { (ownerName: OwnerName, moduleToCodeReferencesMap: Map<ModulePath, Set<CodeReference>>) ->
              mutableListOf<String>().apply {
                add(ownerName)
                val moduleReferenceCount = moduleToCodeReferencesMap.entries.sumOf { it.value.size }
                add(moduleReferenceCount.toString())
                val percent: Double = MathUtils.percentage(
                  amount = moduleReferenceCount,
                  total = totalCount
                )

                add(
                  buildString {
                    appendLine("**References in ${moduleToCodeReferencesMap.size} Modules containing $percent% of total ($moduleReferenceCount of ${totalCount})**")
                    appendLine("[View References for Owner](?page=code_references&statkey=${statKey}&owner=${ownerName})")
                    moduleToCodeReferencesMap.entries.sortedBy { it.key }
                      .forEachIndexed { idx: Int, moduleToCodeReferences: Map.Entry<ModulePath, Set<CodeReference>> ->
                        val modulePath = moduleToCodeReferences.key
                        appendLine("${idx + 1}. `$modulePath` (${moduleToCodeReferences.value.size})")
                        val newUrl =
                          "?page=code_references&statkey=${statKey}&owner=${ownerName}&module=${modulePath}"
                        appendLine("[View References]($newUrl)")
                      }
                  }
                )
              }
            }
          BootstrapTable(
            types = listOf(String::class, Int::class, String::class),
            sortByColumn = 1,
            sortAscending = false,
            headers = mutableListOf(
              "Owner",
              "Code Reference Count for Owner",
              "Code References in Modules",
            ),
            onItemClickCallback = {
              navRouteRepo.updateNavRoute(
                CodeReferencesNavRoute(
                  statKey = statKey,
                  owner = it[0]
                )
              )
            },
            rows = rows
          )
        }
      }
  )
  Hr()
}

