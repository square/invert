package com.squareup.invert.common.pages

import PagingConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ModuleOwnerAndCodeReference
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.CodeReferencesNavRoute.Companion.parser
import com.squareup.invert.models.ExtraKey
import com.squareup.invert.models.ModulePath
import com.squareup.invert.models.OwnerName
import com.squareup.invert.models.StatDataType
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapTable
import kotlin.reflect.KClass

data class CodeReferencesNavRoute(
  val statKey: String? = null
) : BaseNavRoute(CodeReferencesReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      statKey?.let {
        params[STATKEY_PARAM] = it
      }
    }

  companion object {

    private const val STATKEY_PARAM = "statkey"

    fun parser(params: Map<String, String?>): CodeReferencesNavRoute {
      val statKey = params[STATKEY_PARAM]
      return CodeReferencesNavRoute(
        statKey = statKey,
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
  val moduleToOwnerMapFlowValue: Map<ModulePath, OwnerName>? by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  val metadata by reportDataRepo.reportMetadata.collectAsState(null)
  H1 {
    Text(buildString {
      append("Code References")
      codeReferencesNavRoute.statKey?.let { statKey ->
        append(" for $statKey")
      }
    })
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
    StatTiles(statTotalsOrig!!.statTotals
      .filter { it.key.dataType == StatDataType.CODE_REFERENCES }) { statKey ->
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

    val extraKeys = mutableSetOf<ExtraKey>()
    statsForKey!!.forEach { moduleOwnerAndCodeReference ->
      moduleOwnerAndCodeReference.codeReference.extras.forEach {
        extraKeys.add(it.key)
      }
    }

    BootstrapTable(
      headers = listOf("Module", "Owner", "File", "Code") + extraKeys,
      rows = statsForKey!!.map {
        val listOfExtraValues: List<String> = extraKeys.map { key -> it.codeReference.extras[key] ?: "" }
        listOf(
          it.module,
          it.owner,
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
