package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.DiProvidesAndInjectsItem
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.httpsUrlForCommit
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.DependencyInjectionNavRoute.Companion.parser
import kotlinx.browser.window
import kotlinx.html.ATarget
import org.jetbrains.compose.web.dom.Br
import org.jetbrains.compose.web.dom.Datalist
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.H4
import org.jetbrains.compose.web.dom.Option
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapRow
import ui.BootstrapSearchBox
import ui.BootstrapTable
import ui.MarkdownCellContent
import kotlin.reflect.KClass

data class DependencyInjectionNavRoute(
  val typeQuery: String? = null,
  val moduleQuery: String? = null,
) : BaseNavRoute(DependencyInjectionReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      moduleQuery?.let {
        params[MODULE_QUERY_PARAM] = it
      }
      typeQuery?.let {
        params[TYPE_QUERY_PARAM] = it
      }
    }

  companion object {

    private const val MODULE_QUERY_PARAM = "modulequery"
    private const val TYPE_QUERY_PARAM = "providedquery"

    fun parser(params: Map<String, String?>): DependencyInjectionNavRoute {
      val moduleQuery = params[MODULE_QUERY_PARAM]
      val typeQuery = params[TYPE_QUERY_PARAM]
      return DependencyInjectionNavRoute(
        typeQuery = typeQuery,
        moduleQuery = moduleQuery,
      )
    }
  }
}

object DependencyInjectionReportPage : InvertReportPage<DependencyInjectionNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "dependency_injection",
    displayName = "Dependency Injection",
    navRouteParser = { parser(it) }
  )

  override val navRouteKClass: KClass<DependencyInjectionNavRoute> = DependencyInjectionNavRoute::class

  override val composableContent: @Composable (DependencyInjectionNavRoute) -> Unit = { navRoute ->
    DependencyInjectionComposable(navRoute)
  }
}


@Composable
fun DependencyInjectionComposable(
  diNavRoute: DependencyInjectionNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
  val statsData by reportDataRepo.statsData.collectAsState(null)
  val allModulesOrig by reportDataRepo.allModules.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  H1 { Text("Dependency Injection") }

  if (moduleToOwnerMapFlowValue == null) {
    BootstrapLoadingSpinner()
    return
  }

  val moduleQuery = diNavRoute.moduleQuery ?: ""
  val typeQuery = diNavRoute.typeQuery ?: ""

  if (allModulesOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading Modules")
    return
  }

  val allModules = allModulesOrig!!

  val modulesMatchingQuery = if (moduleQuery != null && moduleQuery != ":" && moduleQuery.isNotEmpty()) {
    allModules.filter { it.contains(moduleQuery) }
  } else {
    allModules
  }

  val ALL_MODULES_DATALIST_ID = "available_modules"
  Datalist({ id(ALL_MODULES_DATALIST_ID) }) {
    allModules.map { Option(it) }
  }

  BootstrapRow {
    BootstrapColumn(12) {
      BootstrapSearchBox(
        query = typeQuery,
        placeholderText = "Class/Interface Type Search...",
      ) {
        navRouteRepo.updateNavRoute(diNavRoute.copy(typeQuery = it))
      }
    }
  }
  Br()

  val metadata by reportDataRepo.reportMetadata.collectAsState(null)
  if (statsData == null || metadata == null) {
    BootstrapLoadingMessageWithSpinner("Loading...")
    return
  }

  val gitRepoHttpsUrlForBranch = metadata!!.httpsUrlForCommit()

  val diRowDataRows by reportDataRepo.diProvidesAndInjects.collectAsState(null)
  if (diRowDataRows == null) {
    BootstrapLoadingMessageWithSpinner()
    return
  }
  val columnsHeaders = mutableListOf(
    "Module",
    "Type",
    "Qualifiers",
    "File",
  )
  H4 { Text("Provides") }
  BootstrapTable(
    rows = diRowDataRows!!
      .filterIsInstance<DiProvidesAndInjectsItem.Provides>()
      .filter { rowData ->
        rowData.module.contains(moduleQuery)
            && (
            rowData.type.contains(typeQuery, true)
                || rowData.implementationType.contains(typeQuery, true)
            )
      }
      .map {
        listOf(
          it.module,
          it.type + " -> " + it.implementationType,
          it.qualifiers.joinToString(" "),
          "${it.filePath}#L${it.startLine}-L${it.endLine}",
        )
      },
    types = columnsHeaders.map { String::class }.dropLast(1).toMutableList<KClass<*>>()
      .apply { add(MarkdownCellContent::class) },
    headers = columnsHeaders,
    maxResultsLimitConstant = MAX_RESULTS,
    onItemClickCallback = {
      val filePath = it[3]
      window.open("${gitRepoHttpsUrlForBranch}/$filePath", ATarget.blank)
    }
  )
  H4 { Text("Injects") }
  BootstrapTable(
    rows = diRowDataRows!!
      .filterIsInstance<DiProvidesAndInjectsItem.Injects>()
      .filter { rowData ->

        rowData.module.contains(moduleQuery, true)
            && rowData.type.contains(typeQuery, true)
      }
      .map {
        listOf(
          it.module,
          it.type,
          it.qualifiers.joinToString(" "),
          "${it.filePath}#L${it.startLine}-L${it.endLine}",
        )
      },
    types = columnsHeaders.map { String::class },
    headers = columnsHeaders,
    maxResultsLimitConstant = MAX_RESULTS,
    onItemClickCallback = {
      val filePath = it[3]
      window.open("${gitRepoHttpsUrlForBranch}/$filePath", ATarget.blank)
    }
  )
}
