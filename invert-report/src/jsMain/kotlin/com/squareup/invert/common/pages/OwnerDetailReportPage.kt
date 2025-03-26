package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
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
import com.squareup.invert.common.pages.OwnerDetailNavRoute.Companion.parser
import com.squareup.invert.models.OwnerName
import org.jetbrains.compose.web.dom.H1
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapTabData
import ui.BootstrapTabPane
import ui.BootstrapTable
import kotlin.reflect.KClass

data class OwnerDetailNavRoute(
  val owner: OwnerName,
) : BaseNavRoute(OwnerDetailReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { map ->
      map[OWNER_PARAM] = owner
    }

  companion object {

    private const val OWNER_PARAM = "owner"
    fun parser(params: Map<String, String?>): NavRoute {
      val owner = params[OWNER_PARAM]
      return if (!owner.isNullOrEmpty()) {
        OwnerDetailNavRoute(owner)
      } else {
        OwnersNavRoute
      }
    }
  }
}

object OwnerDetailReportPage : InvertReportPage<OwnerDetailNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "owner_detail",
    navRouteParser = { parser(it) }
  )

  override val navRouteKClass: KClass<OwnerDetailNavRoute> = OwnerDetailNavRoute::class

  override val composableContent: @Composable (OwnerDetailNavRoute) -> Unit = { navRoute ->
    OwnerDetailComposable(navRoute)
  }
}


@Composable
fun OwnerDetailComposable(
  ownerDetailNavRoute: OwnerDetailNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo,
) {
  val ownerName = ownerDetailNavRoute.owner
  val ownerToModulesMap by reportDataRepo.ownerNameToModulesMap.collectAsState(mapOf())
  val modules = ownerToModulesMap?.get(ownerName) ?: listOf()

  val ownerInfoComposable: @Composable () -> Unit = {
    H1 {
      Text("$ownerName owns ${modules.size} Modules")
    }

    BootstrapTable(
      headers = listOf("Module"),
      rows = modules.map { listOf(it) },
      types = listOf(String::class),
      maxResultsLimitConstant = MAX_RESULTS
    ) { cellValues ->
      navRouteRepo.pushNavRoute(ModuleDetailNavRoute(cellValues[0]))
    }
  }

  BootstrapTabPane(
    listOf(
      BootstrapTabData("Info", ownerInfoComposable),
    )
  )

}


