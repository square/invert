package com.squareup.invert.common.pages

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
import com.squareup.invert.common.utils.FormattingUtils.dateDisplayStr
import com.squareup.invert.common.utils.FormattingUtils.formatDecimalSeparator
import com.squareup.invert.models.StatDataType
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.web.attributes.ATarget
import org.jetbrains.compose.web.attributes.target
import org.jetbrains.compose.web.dom.A
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.H3
import org.jetbrains.compose.web.dom.H5
import org.jetbrains.compose.web.dom.P
import org.jetbrains.compose.web.dom.Small
import org.jetbrains.compose.web.dom.Text
import ui.BootstrapButton
import ui.BootstrapButtonType
import ui.BootstrapColumn
import ui.BootstrapJumbotron
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapLoadingSpinner
import ui.BootstrapRow
import ui.NavRouteLink
import kotlin.reflect.KClass

object HomeReportPage : InvertReportPage<HomeReportPage.HomeNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "home",
    displayName = "Home",
    navIconSlug = "house",
    navRouteParser = { HomeNavRoute }
  )
  override val navRouteKClass: KClass<HomeNavRoute> = HomeNavRoute::class

  override val composableContent: @Composable (HomeNavRoute) -> Unit = { navRoute ->
    HomeComposable(navRoute)
  }

  object HomeNavRoute : BaseNavRoute(navPage)
}


@Composable
fun HomeComposable(
  homeNavRoute: HomeReportPage.HomeNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val moduleCount by reportDataRepo.allModules.map { it?.size }.collectAsState(null)
  val artifactCount by reportDataRepo.allArtifacts.map { it?.size }.collectAsState(null)
  val ownersCount by reportDataRepo.allOwnerNames.map { it?.size }.collectAsState(null)
  val pluginIdsCount by reportDataRepo.allPluginIds.map { it?.size }.collectAsState(null)
  val reportMetadata by reportDataRepo.reportMetadata.collectAsState(null)

  if (reportMetadata == null) {
    BootstrapLoadingMessageWithSpinner("Loading Invert Report...")
    return
  }
  val metadata = reportMetadata!!
  BootstrapJumbotron(
    centered = false,
    headerContent = {
      Text("\uD83D\uDD03 Invert Report")
      H5 {
        A(href = metadata.remoteRepoUrl) {
          Text(metadata.remoteRepoUrl.substringAfter("//").substringAfter("/"))
        }
      }
    }) {
    Div({ classes("text-center") }) {
      metadata.latestCommitGitSha?.let { gitSha ->
        P({
          classes("fs-6")
        }) {
          metadata.branchName?.let { branchName ->
            A(href = "${metadata.remoteRepoUrl}/tree/${branchName}/", { target(ATarget.Blank) }) {
              Text(branchName)
            }
            Text(" branch ")
          }
          val commitUrl = metadata.remoteRepoUrl + "/commits/" + gitSha
          A(href = commitUrl, { target(ATarget.Blank) }) {
            Text(gitSha.substring(0, minOf(7, gitSha.length)))
          }
          Text(" on " + metadata.dateDisplayStr())
        }
      }
    }
  }

  val statTotalsOrig by reportDataRepo.statTotals.collectAsState(null)
  val moduleToOwnerMapFlowValue by reportDataRepo.moduleToOwnerMap.collectAsState(null)

  H3 { Text("Stats") }

  if (moduleToOwnerMapFlowValue == null) {
    BootstrapLoadingSpinner()
    return
  }

  if (statTotalsOrig == null) {
    BootstrapLoadingMessageWithSpinner("Loading...")
    return
  }

  val statTotals = statTotalsOrig!!

  BootstrapRow {
    HomeCountComposable(
      moduleCount,
      AllModulesReportPage.navPage,
      AllModulesNavRoute(),
    ) { navRouteRepo.pushNavRoute(it) }

    HomeCountComposable(
      artifactCount,
      ArtifactsReportPage.navPage,
      ArtifactsNavRoute()
    ) { navRouteRepo.pushNavRoute(ArtifactsNavRoute()) }

    HomeCountComposable(
      pluginIdsCount,
      GradlePluginsReportPage.navPage,
      GradlePluginsNavRoute()
    ) { navRouteRepo.pushNavRoute(GradlePluginsNavRoute()) }

    HomeCountComposable(
      ownersCount,
      OwnersReportPage.navPage,
      OwnersNavRoute,
    ) { navRouteRepo.pushNavRoute(OwnersNavRoute) }

    statTotals.statTotals.values.forEach { statTotalAndMetadata ->
      BootstrapColumn(3) {
        BootstrapJumbotron(
          centered = true,
          paddingNum = 2,
          headerContent = {
            Text(statTotalAndMetadata.total.formatDecimalSeparator())
          }
        ) {
          val destRoute = if (statTotalAndMetadata.metadata.dataType == StatDataType.CODE_REFERENCES) {
            CodeReferencesNavRoute(statKey = statTotalAndMetadata.metadata.key)
          } else {
            StatDetailNavRoute(
              statKey = statTotalAndMetadata.metadata.key
            )
          }
          NavRouteLink(destRoute, navRouteRepo::pushNavRoute) {
            Small {
              Text(statTotalAndMetadata.metadata.title)
            }
          }
        }
      }
    }
  }

  BootstrapButton(
    "View All",
    BootstrapButtonType.PRIMARY,
    onClick = {
      navRouteRepo.pushNavRoute(
        AllStatsNavRoute()
      )
    }
  )
}

@Composable
fun HomeCountComposable(count: Int?, navItem: NavPage, destNavRoute: NavRoute, onClick: (NavRoute) -> Unit) {
  count?.let { count ->
    BootstrapColumn(3) {
      BootstrapJumbotron(
        centered = true,
        paddingNum = 2,
        headerContent = {
          Text(count.formatDecimalSeparator())
        }
      ) {
        NavRouteLink(destNavRoute, onClick) {
          Small { Text(navItem.displayName) }
        }
      }
    }
  }
}