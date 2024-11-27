package com.squareup.invert.common.pages

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ModuleOwnerAndCodeReference
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.charts.PlotlyTreeMapComposable
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.CodeReferencesNavRoute.Companion.parser
import com.squareup.invert.models.ExtraKey
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.StatDataType
import com.squareup.invert.models.js.StatTotalAndMetadata
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapRow
import ui.BootstrapSelectDropdown
import ui.BootstrapSelectOption
import ui.BootstrapTable
import kotlin.reflect.KClass

data class CodeReferencesNavRoute(
  val statKey: String? = null,
  val owner: String? = null,
  val module: String? = null,
  val treemap: Boolean? = null,
) : BaseNavRoute(CodeReferencesReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      statKey?.let {
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
    }

  companion object {

    private const val STATKEY_PARAM = "statkey"
    private const val OWNER_PARAM = "owner"
    private const val MODULE_PARAM = "module"
    private const val TREEMAP_PARAM = "treemap"

    fun parser(params: Map<String, String?>): CodeReferencesNavRoute {
      val statKey = params[STATKEY_PARAM]
      val owner = params[OWNER_PARAM]?.trim()?.let {
        if (it.isNotBlank()) {
          it
        } else {
          null
        }
      }
      val module = params[MODULE_PARAM]?.trim()?.let {
        if (it.isNotBlank()) {
          it
        } else {
          null
        }
      }
      val treemap = params[TREEMAP_PARAM]?.trim()?.let {
        if (it.isNotBlank()) {
          it.toBoolean()
        } else {
          null
        }
      }
      return CodeReferencesNavRoute(
        statKey = statKey,
        owner = owner,
        module = module,
        treemap = treemap,
      )
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
  val allOwnerNames by reportDataRepo.allOwnerNames.collectAsState(null)
  val moduleToOwnerMapFlowValue: Map<ModulePath, OwnerName>? by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val statInfosOrig by reportDataRepo.statInfos.collectAsState(null)
  if (statInfosOrig == null || allOwnerNames == null) {
    BootstrapLoadingMessageWithSpinner("Loading Stats")
    return
  }

  val metadata by reportDataRepo.reportMetadata.collectAsState(null)
  H1 {
    Text(buildString {
      append("Code References")
      codeReferencesNavRoute.statKey?.let { statKey ->
        val statInfo = statInfosOrig?.filter { it.key == codeReferencesNavRoute.statKey }?.firstOrNull()
        append(" for ${statInfo?.description ?: statKey} (${codeReferencesNavRoute.statKey})")
      }
    })
  }
  H4 {
    codeReferencesNavRoute.statKey?.let { statKey ->
      A("#", {
        onClick {
          navRouteRepo.updateNavRoute(StatDetailNavRoute(statKeys = listOf(statKey)))
        }
      }) {
        Text("View Grouped by Module")
      }

      Text(" ")
      A("#", {
        onClick {
          navRouteRepo.updateNavRoute(
            codeReferencesNavRoute.copy(
              treemap = if (codeReferencesNavRoute.treemap != null) {
                !codeReferencesNavRoute.treemap
              } else {
                true
              }
            )
          )
        }
      }) {
        if (codeReferencesNavRoute.treemap == true) {
          Text("Hide Treemap")
        } else {
          Text("Show Treemap")
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

  if (statKey == null) {
    val statTotalsOrig by reportDataRepo.statTotals.collectAsState(null)
    if (statTotalsOrig == null) {
      BootstrapLoadingMessageWithSpinner("Loading Code References")
      return
    }
    val codeReferenceStatTotals: List<StatTotalAndMetadata> = statTotalsOrig!!.statTotals.values
      .filter { statTotalAndMetadata -> statTotalAndMetadata.metadata.dataType == StatDataType.CODE_REFERENCES }
    StatTiles(
      codeReferenceStatTotals
    ) { statKey ->
      navRouteRepo.updateNavRoute(
        CodeReferencesNavRoute(statKey)
      )
    }
  } else {

    val statsForKey: MutableList<ModuleOwnerAndCodeReference>? by reportDataRepo.statsForKey(statKey)
      .collectAsState(null)
    if (statsForKey == null) {
      BootstrapLoadingMessageWithSpinner("Loading Stats for $statKey")
      return
    }

    val allCodeReferencesForStat: Set<ModuleOwnerAndCodeReference> = statsForKey!!.toSet()

    val extraKeys = mutableSetOf<ExtraKey>()
    allCodeReferencesForStat
      .forEach { moduleOwnerAndCodeReference ->
        moduleOwnerAndCodeReference.codeReference.extras.forEach {
          extraKeys.add(it.key)
        }
      }

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

    if (codeReferencesNavRoute.treemap == true) {
      BootstrapRow {
        BootstrapColumn {
          PlotlyTreeMapComposable(
            filePaths = filteredByOwner.map { it.codeReference.filePath },
          )
        }
      }
    }

    val codeReferencesByOwner = allCodeReferencesForStat.groupBy { it.owner }
    val totalCodeReferenceCount = allCodeReferencesForStat.size
    BootstrapRow {
      BootstrapColumn(6) {
        H3 {
          Text("Filter by Owner")
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
            navRouteRepo.updateNavRoute(
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
        H3 {
          Text("Filter by Module")
          BootstrapSelectDropdown(
            placeholderText = "-- All Modules --",// (${codeReferencesByModule.size} Total) --",
            currentValue = codeReferencesNavRoute.module,
            options = codeReferencesByModule.map {
              BootstrapSelectOption(
                value = it.key,
                displayText = "${it.key}",// (${it.value.size} of $totalCodeReferenceCount)"
              )
            }.sortedBy { it.displayText }
          ) {
            navRouteRepo.updateNavRoute(
              codeReferencesNavRoute.copy(
                module = it?.value
              )
            )
          }
        }
      }
    }

    BootstrapTable(
      headers = listOf("Module", "Owner", "File", "Code") + extraKeys,
      rows = filteredByOwner
        .map {
          val listOfExtraValues: List<String> = extraKeys.map { key -> it.codeReference.extras[key] ?: "" }
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
      types = listOf(String::class, String::class, String::class, String::class) + extraKeys.map { String::class }
    )
  }
}
