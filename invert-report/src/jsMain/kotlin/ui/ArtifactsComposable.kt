package ui

import PagingConstants.MAX_RESULTS
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.squareup.invert.common.ReportDataRepo
import com.squareup.invert.common.navigation.NavRouteRepo
import com.squareup.invert.common.navigation.routes.ArtifactsNavRoute
import com.squareup.invert.common.pages.ArtifactDetailNavRoute

@Composable
fun ArtifactsComposable(reportDataRepo: ReportDataRepo, navRouteRepo: NavRouteRepo, navRoute: ArtifactsNavRoute) {
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
