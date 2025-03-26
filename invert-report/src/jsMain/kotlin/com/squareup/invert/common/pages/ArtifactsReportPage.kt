package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
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
import ui.BootstrapColumn
import ui.BootstrapLoadingMessageWithSpinner
import ui.BootstrapRow
import ui.BootstrapSearchBox
import ui.BootstrapTable
import ui.TitleRow
import kotlin.reflect.KClass

data class ArtifactsNavRoute(
  val query: String? = null,
) : BaseNavRoute(ArtifactsReportPage.navPage) {

  override fun toSearchParams(): Map<String, String> = toParamsWithOnlyPageId(this)
    .also { params ->
      query?.let {
        params[QUERY_PARAM] = query
      }
    }

  companion object {

    private const val QUERY_PARAM = "query"

    fun parser(params: Map<String, String?>): ArtifactsNavRoute {
      return ArtifactsNavRoute(
        query = params[QUERY_PARAM]
      )
    }
  }
}

object ArtifactsReportPage : InvertReportPage<ArtifactsNavRoute> {
  override val navPage: NavPage = NavPage(
    pageId = "artifacts",
    displayName = "Artifacts",
    navIconSlug = "newspaper",
    navRouteParser = { ArtifactsNavRoute.parser(it) }
  )
  override val navRouteKClass: KClass<ArtifactsNavRoute> = ArtifactsNavRoute::class

  override val composableContent: @Composable (ArtifactsNavRoute) -> Unit = { navRoute ->
    ArtifactsComposable(navRoute)
  }
}

private data class Gav(
  val group: String,
  val artifact: String,
  val version: String,
)

@Composable
fun ArtifactsComposable(
  navRoute: ArtifactsNavRoute,
  reportDataRepo: ReportDataRepo = DependencyGraph.reportDataRepo,
  navRouteRepo: NavRouteRepo = DependencyGraph.navRouteRepo
) {
  val query = navRoute.query ?: ""
  val allArtifactsCollected by reportDataRepo.allArtifacts.collectAsState(null)

  val artifactsMatchingQuery = allArtifactsCollected?.filter { it.contains(query) }

  if (artifactsMatchingQuery == null || allArtifactsCollected == null) {
    BootstrapLoadingMessageWithSpinner("Loading Artifacts...")
    return
  }

  val allArtifacts = allArtifactsCollected!!


  val groupedByGroupAndArtifact = artifactsMatchingQuery.groupBy {
    val split = it.split(":")
    split[0] + ":" + split[1]
  }.mapValues {
    it.value.groupBy {
      val split = it.split(":")
      split[2]
    }.keys
  }


  val gavs: List<Gav> = groupedByGroupAndArtifact.keys.mapNotNull {
    val split = it.split(":")
    if (split.size == 2) {
      Gav(
        group = split[0],
        artifact = split[1],
        version = if (groupedByGroupAndArtifact.size > 1) {
          groupedByGroupAndArtifact[it].toString()
        } else {
          groupedByGroupAndArtifact.get(it)?.elementAt(0) ?: ""
        }
      )
    } else {
      null
    }
  }

  val gavByGroup = gavs.groupBy { it.group }.entries.sortedByDescending { it.value.size }

  val allArtifactsCount = allArtifacts.size
  TitleRow("Artifacts (${artifactsMatchingQuery.size} of $allArtifactsCount)")
  BootstrapRow {
    BootstrapColumn(6) {
      BootstrapSearchBox(
        navRoute.query ?: "",
        "Search For Artifact..."
      ) {
        navRouteRepo.pushNavRoute(
          ArtifactsNavRoute(it)
        )
      }
    }
    BootstrapColumn(6) {
      ChartJsChartComposable(
        type = "bar",
        domId = "chart-invert-artifacts",
        data = ChartsJs.ChartJsData(
          labels = gavByGroup.map { it.key },
          datasets = listOf(
            ChartsJs.ChartJsDataset(
              label = "Group",
              data = gavByGroup.map { it.value.size }
            )
          )
        ),
        onClick = { label, value ->
          navRouteRepo.pushNavRoute(
            ArtifactsNavRoute(
              query = "$label:"
            )
          )
        }
      )
    }
  }

  BootstrapTable(
    headers = listOf("Group", "Artifact", "Versions"),
    rows = groupedByGroupAndArtifact.keys.map {
      val split = it.split(":")
      if (split.size == 2) {
        listOf(
          split[0], split[1], if (groupedByGroupAndArtifact.size > 1) {
            groupedByGroupAndArtifact[it].toString()
          } else {
            groupedByGroupAndArtifact.get(it)?.elementAt(0) ?: ""
          }
        )
      } else {
        listOf("", "", "")
      }
    },
    types = artifactsMatchingQuery.map { String::class },
    maxResultsLimitConstant = MAX_RESULTS,
    onItemClickCallback = {
      navRouteRepo.pushNavRoute(
        ArtifactDetailNavRoute(
          group = it[0],
          artifact = it[1],
        )
      )
    },
  )
}
