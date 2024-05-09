package com.squareup.invert.common.pages


import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.DependencyGraph
import com.squareup.invert.common.InvertReportPage
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavPage
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.BaseNavRoute
import com.squareup.invert.common.pages.ArtifactsNavRoute.Companion.parser
import ui.BootstrapLoadingMessageWithSpinner
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
        navRouteParser = { parser(it) }
    )
    override val navRouteKClass: KClass<ArtifactsNavRoute> = ArtifactsNavRoute::class

    override val composableContent: @Composable (ArtifactsNavRoute) -> Unit = { navRoute ->
        ArtifactsComposable(navRoute)
    }
}

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

    val allArtifactsCount = allArtifacts.size
    TitleRow("Artifacts (${artifactsMatchingQuery.size} of $allArtifactsCount)")
    BootstrapSearchBox(
        navRoute.query ?: "",
        "Search For Artifact..."
    ) {
        navRouteRepo.updateNavRoute(
            ArtifactsNavRoute(it)
        )
    }

    val groupedByGroupAndArtifact = artifactsMatchingQuery.groupBy {
        val split = it.split(":")
        split[0] + ":" + split[1]
    }.mapValues {
        it.value.groupBy {
            val split = it.split(":")
            split[2]
        }.keys
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
        onItemClick = {
            navRouteRepo.updateNavRoute(
                ArtifactDetailNavRoute(
                    group = it[0],
                    artifact = it[1],
                )
            )
        },
    )
}
